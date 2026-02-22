package cn.yvmou.ylib.api.command.args;

import cn.yvmou.ylib.impl.command.core.CommandContext;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 补全提供器接口
 */
@FunctionalInterface
public interface SuggestionProvider {
    /**
     * 提供补全建议
     * @param sender 命令发送者
     * @param context 当前上下文（可能不完整）
     * @param currentInput 当前输入的参数片段
     * @return 建议列表
     */
    @NotNull
    List<String> suggest(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull String currentInput);
}
