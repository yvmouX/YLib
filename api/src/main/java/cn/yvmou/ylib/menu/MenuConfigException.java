package cn.yvmou.ylib.menu;

/**
 * 菜单的**配置 / 布局错误**：布局解析不了、符号没声明、yml 版本比代码新、材质名认不出来……
 * <p>
 * 这类错误一律在**启动期**抛出并中止插件启用，而不是等到玩家点开菜单时才炸。这是整套菜单 API
 * 最核心的一条取舍：菜单的错误全都属于「写错了」，越早报越省事，而且报错时手里有行号 / 列号，
 * 能直接指到出问题的那一格；等玩家点开时才炸就只剩一句「界面打不开」。
 * <p>
 * 继承 {@link RuntimeException}：调用方（{@code onEnable}）拦不拦都能编译，
 * 但正常写法是**不拦**——让它把插件启用带崩，比带着坏菜单上线好。
 */
public class MenuConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MenuConfigException(String message) {
        super(message);
    }

    public MenuConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
