package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SimpleCommandManager;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.api.services.ServerInfoService;
import cn.yvmou.ylib.impl.command.core.AliasCommand;
import cn.yvmou.ylib.impl.command.core.MainCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleCommandManagerImpl implements SimpleCommandManager {
    private final JavaPlugin plugin;
    private final UniversalScheduler scheduler;
    private final LoggerService logger;
    private final MessageService message;
    private final ServerInfoService serverInfo;
    private final CommandConfig commandConfig;

    public SimpleCommandManagerImpl(JavaPlugin plugin, UniversalScheduler scheduler, LoggerService logger, MessageService message, ServerInfoService serverInfo, CommandConfig commandConfig) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.logger = logger;
        this.message = message;
        this.serverInfo = serverInfo;
        this.commandConfig = commandConfig;
    }

    @Override
    public void registerCommands(@NotNull final String mainCommandName, @NotNull final SubCommand... subCommandClass) {
        // 已在 命令文件 中标记为 register: true 的需要注册的 命令名称和SubCommand 之间的 MAP
        final Map<String, SubCommand> requireRegisterConfigSubCommandClassMap = new HashMap<>();
        // 已在 方法中使用 CommandOptions 注解声明的 命令名称和SubCommand 之间的 MAP
        final Map<String, SubCommand> defSubCommandClassMap = new ConcurrentHashMap<>();
        // 从 命令文件 获取到的子命令列表
        final List<String> configSubCommandList = commandConfig.getSubCommandList(mainCommandName);

        // 初始化 命令文件
        try {
            commandConfig.loadCommandConfiguration();
        } catch (Exception e) {
            logger.error("初始化命令文件时发生错误: " + e.getMessage());
        }

        // 获取所有 声明 的子命令
        for (SubCommand sc : subCommandClass) {
            try {
                Method method = sc.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);
                CommandOptions co = method.getAnnotation(CommandOptions.class);
                String commandName = co.name().trim().toLowerCase();

                // 确保 子命令类 在 execute 方法上 实现 @CommandOptions 注解
                if (!method.isAnnotationPresent(CommandOptions.class)) {
                    logger.error(
                            String.format("子命令类 %s 必须在其 execute 方法上使用 @CommandOptions 注解%n" +
                                            "如果你不是开发者，请无视这条警告。",
                                    sc.getClass().getSimpleName())
                    );
                    continue;
                }

                // 确保 子命令类 的 @CommandOptions 注解 中 name 属性不为空
                if (commandName.isEmpty()) {
                    logger.error(
                            String.format("子命令类 %s 的 @CommandOptions 注解中的 name 属性不能为空%n" +
                                            "如果你不是开发者，请无视这条警告。",
                                    sc.getClass().getSimpleName())
                    );
                    continue;
                }
                defSubCommandClassMap.put(commandName, sc);
            } catch (NoSuchMethodException e) {
                logger.error(
                        String.format("命令 %s 的子命令 %s 必须实现 execute(CommandSender, String[]) 方法%n" +
                                        "如果你不是开发者，请无视这条警告，并检查commands.yml%n" + e.getMessage(),
                                mainCommandName, sc)
                );
            }

        }

        // 初始化所有声明的子命令，写入 命令文件
        commandConfig.initCommandConfig(mainCommandName, defSubCommandClassMap);


        // 从 命令文件 获取到所有的子命令的列表
        for (String subCommandName : configSubCommandList) {
            for (SubCommand subCommand : subCommandClass) {
                // 检查 命令文件下 register 是否为 true，的情况下才进行注册
                if (commandConfig.isCommandRegister(mainCommandName, subCommandName)) {
                    requireRegisterConfigSubCommandClassMap.put(subCommandName, subCommand);
                    logger.debug(
                            String.format("正在准备注册命令: /%s %s", mainCommandName, subCommandName)
                    );
                } else {
                    logger.warn(
                            String.format("正在准备移除命令: /%s %s", mainCommandName, subCommandName)
                    );
                }
            }
        }

        // 尝试注册主命令
        try {
            registerMainCommands(mainCommandName, requireRegisterConfigSubCommandClassMap);
        } catch (Exception e) {
            logger.error("注册命令时发生错误: " + e.getMessage());
        }

        // 注册别名命令
        try {
            scheduler.runTask(() -> {
                try {
                    registerAliasCommands(mainCommandName, configSubCommandList);
                } catch (Exception e) {
                    logger.error("注册别名命令时发生错误: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("调度别名命令注册任务时发生错误: " + e.getMessage());
        }
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    /**
     * 注册命令
     *
     * @param commandName         命令名称
     * @param requireRegisterConfigSubCommandClassMap 子命令类列表
     */
    private void registerMainCommands(@NotNull final String commandName, @NotNull final Map<String, SubCommand> requireRegisterConfigSubCommandClassMap) {
        // 反射获取 Bukkit 内部命令管理器组件 CommandMap, 手动构建 PluginCommand 实例，并绑定命令执行器后注册命令
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("getCommandMap() Failed. Please report this issue.");
                return;
            }

            Constructor<PluginCommand> commandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            commandConstructor.setAccessible(true);
            PluginCommand pluginCommand = commandConstructor.newInstance(commandName, plugin);

            pluginCommand.setExecutor(new MainCommand(logger, message, commandName, requireRegisterConfigSubCommandClassMap, serverInfo, commandConfig));

            commandMap.register(serverInfo.getPluginName(), pluginCommand);
            logger.debug("Successfully registered MainCommand: " + commandName);
        } catch (Exception e) {
            logger.error("Error occurred while registering the MainCommand: " + commandName + " " + e.getMessage());
        }
    }

    /**
     * 注册命令别名
     *
     * <p>
     *     eg:
     *       /yLib reload 主命令是 yLib，子命令是 reload
     *       假设所创建的命令别名为 rl
     *       那么/rl = /yLib reload
     * </p>
     *
     * @param mainCommandName 命令名称
     */
    private void registerAliasCommands(@NotNull final String mainCommandName, @NotNull final List<String> configSubCommandList) {
        // 首先获取到 register: true 的项
        final List<String> requireRegisterConfigSubCommandList = new ArrayList<>();
        for (String subCommand : configSubCommandList) {
            if (commandConfig.isCommandRegister(mainCommandName, subCommand)) {
                requireRegisterConfigSubCommandList.add(subCommand);
            }
        }

        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                logger.error("getCommandMap() Failed. Please report this issue.");
                return;
            }

            // 获取 PluginCommand 构造器
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            // 预先收集所有需要注册的命令 和 原命令的映射 eg：/yess tp -> /tp
            // Map = 新命令 : 原命令的子命令
            Map<PluginCommand, String> commandsToRegister = new ConcurrentHashMap<>();

            // 先收集所有需要注册的命令
            for (String subCommand : requireRegisterConfigSubCommandList) {
                // 获取该 主命令的子命令 的所有别名
                List<String> alias = commandConfig.getCommandAliases(mainCommandName, subCommand);

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
                commandMap.register(serverInfo.getPluginPrefix(), entry.getKey());
                willInfo.add(entry.getKey().getName());
                willSubCommandInfo = entry.getValue();
                logger.debug("已注册命令别名：/" + entry.getKey().getName() + " -> /" + mainCommandName + " " + entry.getValue());
            }
            if (willSubCommandInfo != null) {
                logger.debug(
                        String.format("已为 /%s %s 创建别名 -> " + willInfo,
                                mainCommandName, willSubCommandInfo)
                );
            }
        } catch (Exception e) {
            logger.warn("Error occurred while registering the AliasCommand: " + e.getMessage());
        }
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getPluginManager());
        } catch (Exception ex) {
            logger.error("Error occurred while registering the CommandMap, please report this issue: " + ex.getMessage());
            return null;
        }
    }
}
