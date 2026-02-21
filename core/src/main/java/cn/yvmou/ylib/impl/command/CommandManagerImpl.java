package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.command.tree.CommandNode;
import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.impl.command.core.AnnotationParser;
import cn.yvmou.ylib.impl.command.core.CommandDispatcher;
import cn.yvmou.ylib.impl.command.core.YLibCommandAdapter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;

public class CommandManagerImpl implements CommandManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final CommandConfig commandConfig;
    
    private final AnnotationParser annotationParser;
    private final CommandDispatcher dispatcher;

    public CommandManagerImpl(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.commandConfig = new cn.yvmou.ylib.impl.command.CommandConfig(plugin, logger);
        this.annotationParser = new AnnotationParser();
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

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */

    private void registerClass(@NotNull Object commandInstance) {
        registerNode(annotationParser.parse(commandInstance));
    }

    private void registerNode(@NotNull CommandNode root) {
        try {
            String commandName = root.getLiteral();
            // 1. 初始化配置（仅无该命令相关配置时）
            List<String> nodeAliases = root.getAliases();
            commandConfig.initCommand(
                    commandName,
                    root.getDescription(),
                    root.getPermission(),
                    nodeAliases,
                    root.getChildren()
            );

            // 2. 检查是否启用
            if (!commandConfig.isEnabled(commandName)) {
                logger.warn("Command " + commandName + " is disabled in commands.yml");
                return;
            }

            // 3. 应用配置覆盖（别名、描述、权限）
            List<String> configAliases = commandConfig.getAliases(commandName);
            if (!configAliases.isEmpty()) root.aliases(configAliases);

            String configDesc = commandConfig.getDescription(commandName);
            if (!configDesc.isEmpty()) root.description(configDesc);

            String configPerm = commandConfig.getPermission(commandName);
            if (!configPerm.isEmpty()) root.permission(configPerm);

            List<CommandNode> configChildren = commandConfig.getChildren(commandName);
            if (!configChildren.isEmpty()) {
                for (CommandNode child : configChildren) {
                    root.then(child);
                }
            }

            // 4. 注册到 Bukkit
            registerToBukkit(root);

            logger.info("Registered command: " + commandName);
        } catch (Exception e) {
            logger.error("Failed to register command: " + e.getMessage(), e);
        }
    }

    private void registerToBukkit(CommandNode root) {
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;
            
            // 注册主命令
            YLibCommandAdapter command = new YLibCommandAdapter(
                root.getLiteral(), 
                plugin, 
                root, 
                dispatcher
            );
            commandMap.register(plugin.getName(), command);
            
            // 注册别名
            List<String> aliases = commandConfig.getAliases(root.getLiteral());
            for (String alias : aliases) {
                YLibCommandAdapter aliasCommand = new YLibCommandAdapter(
                    alias, 
                    plugin, 
                    root, 
                    dispatcher
                );
                commandMap.register(plugin.getName(), aliasCommand);
                logger.debug("Registered alias: " + alias + " -> " + root.getLiteral());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
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
