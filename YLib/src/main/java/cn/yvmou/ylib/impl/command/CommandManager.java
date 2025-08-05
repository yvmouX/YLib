package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.tools.LoggerTools;
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

import static cn.yvmou.ylib.YLib.yLib;

public class CommandManager {
    private final Plugin plugin;
    private final LoggerTools logger = yLib.getLoggerTools();

    public CommandManager(Plugin plugin) { this.plugin = plugin; }

    /**
     *
     * @param commandName 需要注册的主命令的名称
     * @param subCommandClass 该主命令所有子命令的类
     */
    public void registerCommands(@NotNull String commandName, @NotNull SubCommand... subCommandClass) {
        //注册主命令
        registerMainCommands(commandName, subCommandClass);

        // 注册别名命令
        yLib.getScheduler().runTask(() -> {
            registerAliasCommands(commandName);
        });
    }

    private void registerMainCommands(@NotNull String commandName, @NotNull SubCommand... subCommandClass) {
        try {
            // 获取 CommandMap
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("无法获取 CommandMap，主命令注册失败。");
                return;
            }

            Constructor<PluginCommand> commandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            commandConstructor.setAccessible(true);

            PluginCommand pluginCommand = commandConstructor.newInstance(commandName, plugin);
            pluginCommand.setExecutor(new MainCommand(plugin, commandName, subCommandClass));

            commandMap.register(YLib.getyLib().getPrefix(), pluginCommand);
            logger.info("成功注册主命令" + commandName);
        } catch (Exception e) {
            logger.error("注册命令时发生错误: " + e.getMessage());
        }
    }

    private void registerAliasCommands(String commandName) {
        Map<String, String> aliases = new HashMap<>();
        try {
            // 获取 CommandMap
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.warn("无法获取 CommandMap，别名命令注册失败。");
                return;
            }

            // 获取 PluginCommand 构造器
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            // 预先收集所有需要注册的命令
            List<PluginCommand> commandsToRegister = new ArrayList<>();

            // 遍历配置文件中的命令
            FileConfiguration commandsConfig = YLib.getyLib().getCommandConfig().getConfig();
            ConfigurationSection configurationSection = commandsConfig.getConfigurationSection("commands." + commandName);
            if (configurationSection == null) return;
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
                        pluginCommand.setExecutor(new AliasCommand(plugin, commandName, cmd));

                        commandsToRegister.add(pluginCommand);
                    }
                }
            }

            // 批量注册所有命令
            for (PluginCommand cmdObj : commandsToRegister) {
                commandMap.register(YLib.getyLib().getPluginName(), cmdObj);
                logger.info("已注册命令别名：/" + cmdObj.getName() + " -> /" + commandName);
            }

        } catch (Exception e) {
            logger.warn("注册命令别名时发生错误: " + e.getMessage());
        }
    }


    private CommandMap getCommandMap() {
        try {
            // paper
            return Bukkit.getCommandMap();
        } catch (Exception e) {
            // Spigot
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
}
