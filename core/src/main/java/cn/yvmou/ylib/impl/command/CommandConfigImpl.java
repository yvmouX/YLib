package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.logger.Logger;
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

public class CommandConfigImpl implements CommandConfig {
    private final Plugin plugin;
    private final Logger logger;

    private File configFile;
    private FileConfiguration config;
    public CommandConfigImpl(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }


    @Override
    public void loadCommandConfiguration() {
        ensureConfigFileExists();

        if (!configFile.exists()) {
            boolean v = false;
            try {
                v = configFile.createNewFile();
            } catch (IOException e) {
                logger.error("创建文件commands.yml时出错: " + v + e.getMessage());
            }

        }
        if (config != null) {
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile);

    }

    @Override
    public void initCommandConfigFromAnnotations(String mainCommandName, Map<String, SubCommand> subCommandMap) {
        boolean configChanged = false;

        for (SubCommand subCommand : subCommandMap.values()) {
            // 如果命令路径下没有配置 创建默认配置
            try {
                Method executeMethod = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);
                // 获取子命令的注解参数
                CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
                String basePath = "commands." + mainCommandName + "." + options.name();
                // 检查配置是否存在
                if (!config.contains(basePath + ".permission")) {
                    configChanged = true;
                    config.set(basePath + ".permission", options.permission().isEmpty() ? "none" : options.permission());
                    logger.debug("已为命令/{} {} permission 创建配置", mainCommandName, options.name());
                }
                if (!config.contains(basePath + ".description")) {
                    configChanged = true;
                    config.set(basePath + ".description", options.description().isEmpty() ? "none" : options.description());
                    logger.debug("已为命令/{} {} description 创建配置", mainCommandName, options.name());
                }
                if (!config.contains(basePath + ".usage")) {
                    configChanged = true;
                    config.set(basePath + ".usage", options.usage().isEmpty() ? "none" : options.usage());
                    logger.debug("已为命令/{} {} usage 创建配置", mainCommandName, options.name());
                }
                if (!config.contains(basePath + ".onlyPlayer")) {
                    configChanged = true;
                    config.set(basePath + ".onlyPlayer", options.onlyPlayer());
                    logger.debug("已为命令/{} {} onlyPlayer 创建配置", mainCommandName, options.name());
                }
                if (!config.contains(basePath + ".register")) {
                    configChanged = true;
                    config.set(basePath + ".register", options.register());
                    logger.debug("已为命令/{} {} register 创建配置", mainCommandName, options.name());
                }
                if (!config.contains(basePath + ".alias")) {
                    configChanged = true;
                    config.set(basePath + ".alias", options.alias().length < 1 ? new ArrayList<>() : Arrays.asList(options.alias()));
                    logger.debug("已为命令/{} {} alias 创建配置", mainCommandName, options.name());
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


    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */

    private void checkConfig() {
        loadCommandConfiguration();
    }

    private void ensureConfigFileExists() {
        if (configFile != null) return;

        configFile = new File(plugin.getDataFolder(), "commands.yml");
    }
}
