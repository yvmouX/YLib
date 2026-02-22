package cn.yvmou.ylib.command;

import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.command.tree.CommandNode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandConfig {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;
    private YamlConfiguration config;
    
    private static final String FILE_NAME = "commands.yml";

    public CommandConfig(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public void load() {
        if (!file.exists()) {
            // Check if resource exists before saving
            if (plugin.getResource(FILE_NAME) != null) {
                plugin.saveResource(FILE_NAME, false);
            } else {
                // If not found in JAR, create empty file
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    logger.error("Failed to create commands.yml", e);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void initCommand(@NotNull String commandName, String defaultDescription, String defaultPermission, List<String> defaultAliases, List<CommandNode> children) {
        String path = "commands." + commandName;

        // 如果配置文件中不存在该命令的配置，则使用默认值创建
        if (!config.contains(path + ".enabled")) {
            config.set(path + ".enabled", true);
        }

        if (!config.contains(path + ".aliases")) {
            config.set(path + ".aliases", defaultAliases != null ? defaultAliases : new ArrayList<>());
        }

        if (!config.contains(path + ".description")) {
            config.set(path + ".description", defaultDescription != null ? defaultDescription : "");
        }

        if (!config.contains(path + ".permission")) {
            config.set(path + ".permission", defaultPermission != null ? defaultPermission : "");
        }

        if (!config.contains(path + ".children")) {
            List<Map<String, Object>> childrenConfig = new ArrayList<>();

            if (children != null && !children.isEmpty()) {
                for (CommandNode childNode : children) {
                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("name", childNode.getLiteral());// 对应 YAML 中的 name: "xxx"
                    childMap.put("permission", childNode.getPermission()); // 对应 YAML 中的 permission: "xxx"
                    childrenConfig.add(childMap);
                }
            }
            config.set(path + ".children", childrenConfig);
        }

        // 保存配置文件
        try {
            config.save(file);
        } catch (IOException e) {
            logger.error("Failed to save commands.yml", e);
        }
    }

    public boolean isEnabled(@NotNull String commandName) {
        return config.getBoolean("commands." + commandName + ".enabled", true);
    }

    public @NotNull List<String> getAliases(@NotNull String commandName) {
        return config.getStringList("commands." + commandName + ".aliases");
    }

    public String getDescription(@NotNull String commandName) {
        return config.getString("commands." + commandName + ".description", "");
    }

    public String getPermission(@NotNull String commandName) {
        return config.getString("commands." + commandName + ".permission", "");
    }

    public List<CommandNode> getChildren(@NotNull String commandName) {
        List<Map<?, ?>> childrenList = config.getMapList("commands." + commandName + ".children");

        List<CommandNode> nodeList = new ArrayList<>();

        for (Map<?, ?> childMap : childrenList) {
            String name = (String) childMap.get("name");
            String permission = (String) childMap.get("permission");

            CommandNode node = CommandNode.literal(name)
                    .permission(permission);

            nodeList.add(node);
        }
        return nodeList;
    }
}
