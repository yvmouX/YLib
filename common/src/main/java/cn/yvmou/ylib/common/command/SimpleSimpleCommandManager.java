package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.common.command.core.AliasCommand;
import cn.yvmou.ylib.common.command.core.MainCommand;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private final Map<String, SubCommand> subCommandClassList = new HashMap<>(); // 子命令名称 : SubCommand

    public SimpleSimpleCommandManager(Plugin plugin, LoggerService logger, MessageService message, CommandConfig commandConfig) {
        this.plugin = plugin;
        this.logger = logger;
        this.message = message;
        this.commandConfig = commandConfig;
    }

    @Override
    public void registerCommands(@NotNull String mainCommandName, @NotNull SubCommand... subCommandClass) {
        subCommandClassList.clear();
        // 获取到所有的子命令
        List<String> configSubCommandList = getCommandConfig().getSubCommandList(mainCommandName);
        for (String subCommandName : configSubCommandList) {
            for (SubCommand subCommand : subCommandClass) {
                CommandOptions commandOptions = null;
                String optionCommandName;
                // 确保 subCommandName 正确实现
                try {
                    Method method = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);
                    commandOptions = method.getAnnotation(CommandOptions.class);
                    optionCommandName = commandOptions.name().trim().toLowerCase();

                    // 确保 子命令类 在 execute 方法上 实现 @CommandOptions 注解
                    if (!method.isAnnotationPresent(CommandOptions.class)) {
                        logger.error(
                                String.format("子命令类 %s 必须在其 execute 方法上使用 @CommandOptions 注解%n" +
                                                "如果你不是开发者，请无视这条警告。",
                                        subCommand.getClass().getSimpleName())
                        );
                        continue;
                    }

                    // 确保 子命令类 的 @CommandOptions 注解 中 name 属性不为空
                    if (optionCommandName.isEmpty()) {
                        logger.error(
                                String.format("子命令类 %s 的 @CommandOptions 注解中的 name 属性不能为空%n" +
                                                "如果你不是开发者，请无视这条警告。",
                                        subCommand.getClass().getSimpleName())
                        );
                        continue;
                    }

                    // 防止用户 在配置文件添加无用配置
                    if (!optionCommandName.equals(subCommandName)) {
                        logger.error(
                                String.format("命令 /%s %s 似乎并没有被实现？%n" +
                                                "请检查commands.yml，移除相关配置。",
                                        mainCommandName, subCommandName)
                        );
                        continue;
                    }
                } catch (NoSuchMethodException e) {
                    logger.error(
                            String.format("命令 %s 的子命令 %s 必须实现 execute(CommandSender, String[]) 方法%n" +
                                            "如果你不是开发者，请无视这条警告，并检查commands.yml%n" + e.getMessage(),
                                    mainCommandName, subCommandName)
                    );
                }
                subCommandClassList.put(subCommandName, subCommand);

                // 二次检查
                // 移除未注册的子命令
                Set<String> subs = subCommandClassList.keySet();
                List<String> willInfo = new ArrayList<>();
                for (String sub : subs) {
                    boolean isRegister = ylib.getSimpleCommandManager().getCommandConfig().isCommandRegister(mainCommandName, sub);
                    if (!isRegister) {
                        subCommandClassList.remove(sub.toLowerCase());
                        willInfo.add(sub);
                    }
                }

                if (!subCommandClassList.isEmpty()) {
                    logger.info(
                            String.format("添加到 %s 的子命令注册列表: " + subCommandClassList.keySet(),
                                    mainCommandName)
                    );
                } else {
                    logger.warn(
                            String.format("已移除 %s 未注册的子命令: " + willInfo,
                                    mainCommandName)
                    );
                }
            }
        }

        // 初始化 CommandConfig
        // 1、创建commands.yml（如果配置文件不存在）
        // 2、初始commands.yml 内容（如果该路径下没有内容），强制继承自 SubCommand 的方法必须实现 CommandOptions 注解
        //logger.info("正在初始化命令配置...");
        try {
            getCommandConfig().initConfigFile();
            getCommandConfig().initCommandConfig(mainCommandName, subCommandClassList);
        } catch (Exception e) {
            logger.error("初始化命令配置时发生错误: " + e.getMessage());
        }

        // 注册主命令
        try {
            //logger.info("开始注册主命令...");
            registerMainCommands(mainCommandName, subCommandClassList);
            //logger.info("主命令注册完成");
        } catch (Exception e) {
            logger.error("注册命令时发生错误: " + e.getMessage());
        }

        // 注册别名命令
        try {
            //logger.info("开始注册别名命令...");
            ylib.getScheduler().runTask(() -> {
                try {
                    registerAliasCommands(mainCommandName);
                    //logger.info("别名命令注册完成");
                } catch (Exception e) {
                    logger.error("注册别名命令时发生错误: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("调度别名命令注册任务时发生错误: " + e.getMessage());
        }
    }

    private void registerMainCommands(@NotNull String commandName, @NotNull Map<String, SubCommand> subCommandClassList) {
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
            pluginCommand.setExecutor(new MainCommand(logger, message, commandName, subCommandClassList));

            // 注册命令到 CommandMap
            commandMap.register(ylib.getServerInfo().getPluginName().toLowerCase(), pluginCommand);
            //logger.info("成功注册主命令: " + commandName);
        } catch (Exception e) {
            logger.error("注册命令时发生错误: " + e.getMessage());
        }
    }

    /**
     * 为主命令的子命令创建 命令别名 X (reload、rl、restart...)
     * 例子：
     * /yess reload 主命令是 yess，子命令是 reload
     * 假设所创建的命令别名为 rl
     * 那么/rl = /yess reload
     *
     * @param mainCommandName 命令名称
     */
    private void registerAliasCommands(String mainCommandName) {
        try {
            // 获取 CommandMap
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("can't find commandMap, register alias: " + mainCommandName + "fail");
                return;
            }

            // 获取 PluginCommand 构造器
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            // 预先收集所有需要注册的命令 和 原命令的映射 eg：/yess tp -> /tp
            // 新命令 : 原命令的子命令
            Map<PluginCommand, String> commandsToRegister = new ConcurrentHashMap<>();
            // 遍历配置文件中的命令
            List<String> subCommandList = getCommandConfig().getSubCommandList(mainCommandName);

            // 先收集所有需要注册的命令
            for (String subCommand : subCommandList) {
                // 确保该 子命令 是启用的
                boolean enable = getCommandConfig().isCommandRegister(mainCommandName, subCommand);
                if (!enable) continue;
                
                // 获取该 主命令的子命令 的所有别名
                List<String> alias = getCommandConfig().getCommandAliases(mainCommandName, subCommand);

                for (String alia : alias) {
                    String a = alia.trim().toLowerCase();
                    PluginCommand pluginCommand = constructor.newInstance(a, plugin);
                    pluginCommand.setExecutor(new AliasCommand(plugin, mainCommandName, subCommand)); // 使用 AliasCommand 处理该命令
                    commandsToRegister.put(pluginCommand, subCommand);
                }
            }

            // 统一注册所有收集到的命令
            List<String> willInfo = new ArrayList<>();
            String willSubCommandInfo = null;
            for (Map.Entry<PluginCommand, String> entry : commandsToRegister.entrySet()) {
                commandMap.register(ylib.getServerInfo().getPluginPrefix(), entry.getKey());
                willInfo.add(entry.getKey().getName());
                willSubCommandInfo = entry.getValue();
//                logger.info("已注册命令别名：/" + entry.getKey().getName() + " -> /" + mainCommandName + " " + entry.getValue());
            }
            if (willSubCommandInfo != null) {
                logger.info(
                        String.format("已为 /%s %s 创建别名 -> " + willInfo,
                                mainCommandName, willSubCommandInfo)
                );
            }
        } catch (Exception e) {
            logger.warn("注册命令别名时发生错误: " + e.getMessage());
        }
    }

    @Override
    public CommandConfig getCommandConfig() {
        return commandConfig;
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
