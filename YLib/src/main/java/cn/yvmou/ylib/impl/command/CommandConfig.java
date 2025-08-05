package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 将命令保存值文件中，便于用户修改和读取
 *
 */
public class CommandConfig {
    private final Plugin plugin;
    private String name;
    private String permission;
    private boolean onlyPlayer;
    private String[] alias;
    private boolean register;
    private String usage;

    FileConfiguration config;

    public CommandConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration getConfig() {
        File configFile = new File(plugin.getDataFolder(), "commands.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        return config;
    }

    public void initCommandConfig(String commandName, SubCommand... commandHolder) {
        File configFile = new File(plugin.getDataFolder(), "commands.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        boolean configChanged = false;

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                configChanged = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            for (SubCommand subCommand : commandHolder) {
                Method method = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);

                // 获取数据
                if (method.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions commandOptions = method.getAnnotation(CommandOptions.class);
                    name = commandOptions.name();
                    permission = commandOptions.permission();
                    alias = commandOptions.alias();
                    register = commandOptions.register();
                    usage = commandOptions.usage();
                }
                String path = "commands." + commandName + "." + name;

                // 写入数据
                if (!config.contains(path)) {
                    config.set(path + ".permission", permission);
                    config.set(path + ".alias", alias);
                    config.set(path + ".register", register);
                    config.set(path + ".usage", usage);
                    configChanged = true;
                }
            }
        } catch (NoSuchMethodException ignored) {}

        if (configChanged) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getSubCommandList(String mainCommand) {
        ConfigurationSection configSection = config.getConfigurationSection("commands." + mainCommand);
        if (configSection == null) { return null; }

        return new ArrayList<>(configSection.getKeys(false));
    }

    /**
     * 获取
     * @return permission
     */
    public String getPermission(String mainCommand, String subCommand) {
        return getConfig().getString(getBasePath(mainCommand, subCommand) + "permission");
    }

    /**
     * 获取
     * @return alias
     */
    public String[] getAlias(String mainCommand, String subCommand) {
        return getConfig().getString(getBasePath(mainCommand, subCommand) + "alias").split(",");
    }

    /**
     * 获取
     * @return register
     */
    public boolean isRegister(String mainCommand, String subCommand) {
        return getConfig().getBoolean(getBasePath(mainCommand, subCommand) + "register");
    }

    /**
     * 获取
     * @return usage
     */
    public String getUsage(String mainCommand, String subCommand) {
        return getConfig().getString(getBasePath(mainCommand, subCommand) + "usage");
    }


    public String toString() {
        return "CommandConfig{plugin = " + plugin + ", name = " + name + ", permission = " + permission + ", onlyPlayer = " + onlyPlayer + ", alias = " + alias + ", register = " + register + ", usage = " + usage + ", config = " + config + "}";
    }

    private String getBasePath(String mainCommand, String subCommand) {
        return "commands." + mainCommand + "." + subCommand + ".";
    }
}
