package cn.yvmou.ylib;

import cn.yvmou.ylib.utils.LegacyCodes;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * 给**命令发送者**看的一条提示。
 *
 * <h2>为什么它不是日志</h2>
 * 判定标准一句话：<b>去掉时间戳 / 级别 / logger 名之后剩下的就是它的全部内容，那它是游戏消息。</b>
 * 这类消息该带 MC 颜色码、走聊天框，不该进日志文件。
 * <p>
 * 这个类是从 {@code Logger} 里拆出来的：原先 {@code Logger#to(sender)} / {@code toLog(sender)}
 * 让日志接口同时承担「写控制台」和「给玩家发消息」两件事，于是 {@code Logger} 的公开签名里
 * 既有 {@code ChatColor} 又有 {@code CommandSender}。整库 86 个日志调用点里只有 4 个是玩家定向的，
 * 却为此多挂了两个类（{@code Message} + {@code MessageImpl}）。
 *
 * <h2>与 {@code MessageService} 的分工</h2>
 * 这里只做「把一句话发给某个人」，<b>不做多语言</b>——需要按玩家语言取文案时用
 * {@code MessageService.send(sender, key, args)}（它内部会渲染语言文件）。
 * 两者不冲突：一个是输出通道，一个是取值来源。
 *
 * <h2>为什么单独一个类而不是塞回 logger 包</h2>
 * 它是**发送者侧**的概念，与日志无关；放在根包里与 {@link PluginInfo} 同级，
 * 免得再有人以为「给玩家发消息是日志的一部分」。
 */
public final class Notices {

    private Notices() {
    }

    /**
     * 普通提示。
     * <p>
     * 沿用原先 {@code MessageImpl} 的形状：<b>只发正文，不带前缀、不带级别标签</b>。
     * 玩家刚敲完一条命令，回执里再重复一遍插件名只是噪音。
     */
    public static void info(@NotNull CommandSender sender, @NotNull String text) {
        sender.sendMessage(LegacyCodes.toSection(text));
    }

    /**
     * 警告提示（前缀后跟一个黄色标记）。
     * <p>
     * 用符号而不是「WARN」字样：这是给玩家看的，不是日志级别。
     */
    public static void warn(@NotNull CommandSender sender, @NotNull String text) {
        sender.sendMessage("§e" + LegacyCodes.toSection(text));
    }

    /**
     * 错误提示（红色）。
     *
     * <h2>它修掉的一个副作用</h2>
     * 原先这类提示走的是 {@code Logger#toLog(sender).error(...)}，而那条路径会把参数里任何
     * {@code Throwable} 以 {@code SEVERE} 写进控制台、还附一句 {@code "YLib error"}。
     * 于是「告诉玩家没权限」这种再普通不过的提示，可能在控制台留下一行莫名其妙的错误日志。
     * <b>玩家提示不该有控制台副作用</b>——要记日志就由调用方显式调 {@code logger}，两件事分开。
     */
    public static void error(@NotNull CommandSender sender, @NotNull String text) {
        sender.sendMessage("§c" + LegacyCodes.toSection(text));
    }
}
