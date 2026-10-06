package cn.yvmou.ylib.menu;

import org.bukkit.inventory.ItemStack;

/**
 * 「往这一格拉进一件物品」的回调：玩家手上带着物品点这一格，或把物品拖到这一格上时触发。
 *
 * <h2>为什么不并进 {@link ClickHandler}</h2>
 * 两者问的不是同一件事：{@link ClickHandler} 问「怎么点的」，这里问「放了什么」。而**同一只手可以按出两种意思**——
 * 空手左键点 = 点按钮，手上带着物品左键点 = 放入。合成一个接口，每个回调都得自己判一次光标，
 * 还很容易忘记判而让「玩家顺手拿着方块点了一下按钮，按钮就被改掉了」。
 * 分开之后，这一格的语义在声明处就写清楚了。
 *
 * <h2>语义</h2>
 * <ul>
 *   <li><b>优先于点击回调</b>：手上有物品时走这里，{@code onClick} 不再触发（手上的物品是更明确的意图）；</li>
 *   <li><b>事件一律取消</b>：物品不会被真的放进槽里，也不受 {@link Interaction} 策略影响——
 *       这一格是「接收」而不是「放下」；</li>
 *   <li><b>框架不消耗它</b>：回调拿到的是一份副本，扣不扣由业务决定（「拖一件钻石进来当图标」不该把整叠收走）。
 *       因为事件被取消，那件物品还留在玩家光标上，他自己放回背包即可；</li>
 *   <li><b>只认这两种手势</b>：光标带物品点这一格、拖拽只落在这一格。<b>Shift+左键</b>把自己的物品快速移入、
 *       以及数字键换手都不算——那两种手势的目标格由原版决定，不一定是这一格。</li>
 * </ul>
 *
 * <p>拖拽进来时 {@link MenuContext#clickType()} 与 {@link MenuContext#event()} 是 {@code null}
 * （拖拽不是点击，没有 {@code InventoryClickEvent}）；点击进来时两者都有值。
 *
 * @see SymbolBinder#onItemDrop(ItemDropHandler)
 */
@FunctionalInterface
public interface ItemDropHandler {

    /**
     * @param slot    被放入的那一格
     * @param ctx     上下文（{@code ctx.menu().refresh()} 刷新界面，{@code ctx.session()} 读草稿）
     * @param dropped 玩家放进来的物品的副本
     */
    void handle(Slot slot, MenuContext ctx, ItemStack dropped);
}
