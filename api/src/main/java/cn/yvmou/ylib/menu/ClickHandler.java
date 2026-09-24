package cn.yvmou.ylib.menu;

/**
 * 点击回调：{@code (slot, ctx)}。
 * <p>
 * {@code slot} 是**具体**槽位——一个符号可以占 N 格，回调因此必然是按格触发的；多数回调忽略它，
 * 只有「同一个符号在不同格子上做不同事」时才需要（边框的顶边与左边各管各的）。
 */
@FunctionalInterface
public interface ClickHandler {

    void handle(Slot slot, MenuContext ctx);
}
