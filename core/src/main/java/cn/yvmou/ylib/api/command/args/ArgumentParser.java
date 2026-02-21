package cn.yvmou.ylib.api.command.args;

import cn.yvmou.ylib.api.command.exception.CommandParseException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * 参数解析器接口
 * @param <T> 参数类型
 */
@FunctionalInterface
public interface ArgumentParser<T> {
    /**
     * 解析参数
     * @param sender 命令发送者
     * @param input 参数字符串
     * @return 解析后的对象
     * @throws CommandParseException 解析失败时抛出
     */
    @NotNull
    T parse(@NotNull CommandSender sender, @NotNull String input) throws CommandParseException;
}
