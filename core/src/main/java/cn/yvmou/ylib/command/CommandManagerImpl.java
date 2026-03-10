package cn.yvmou.ylib.command;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.command.annotation.AnnotationParser;
import cn.yvmou.ylib.command.config.CommandConfig;
import cn.yvmou.ylib.command.config.CommandConfigApplicator;
import cn.yvmou.ylib.command.config.CommandConfigFile;
import cn.yvmou.ylib.command.config.CommandConfigLoader;
import cn.yvmou.ylib.command.tree.CommandNode;
import cn.yvmou.ylib.command.wrapped.WrappedCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandManagerImpl implements CommandManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    
    // 核心组件
    private final CommandConfigFile configFile;
    private final CommandConfigLoader configLoader;
    private final CommandConfigApplicator configApplicator;
    
    private final CommandDispatcher dispatcher;
    
    // 存储已注册的命令节点，用于热重载
    private final Map<String, CommandNode> registeredCommands = new HashMap<>();

    public CommandManagerImpl(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        
        // 初始化配置系统
        this.configFile = new CommandConfigFile(plugin, logger);
        this.configLoader = new CommandConfigLoader();
        this.configApplicator = new CommandConfigApplicator();
        
        this.dispatcher = new CommandDispatcher();
    }

    @Override
    public void register(@NotNull Object commandInstance) {
        if (commandInstance instanceof CommandNode) {
            registerNode((CommandNode) commandInstance);
        } else {
            registerClass(commandInstance);
        }
    }
    
    @Override
    public void reload() {
        logger.info("Reloading command configurations...");
        
        // 1. 重新加载配置文件
        configFile.load();
        
        // 2. 遍历已注册的命令，重新应用配置
        for (Map.Entry<String, CommandNode> entry : registeredCommands.entrySet()) {
            String commandName = entry.getKey();
            CommandNode root = entry.getValue();
            
            ConfigurationSection section = configFile.getCommandSection(commandName);
            if (section != null) {
                try {
                    CommandConfig config = configLoader.load(section);
                    
                    // 注意：热重载暂时不支持动态禁用/启用整个命令（因为需要从 CommandMap 卸载）
                    // 但支持修改属性和子命令状态
                    if (config.isEnabled()) {
                         configApplicator.apply(root, config);
                         logger.debug("Reloaded configuration for command: " + commandName);
                    } else {
                        logger.warn("Command " + commandName + " is disabled in config, but cannot be fully unregistered dynamically.");
                    }
                } catch (Exception e) {
                    logger.error("Failed to reload config for command " + commandName, e);
                }
            }
        }
        
        logger.info("Command configurations reloaded.");
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */

    private void registerClass(@NotNull Object commandInstance) {
        registerNode(AnnotationParser.parse(commandInstance));
    }

    private void registerNode(@NotNull CommandNode root) {
        try {
            String commandName = root.getLiteral();
            
            // 记录已注册的命令
            registeredCommands.put(commandName, root);
            
            // 1. 获取或创建配置段，并合并新命令
            ConfigurationSection section = configFile.getCommandSection(commandName);
            if (section == null) {
                section = configFile.createCommandSection(commandName);
            }
            
            // 更新默认配置（如果代码中有新命令，会自动写入配置文件）
            updateConfigWithDefaults(root, section);
            configFile.save();

            // 2. 重新加载配置并应用
            // 注意：因为我们刚才可能修改了 section，最好重新获取一次或者直接使用当前的 section
            // 这里为了保险起见，使用 loader 从 section 加载
            CommandConfig config = configLoader.load(section);
            if (config != null) {
                // 3. 检查是否启用
                if (!config.isEnabled()) {
                    logger.warn("Command " + commandName + " is disabled in commands.yml");
                    return;
                }
                
                // 4. 应用配置到节点树
                configApplicator.apply(root, config);
            }

            // 5. 注册到 Bukkit
            registerToBukkit(root);

            logger.info("Registered command: " + commandName);
        } catch (Exception e) {
            logger.error("Failed to register command: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新配置文件的默认值（合并逻辑）
     * 仅当配置项不存在时才写入，保留用户的修改
     */
    private void updateConfigWithDefaults(CommandNode node, ConfigurationSection section) {
        // 仅保存基本信息（如果不存在）
        if (!section.contains("permission") && node.getPermission() != null) {
            section.set("permission", node.getPermission());
        }
        if (!section.contains("description") && node.getDescription() != null) {
            section.set("description", node.getDescription());
        }
        if (!section.contains("aliases") && node.getAliases() != null && !node.getAliases().isEmpty()) {
            section.set("aliases", node.getAliases());
        }
        if (!section.contains("enabled")) {
            section.set("enabled", true);
        }
        
        // 递归处理子命令
        ConfigurationSection subcommandsSection = section.getConfigurationSection("subcommands");
        
        for (CommandNode child : node.getChildren()) {
            if (child.isLiteral()) {
                if (subcommandsSection == null) {
                    subcommandsSection = section.createSection("subcommands");
                }
                
                String childName = child.getLiteral();
                ConfigurationSection childSection = subcommandsSection.getConfigurationSection(childName);
                if (childSection == null) {
                    childSection = subcommandsSection.createSection(childName);
                }
                
                updateConfigWithDefaults(child, childSection);
            }
        }
    }

    private void registerToBukkit(CommandNode root) {
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;
            
            // 创建包装命令并注册
            WrappedCommand wrappedCommand = new WrappedCommand(root.getLiteral(), root, dispatcher, logger);
            // 同步属性到 Bukkit Command
            wrappedCommand.setDescription(root.getDescription() != null ? root.getDescription() : "");
            wrappedCommand.setPermission(root.getPermission());
            wrappedCommand.setAliases(root.getAliases() != null ? root.getAliases() : Collections.emptyList());
            
            commandMap.register(plugin.getName(), wrappedCommand);
            
            // 注意：CommandMap.register 会自动处理别名，只要 wrappedCommand.setAliases 设置了即可。
            // 不需要手动循环注册别名，除非是为了兼容某些特殊情况。
            
        } catch (Exception e) {
            logger.error("Error registering command: " + e.getMessage(), e);
        }
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getPluginManager());
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            logger.error("Error occurred while getting CommandMap: {}", ex.getMessage(), ex);
            return null;
        }
    }
}
