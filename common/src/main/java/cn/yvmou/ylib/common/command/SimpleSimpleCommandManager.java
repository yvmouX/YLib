package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.common.command.core.AliasCommand;
import cn.yvmou.ylib.common.command.core.MainCommand;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.yvmou.ylib.common.YLibImpl.ylib;

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
public class SimpleSimpleCommandManager implements cn.yvmou.ylib.api.command.SimpleCommandManager {
    private final Plugin plugin;
    private final LoggerService logger;
    private final MessageService message;
    private final CommandConfig commandConfig;
    private final Map<String, MainCommand> registeredCommands = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> commandAliases = new ConcurrentHashMap<>();

    public SimpleSimpleCommandManager(Plugin plugin, LoggerService logger, MessageService message) {
        this.plugin = plugin;
        this.logger = logger;
        this.message = message;
        commandConfig = new CommandConfig(plugin, logger);
    }

    @Override
    public void registerCommands(@NotNull String commandName, @NotNull SubCommand... subCommandClass) {
        if (registeredCommands.containsKey(commandName)) {
            logger.warn(commandName + " is already registered!");
            unregisterCommands(commandName);
        }

        //注册主命令
        registerMainCommands(commandName, subCommandClass);

        // 注册别名命令
        ylib.getScheduler().runTask(() -> {
            registerAliasCommands(commandName);
        });
    }

    @Override
    public void unregisterCommands(@NotNull String commandName) {
        if (registeredCommands.containsKey(commandName)) {
            logger.error("Unregistered command: " + commandName);
            return;
        }

        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap != null) {
                // 注销主命令
                commandMap.getCommand(commandName);
                // 注销别名命令
                Set<String> aliases = commandAliases.get(commandName);
                if (aliases != null) {
                    for (String alias : aliases) {
                        commandMap.getCommand(alias);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to unregister command: " + commandName, e);
        }
    }

    private void registerMainCommands(@NotNull String commandName, @NotNull SubCommand... subCommandClass) {
        try {
            // 获取 CommandMap
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("无法获取 CommandMap，主命令注册失败。");
                return;
            }

            // 创建 PluginCommand 实例
            Constructor<PluginCommand> commandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            commandConstructor.setAccessible(true);
            PluginCommand pluginCommand = commandConstructor.newInstance(commandName, plugin);

            // 创建主命令执行器
            MainCommand mainCommandExecutor = new MainCommand(plugin, logger, message, commandName, subCommandClass);
            pluginCommand.setExecutor(mainCommandExecutor);

            // 注册命令到 CommandMap
            commandMap.register(ylib.getServerInfo().getPluginName().toLowerCase(), pluginCommand);
            logger.info("成功注册主命令" + commandName);

            // 保存注册信息
            registeredCommands.put(commandName, mainCommandExecutor);
        } catch (Exception e) {
            logger.error("注册命令时发生错误: " + e.getMessage());
        }
    }

    private void registerAliasCommands(String commandName) {
        MainCommand mainCommand = registeredCommands.get(commandName);
        if (mainCommand == null) {
            logger.error("mainCommand " + commandName + " not found，can't register alias!");
            return;
        }

        try {
            // 获取 CommandMap
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("can't find commandMap, register alias: " + commandName + "fail");
                return;
            }

            // 获取 PluginCommand 构造器
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            // 预先收集所有需要注册的命令
            List<PluginCommand> commandsToRegister = new ArrayList<>();

            // 遍历配置文件中的命令
            FileConfiguration commandsConfig = ylib.getCommandConfig().getConfig();
            ConfigurationSection configurationSection = commandsConfig.getConfigurationSection("commands." + commandName);

            if (configurationSection == null) {
                logger.error("command " + commandName + " not found");
                return;
            }

            Set<String> allCmd = configurationSection.getKeys(false);

            String basePath = "commands." + commandName;
            for (String cmd : allCmd) {
                boolean enabled = commandsConfig.getBoolean(basePath + "." + cmd + ".register", true);
                List<String> allAliasList = commandsConfig.getStringList(basePath + "." + cmd + ".alias");

                for (String alias : allAliasList) {
                    String a = alias.trim();
                    if (enabled && !a.isEmpty()) {
                        // 创建命令实例
                        PluginCommand pluginCommand = constructor.newInstance(a, plugin);
                        pluginCommand.setExecutor(new AliasCommand(plugin, logger, commandName, cmd));

                        commandsToRegister.add(pluginCommand);
                    }
                }

                // 批量注册所有命令
                for (PluginCommand cmdObj : commandsToRegister) {
                    commandMap.register(ylib.getServerInfo().getPluginName().toLowerCase(), cmdObj);
                    logger.info("已注册命令别名：/" + cmdObj.getName() + " -> /" + commandName);
                }
            }
        } catch (Exception e) {
            logger.warn("注册命令别名时发生错误: " + e.getMessage());
        }
    }

    @Override
    public CommandConfig getCommandConfig() {
        return commandConfig;
    }

    @Override
    public boolean isCommandRegistered(@NotNull String commandName) {
        return registeredCommands.containsKey(commandName);
    }

    @Override
    public int getRegisteredCommandCount() {
        return registeredCommands.size();
    }

    @Override
    public String[] getRegisteredCommands() {
        return registeredCommands.keySet().toArray(new String[0]);
    }

    @Override
    public Set<String> getRegisteredCommandNames() {
        return new HashSet<>(registeredCommands.keySet());
    }

    @Override
    public Set<String> getCommandAliases(@NotNull String commandName) {
        return commandAliases.get(commandName);
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getPluginManager());
        } catch (Exception ex) {
            logger.error("无法获取 CommandMap，请将此问题报告给开发者: " + ex.getMessage());
            return null;
        }
    }
}
