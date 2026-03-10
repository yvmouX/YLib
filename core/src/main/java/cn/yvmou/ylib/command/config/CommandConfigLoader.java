package cn.yvmou.ylib.command.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;

/**
 * 命令配置加载器
 * 负责从 Bukkit 的 ConfigurationSection 中解析出 CommandConfig 对象
 */
public class CommandConfigLoader {

    /**
     * 从配置段中加载 CommandConfig
     * @param section 配置段 (如 commands.yml 中的 "example")
     * @return 解析后的 CommandConfig
     */
    public CommandConfig load(ConfigurationSection section) {
        if (section == null) return null;

        CommandConfig config = new CommandConfig(section.getName());

        // 1. 读取基本属性
        if (section.contains("permission")) {
            config.setPermission(section.getString("permission"));
        }
        
        if (section.contains("description")) {
            config.setDescription(section.getString("description"));
        }
        
        if (section.contains("enabled")) {
            config.setEnabled(section.getBoolean("enabled", true));
        }

        // 处理别名 (支持字符串列表或单字符串)
        if (section.contains("aliases")) {
            if (section.isList("aliases")) {
                config.setAliases(section.getStringList("aliases"));
            } else {
                String alias = section.getString("aliases");
                if (alias != null && !alias.isEmpty()) {
                    config.setAliases(Collections.singletonList(alias));
                }
            }
        }

        // 2. 递归处理子命令 (subcommands)
        ConfigurationSection subcommandsSection = section.getConfigurationSection("subcommands");
        if (subcommandsSection != null) {
            for (String subCommandName : subcommandsSection.getKeys(false)) {
                ConfigurationSection subSection = subcommandsSection.getConfigurationSection(subCommandName);
                if (subSection != null) {
                    CommandConfig subConfig = load(subSection);
                    // 修正子命令名称（因为 section 的 name 可能就是子命令的 literal）
                    subConfig.setName(subCommandName); 
                    config.addSubcommand(subCommandName, subConfig);
                }
            }
        }

        return config;
    }
}
