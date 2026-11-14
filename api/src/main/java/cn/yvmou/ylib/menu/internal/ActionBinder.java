package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.ActionContext;
import cn.yvmou.ylib.menu.ActionKind;
import cn.yvmou.ylib.menu.ClickHandler;
import cn.yvmou.ylib.menu.MenuActions;
import cn.yvmou.ylib.menu.MenuContext;
import cn.yvmou.ylib.menu.Slot;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 声明式动作的执行器：把 yml 里的 {@code actions} 列表变成一个 {@link ClickHandler}。
 *
 * <h2>参数替换</h2>
 * 动作参数里可以写 {@code {player}}（玩家名）与 {@code {page}} / {@code {pages}}（页码）。
 * 替换在**执行时**做而不是解析时——页码会随翻页变化，解析期替换等于把它冻在启动那一刻。
 *
 * <h2>安全边界</h2>
 * 命令里的 {@code {player}} 直接替换成玩家名，<b>不做转义</b>：玩家名由服务端保证唯一性与合法性
 * （只含字母数字下划线、长度受限），因此不存在注入空格再拼参数的问题。真正需要宿主负责的是
 * <b>权限判断</b>——{@link MenuActions#playerCommand} 的实现应当自己校验，
 * 而不是指望「yml 里的动作是安全的」。
 */
public final class ActionBinder implements ClickHandler {

    private final List<ActionSpec> actions;
    private final MenuActions host;

    ActionBinder(List<ActionSpec> actions, MenuActions host) {
        this.actions = Collections.unmodifiableList(new ArrayList<ActionSpec>(actions));
        this.host = host;
    }

    /** 把一组动作包成点击回调；动作表为空时返回 {@code null}（表示「这一格没有 yml 行为」）。 */
    public static ClickHandler bind(List<ActionSpec> actions, MenuActions host) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        return new ActionBinder(actions, host);
    }

    @Override
    public void handle(Slot slot, MenuContext ctx) {
        ActionContext context = new BinderContext(ctx);
        for (ActionSpec action : actions) {
            try {
                run(action, context);
            } catch (RuntimeException failure) {
                // 单条动作失败不该连累后面的动作，但必须留下痕迹
                MenuLog.warn("声明式动作执行失败: " + action.kind() + " -> " + failure);
            }
        }
    }

    private void run(ActionSpec action, ActionContext ctx) {
        Player viewer = ctx.viewer();
        switch (action.kind()) {
            case MESSAGE:
                host.message(viewer, substitute(action.argument(), ctx));
                return;
            case PLAYER_COMMAND:
                host.playerCommand(viewer, stripSlash(substitute(action.argument(), ctx)));
                return;
            case CONSOLE_COMMAND:
                host.consoleCommand(stripSlash(substitute(action.argument(), ctx)));
                return;
            case OPEN_MENU:
                host.openMenu(viewer, substitute(action.argument(), ctx));
                return;
            case CLOSE:
                ctx.close();
                return;
            case REFRESH:
                ctx.refresh();
                return;
            case NEXT_PAGE:
                ctx.page(ctx.page() + 1);
                ctx.refresh();
                return;
            case PREV_PAGE:
                if (ctx.page() > 0) {
                    ctx.page(ctx.page() - 1);
                    ctx.refresh();
                }
                return;
            default:
                // 枚举被扩展而这里忘了加分支：记一条日志而不是静默无反应
                MenuLog.warn("未实现的动作类型: " + action.kind());
        }
    }

    /** 执行期的参数替换：{@code {player}} / {@code {page}} / {@code {pages}}。 */
    private static String substitute(String text, ActionContext ctx) {
        if (text == null) {
            return "";
        }
        String result = text.replace("{player}", ctx.viewer().getName());
        result = result.replace("{page}", String.valueOf(ctx.page() + 1));
        return result;
    }

    /** yml 里写 {@code /home} 和 {@code home} 都认（前导斜杠剥掉，交给宿主决定怎么派发）。 */
    private static String stripSlash(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        int start = 0;
        while (start < trimmed.length() && trimmed.charAt(start) == '/') {
            start++;
        }
        return trimmed.substring(start);
    }

    /** {@link MenuContext} → {@link ActionContext}：只暴露声明式动作该看到的那几样。 */
    private static final class BinderContext implements ActionContext {

        private final MenuContext ctx;

        BinderContext(MenuContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Player viewer() {
            return ctx.viewer();
        }

        @Override
        public cn.yvmou.ylib.menu.MenuSession session() {
            return ctx.session();
        }

        @Override
        public int page() {
            return ctx.page();
        }

        @Override
        public void page(int page) {
            ctx.page(page);
        }

        @Override
        public org.bukkit.event.inventory.InventoryClickEvent event() {
            return ctx.event();
        }

        @Override
        public void close() {
            ctx.menu().close();
        }

        @Override
        public void refresh() {
            ctx.menu().refresh();
        }
    }
}
