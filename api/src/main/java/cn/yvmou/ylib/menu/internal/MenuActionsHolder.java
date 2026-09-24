package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.MenuActions;
import org.bukkit.entity.Player;

/**
 * yml 声明式动作的宿主实现放在这里，供解析层（{@code MenuConfigLoader}）与执行层
 * （{@code ActionBinder}）取用。
 * <p>
 * 单独一个持有者而不是直接放在 {@code Menus} 里：{@code Menus} 是给人看的门面，
 * 不该被内部解析代码反向依赖。这里只存一个 {@code volatile} 引用，等价于一个「全局配置项」。
 * <p>
 * 没登记实现时返回一个**会出声**的兜底实现：yml 里配了动作却什么都没发生是最难查的一类问题，
 * 至少让日志说清楚「缺的是哪一步」。
 */
public final class MenuActionsHolder {

    private static volatile MenuActions actions;

    private MenuActionsHolder() {
    }

    /** 登记宿主实现；在 {@code onEnable} 里调一次。传 {@code null} 表示恢复未登记状态。 */
    public static void set(MenuActions implementation) {
        actions = implementation;
    }

    /** 当前实现；没登记时是「只记日志」的兜底。 */
    public static MenuActions get() {
        MenuActions current = actions;
        return current != null ? current : MISSING;
    }

    private static final MenuActions MISSING = new MenuActions() {

        private void warn(String action) {
            MenuLog.warn("yml 里配了 " + action + " 动作，但没有登记 MenuActions 实现"
                    + "（在 onEnable 里调用 Menus.actions(...)）");
        }

        @Override
        public void message(Player viewer, String text) {
            warn("message");
        }

        @Override
        public void playerCommand(Player viewer, String command) {
            warn("player_command");
        }

        @Override
        public void consoleCommand(String command) {
            warn("console_command");
        }

        @Override
        public void openMenu(Player viewer, String menu) {
            warn("open_menu");
        }
    };
}
