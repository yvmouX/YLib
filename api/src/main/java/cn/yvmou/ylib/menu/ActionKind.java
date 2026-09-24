package cn.yvmou.ylib.menu;

import java.util.Locale;

/**
 * yml 里 {@code actions} 支持的动作类型。
 *
 * <pre>{@code
 * items:
 *   create_btn:
 *     material: WRITABLE_BOOK
 *     actions:
 *       - type: open_menu
 *         menu: CreateQuestMenu
 *       - type: message
 *         text: '&7共 {count} 个任务'
 * }</pre>
 *
 * <p>
 * 若 yml 里写了 {@code actions}，就由它**完全接管**该符号的点击行为：代码里的
 * {@code .onClick(...)} 被忽略，并在启动日志里提示一句——否则同一格上跑着两套逻辑，
 * 表现是「点了做了两件事」或者「改了配置却没反应」，两种都很难查。
 */
public enum ActionKind {

    /** 给玩家发一条文本。 */
    MESSAGE("text"),
    /** 以玩家身份执行命令。 */
    PLAYER_COMMAND("command"),
    /** 以控制台身份执行命令（提权）。 */
    CONSOLE_COMMAND("command"),
    /** 打开另一个菜单。 */
    OPEN_MENU("menu"),
    /** 关掉当前界面；不需要参数。 */
    CLOSE(null),
    /** 原地重渲染；不需要参数。 */
    REFRESH(null),
    /** 下一页；不需要参数。 */
    NEXT_PAGE(null),
    /** 上一页；不需要参数。 */
    PREV_PAGE(null);

    private final String parameter;

    ActionKind(String parameter) {
        this.parameter = parameter;
    }

    /** 这个动作需要的参数键；{@code null} 表示不需要参数。 */
    public String parameter() {
        return parameter;
    }

    /**
     * yml 里的 {@code type} → 动作类型；认不出来返回 {@code null}。
     * <p>
     * 大小写不敏感：yml 里写 {@code Open_Menu} 是很自然的写法，为此报错没有意义。
     */
    public static ActionKind parse(String type) {
        if (type == null) {
            return null;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (ActionKind kind : values()) {
            if (kind.name().equals(normalized)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * 给「认不出来的 type」写一条有建设性的报错：列出全部合法类型，
     * 比只说「未知动作」省掉一次翻文档。
     */
    public static String describeAll() {
        StringBuilder text = new StringBuilder();
        for (ActionKind kind : values()) {
            if (text.length() > 0) {
                text.append(" / ");
            }
            text.append(kind.name().toLowerCase(Locale.ROOT));
        }
        return text.toString();
    }
}
