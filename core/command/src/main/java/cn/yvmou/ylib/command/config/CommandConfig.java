package cn.yvmou.ylib.command.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令配置数据结构，用于从 YAML 加载配置并覆盖默认设置
 */
public class CommandConfig {
    private String name;
    private List<String> aliases;
    private String permission;
    private String description;
    private boolean enabled = true;
    private Map<String, CommandConfig> subcommands = new HashMap<>();

    public CommandConfig() {
    }

    public CommandConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, CommandConfig> getSubcommands() {
        return subcommands;
    }

    public void setSubcommands(Map<String, CommandConfig> subcommands) {
        this.subcommands = subcommands;
    }
    
    public void addSubcommand(String name, CommandConfig config) {
        this.subcommands.put(name, config);
    }
}
