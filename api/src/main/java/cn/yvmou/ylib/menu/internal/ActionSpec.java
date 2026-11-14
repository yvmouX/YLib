package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.ActionKind;

/**
 * 一条声明式动作：yml 里 {@code actions} 列表的一项。
 * <p>
 * 参数只有一个（{@code text} / {@code command} / {@code menu}），这是刻意的：
 * 动作 DSL 越丰富，要维护的安全边界与文档越多，而「发消息 / 执行命令 / 开菜单 / 翻页 / 关闭 / 刷新」
 * 这几样已经覆盖了服主改配置时的绝大多数诉求。真要做复杂逻辑，那本来就该写代码。
 */
public final class ActionSpec {

    private final ActionKind kind;
    private final String argument;
    private final int line;

    public ActionSpec(ActionKind kind, String argument, int line) {
        this.kind = kind;
        this.argument = argument;
        this.line = line;
    }

    public ActionKind kind() {
        return kind;
    }

    /** 参数值；{@link ActionKind#CLOSE} 这类无参动作返回 {@code null}。 */
    public String argument() {
        return argument;
    }

    /** 它在 yml 里的行号，用于报错与日志（0 表示不知道）。 */
    public int line() {
        return line;
    }
}
