package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.common.services.LoggerService;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 命令配置管理器
 * <p>
 * 负责管理命令的配置文件，包括自动生成配置、读取配置等功能。
 * 支持命令别名、权限、使用限制等配置。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class CommandConfig implements cn.yvmou.ylib.api.command.CommandConfig {
    private final Plugin plugin;
    private final LoggerService logger;

    private File configFile;
    private FileConfiguration config;

    public CommandConfig(Plugin plugin, LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    /**
     * 加载配置文件
     */
    public Map<String, SubCommand> loadConfig(String mainCommandName, Map<String, SubCommand> subCommandMap) {
        try {
            initConfigFile();
        } catch (IOException e) {
            logger.error("Could not load commands.yml: " + e.getMessage());
        }

        return initCommandConfig(mainCommandName, subCommandMap);
    }

    /**
     * 初始化配置文件
     * 如果配置文件不存在，则创建一个
     *
     * @throws IOException ioexception
     */
    private void initConfigFile() throws IOException {
        // 如果配置文件或配置对象为null，初始化它
        if (configFile == null || config == null) {
            // 确保插件数据目录存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            configFile = new File(plugin.getDataFolder(), "commands.yml");

            // 如果commands.yml不存在，创建一个
            if (!configFile.exists()) {
                configFile.createNewFile();
                logger.info("commands.yml has been created: " + configFile.getAbsolutePath());
            }

            // 加载配置文件
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    /**
     * 初始化命令配置
     * <p>
     * 根据子命令的注解信息，在配置文件中创建相应的配置项。
     * 如果配置已存在，则不会覆盖。
     * </p>
     * 
     * @param mainCommandName 主命令名称
     * @param subCommandMap 子命令映射，键为子命令名称，值为子命令实例
     * @return 处理后的子命令映射
     */
    private Map<String, SubCommand> initCommandConfig(String mainCommandName, Map<String, SubCommand> subCommandMap) {
        boolean configChanged = false;
        Map<String, SubCommand> newSubCommandMap = subCommandMap;


        for (SubCommand subCommand : subCommandMap.values()) {
            // 强制验证注解存在，如果不存在，则移除改 subCommand
            if (!validateCommandAnnotation(subCommand)) {
                newSubCommandMap.remove(mainCommandName, subCommand);
            }

            // 如果命令路径下没有配置 创建默认配置
            try {
                Method executeMethod = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);
                // 获取子命令的注解参数
                CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
                String basePath = "commands." + mainCommandName + "." + options.name();
                // 检查配置是否存在
                if (!config.contains(basePath)) {
                    config.set(basePath + ".permission", options.permission().isEmpty() ? "none" : options.permission());
                    config.set(basePath + ".onlyPlayer", options.onlyPlayer());
                    config.set(basePath + ".register", options.register());
                    config.set(basePath + ".usage", options.usage().isEmpty() ? "none" : options.usage());
                    config.set(basePath + ".alias", options.alias().length < 1 ? new ArrayList<>() : Arrays.asList(options.alias()));

                    configChanged = true;
                    logger.info("已为命令 " + mainCommandName + " 的子命令 " + options.name() + " 创建默认配置");
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("子命令必须实现 execute(CommandSender, String[]) 方法", e);
            }
        }

        // 保存默认配置
        if (configChanged) {
            try {
                config.save(configFile);
                logger.info("commands.yml 配置文件已保存");
            } catch (IOException e) {
                logger.error("Failed to save commands.yml", e);
            }
        }

        return newSubCommandMap;
    }

    /**
     * 验证子命令的注解
     * <p>
     * 强制要求所有子命令必须使用 @CommandOptions 注解，
     * 否则抛出异常阻止插件加载
     * </p>
     *
     * @param subCommand 子命令实例
     * @throws IllegalStateException 如果注解验证失败
     */
    private boolean validateCommandAnnotation(SubCommand subCommand) {
        try {
            Method executeMethod = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);

            if (!executeMethod.isAnnotationPresent(CommandOptions.class)) {
                logger.error(
                        String.format("子命令类 %s 必须在其 execute 方法上使用 @CommandOptions 注解",
                                subCommand.getClass().getSimpleName())
                );
                return false;
            }

            CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
            if (options.name() == null || options.name().trim().isEmpty()) {
                logger.error(
                        String.format("子命令类 %s 的 @CommandOptions 注解中的 name 属性不能为空",
                                subCommand.getClass().getSimpleName())
                );
                return false;
            }

        } catch (NoSuchMethodException e) {
            logger.error(
                    String.format("子命令类 %s 必须实现 execute(CommandSender, String[]) 方法",
                            subCommand.getClass().getSimpleName()), e
            );
            return false;
        }
        return true;
    }

    private void checkConfig() {
        try {
            initConfigFile();
        } catch (IOException e) {
            logger.error("初始化配置文件出错: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileConfiguration getConfig() {
        checkConfig();
        return config;
    }

    @Override
    public List<String> getSubCommandList(String mainCommandName) {
        checkConfig();
        ConfigurationSection configSection = config.getConfigurationSection("commands." + mainCommandName);
        if (configSection == null) {
            return null;
        }

        return new ArrayList<>(configSection.getKeys(false));
    }

    @Override
    @NotNull
    public List<String> getCommandAliases(@NotNull String mainCommandName, @NotNull String subCommandName) {
        checkConfig();
        String path = "commands." + mainCommandName + "." + subCommandName + ".alias";
        return config.getStringList(path);
    }

    @Override
    @NotNull
    public String getCommandUsage(@NotNull String mainCommandName, @NotNull String subCommandName) {
        checkConfig();
        String path = "commands." + mainCommandName + "." + subCommandName + ".usage";
        return config.getString(path, "/" + mainCommandName + " " + subCommandName);
    }

    @Override
    public boolean isPlayerOnly(@NotNull String mainCommandName, @NotNull String subCommandName) {
        checkConfig();
        String path = "commands." + mainCommandName + "." + subCommandName + ".onlyPlayer";
        return config.getBoolean(path, false);
    }

    @Override
    @NotNull
    public String getCommandPermission(@NotNull String mainCommandName, @NotNull String subCommandName) {
        checkConfig();
        String path = "commands." + mainCommandName + "." + subCommandName + ".permission";
        return config.getString(path, "");
    }

    @Override
    public boolean isCommandRegister(@NotNull String mainCommandName, @NotNull String subCommandName) {
        checkConfig();
        String path = "commands." + mainCommandName + "." + subCommandName + ".register";
        return config.getBoolean(path, true);
    }
}
