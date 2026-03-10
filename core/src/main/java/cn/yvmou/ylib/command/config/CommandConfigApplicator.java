package cn.yvmou.ylib.command.config;

import cn.yvmou.ylib.command.tree.CommandNode;

import java.util.Iterator;
import java.util.List;

/**
 * 配置应用器
 * 负责将 CommandConfig 中的设置应用到 CommandNode 树上
 */
public class CommandConfigApplicator {

    /**
     * 将配置应用到命令节点
     * @param node 目标节点
     * @param config 配置数据
     */
    public void apply(CommandNode node, CommandConfig config) {
        if (config == null) return;

        // 1. 应用基本属性覆盖
        if (config.getPermission() != null) {
            node.permission(config.getPermission());
        }
        
        if (config.getDescription() != null) {
            node.description(config.getDescription());
        }
        
        // 注意：别名只能应用在 Literal 节点上
        if (config.getAliases() != null && node.isLiteral()) {
            List<String> aliases = config.getAliases();
            node.aliases(aliases.toArray(new String[0]));
        }

        // 2. 处理子命令覆盖与裁剪
        if (config.getSubcommands() != null && !config.getSubcommands().isEmpty()) {
            Iterator<CommandNode> it = node.getChildren().iterator();
            while (it.hasNext()) {
                CommandNode child = it.next();
                
                // 我们主要通过 Literal 名称来匹配配置中的子命令
                if (child.isLiteral()) {
                    String childName = child.getLiteral();
                    CommandConfig subConfig = config.getSubcommands().get(childName);
                    
                    if (subConfig != null) {
                        // 更新启用状态，而不是移除节点
                        child.enabled(subConfig.isEnabled());
                        
                        // 递归应用配置到子节点
                        apply(child, subConfig);
                    }
                }
            }
        }
    }
}
