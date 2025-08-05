package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.api.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单命令管理器实现
 * <p>
 * 提供完整的命令注册和管理功能，包括主命令注册、别名命令注册、
 * 权限检查、配置管理等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class SimpleCommandManager implements CommandManager {
    
    private final Plugin plugin;
    private final LoggerService logger;
    private final MessageService messageService;
    private final Map<String, MainCommand> registeredCommands = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> commandAliases = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param logger 日志服务
     * @param messageService 消息服务
     */
    public SimpleCommandManager(@NotNull Plugin plugin, @NotNull LoggerService logger, 
                               @NotNull MessageService messageService) {
        this.plugin = plugin;
        this.logger = logger;
        this.messageService = messageService;
    }
    
    @Override
    public void registerCommands(@NotNull String commandName, @NotNull SubCommand... subCommands) {
        if (registeredCommands.containsKey(commandName)) {
            logger.warning("命令 " + commandName + " 已经注册，将覆盖原有注册");
            unregisterCommand(commandName);
        }
        
        try {
            // 注册主命令
            registerMainCommand(commandName, subCommands);
            
            // 延迟注册别名命令（确保主命令已完全注册）
            Bukkit.getScheduler().runTask(plugin, () -> registerAliasCommands(commandName));
            
            logger.info("成功注册命令: " + commandName + " (包含 " + subCommands.length + " 个子命令)");
            
        } catch (Exception e) {
            logger.error("注册命令 " + commandName + " 时发生错误: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void unregisterCommand(@NotNull String commandName) {
        MainCommand mainCommand = registeredCommands.remove(commandName);
        if (mainCommand == null) {
            logger.warning("尝试注销未注册的命令: " + commandName);
            return;
        }
        
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap != null) {
                // 注销主命令
                commandMap.getCommand(commandName);
                
                // 注销别名命令
                Set<String> aliases = commandAliases.remove(commandName);
                if (aliases != null) {
                    for (String alias : aliases) {
                        commandMap.getCommand(alias);
                    }
                }
            }
            
            logger.info("成功注销命令: " + commandName);
            
        } catch (Exception e) {
            logger.error("注销命令 " + commandName + " 时发生错误: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isCommandRegistered(@NotNull String commandName) {
        return registeredCommands.containsKey(commandName);
    }
    
    @Override
    public int getRegisteredCommandCount() {
        return registeredCommands.size();
    }
    
    /**
     * 注册主命令
     * 
     * @param commandName 命令名称
     * @param subCommands 子命令数组
     */
    private void registerMainCommand(@NotNull String commandName, @NotNull SubCommand... subCommands) 
            throws Exception {
        
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            throw new RuntimeException("无法获取 CommandMap，主命令注册失败");
        }
        
        // 创建 PluginCommand 实例
        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        PluginCommand pluginCommand = constructor.newInstance(commandName, plugin);
        
        // 创建主命令执行器
        MainCommand mainCommandExecutor = new MainCommand(plugin, commandName, logger, messageService, subCommands);
        pluginCommand.setExecutor(mainCommandExecutor);
        
        // 注册命令到 CommandMap
        commandMap.register(plugin.getName().toLowerCase(), pluginCommand);
        
        // 保存注册信息
        registeredCommands.put(commandName, mainCommandExecutor);
        
        logger.debug("主命令 " + commandName + " 注册成功");
    }
    
    /**
     * 注册别名命令
     * 
     * @param commandName 主命令名称
     */
    private void registerAliasCommands(@NotNull String commandName) {
        MainCommand mainCommand = registeredCommands.get(commandName);
        if (mainCommand == null) {
            logger.warning("主命令 " + commandName + " 不存在，无法注册别名");
            return;
        }
        
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.warning("无法获取 CommandMap，别名命令注册失败");
                return;
            }
            
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            
            FileConfiguration config = mainCommand.getCommandConfig().getConfig();
            ConfigurationSection commandSection = config.getConfigurationSection("commands." + commandName);
            
            if (commandSection == null) {
                logger.debug("命令 " + commandName + " 没有配置信息，跳过别名注册");
                return;
            }
            
            Set<String> aliases = new HashSet<>();
            Set<String> subCommandNames = commandSection.getKeys(false);
            
            for (String subCommandName : subCommandNames) {
                if ("_info".equals(subCommandName)) continue;
                
                String basePath = "commands." + commandName + "." + subCommandName;
                boolean enabled = config.getBoolean(basePath + ".register", true);
                
                if (!enabled) {
                    logger.debug("子命令 " + subCommandName + " 已禁用，跳过别名注册");
                    continue;
                }
                
                List<String> aliasList = config.getStringList(basePath + ".alias");
                
                for (String alias : aliasList) {
                    String trimmedAlias = alias.trim();
                    if (trimmedAlias.isEmpty() || trimmedAlias.equals(subCommandName)) {
                        continue;
                    }
                    
                    try {
                        // 创建别名命令
                        PluginCommand aliasCommand = constructor.newInstance(trimmedAlias, plugin);
                        aliasCommand.setExecutor(new AliasCommand(plugin, commandName, subCommandName, logger));
                        
                        // 注册别名命令
                        commandMap.register(plugin.getName().toLowerCase(), aliasCommand);
                        aliases.add(trimmedAlias);
                        
                        logger.debug("注册别名命令: /" + trimmedAlias + " -> /" + commandName + " " + subCommandName);
                        
                    } catch (Exception e) {
                        logger.warning("注册别名 " + trimmedAlias + " 失败: " + e.getMessage());
                    }
                }
            }
            
            if (!aliases.isEmpty()) {
                commandAliases.put(commandName, aliases);
                logger.info("为命令 " + commandName + " 注册了 " + aliases.size() + " 个别名");
            }
            
        } catch (Exception e) {
            logger.error("注册别名命令时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取 CommandMap
     * 
     * @return CommandMap 实例，如果获取失败返回null
     */
    private CommandMap getCommandMap() {
        try {
            // 使用反射方法获取 CommandMap (兼容所有平台)
            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            logger.error("无法获取 CommandMap: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取已注册的命令列表
     * 
     * @return 命令名称列表
     */
    @Override
    @NotNull
    public String[] getRegisteredCommands() {
        return registeredCommands.keySet().toArray(new String[0]);
    }
    
    /**
     * 获取已注册的命令名称集合
     * 
     * @return 命令名称集合
     */
    @NotNull
    public Set<String> getRegisteredCommandNames() {
        return new HashSet<>(registeredCommands.keySet());
    }
    
    /**
     * 获取命令的别名列表
     * 
     * @param commandName 命令名称
     * @return 别名列表，如果没有别名返回空集合
     */
    @NotNull
    public Set<String> getCommandAliases(@NotNull String commandName) {
        return commandAliases.getOrDefault(commandName, Collections.emptySet());
    }
    
    /**
     * 获取主命令执行器
     * 
     * @param commandName 命令名称
     * @return 主命令执行器，如果不存在返回null
     */
    public MainCommand getMainCommand(@NotNull String commandName) {
        return registeredCommands.get(commandName);
    }
    
    /**
     * 重新加载所有命令配置
     */
    public void reloadAllConfigs() {
        for (Map.Entry<String, MainCommand> entry : registeredCommands.entrySet()) {
            try {
                entry.getValue().getCommandConfig().reloadConfig();
                logger.debug("重新加载命令配置: " + entry.getKey());
            } catch (Exception e) {
                logger.error("重新加载命令配置失败: " + entry.getKey() + " - " + e.getMessage(), e);
            }
        }
        logger.info("所有命令配置已重新加载");
    }
    
    /**
     * 获取命令统计信息
     * 
     * @return 统计信息字符串
     */
    @NotNull
    public String getStatistics() {
        int totalCommands = registeredCommands.size();
        int totalAliases = commandAliases.values().stream().mapToInt(Set::size).sum();
        int totalSubCommands = 0;
        
        for (MainCommand mainCommand : registeredCommands.values()) {
            totalSubCommands += mainCommand.getRegisteredSubCommands().size();
        }
        
        return String.format("命令统计: %d 个主命令, %d 个子命令, %d 个别名", 
            totalCommands, totalSubCommands, totalAliases);
    }
}