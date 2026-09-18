package cn.yvmou.ylib.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * 菜单事件监听器：把容器上的点击翻译成菜单项的 action，把拖入「接收物品」那一格的物品交给它。
 * <p>
 * 判归属只能看视图的上层容器（玩家点自己背包时 clickedInventory 是背包）；必须先无条件取消事件再执行动作，
 * 否则动作抛异常就会漏掉取消；派发期间用 {@link #dispatching} 防重入。
 * 宿主插件启用时调用一次 {@link #init(Plugin)} 即可，不必自己注册。
 */
public final class MenuListener implements Listener {

    /** 宿主插件：写日志用（动作抛异常时必须留下可排查的痕迹）。 */
    private final Plugin plugin;

    /** 是否正在派发动作，用于防重入。服务端事件在同一线程串行处理，普通布尔量足够。 */
    private boolean dispatching;

    public MenuListener(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** 注册点击分发与聊天输入拦截：宿主插件启用时调用一次即可（界面能打开但点击没反应，多半是忘了这一步）。 */
    public static void init(Plugin plugin) {
        // 菜单里「点一下 → 聊天栏填值」要用到它；宿主只调这一个方法就该两样都能用
        InputListener.init(plugin);
        Bukkit.getPluginManager().registerEvents(new MenuListener(plugin), plugin);
    }

    /** 点击派发：显式 {@code ignoreCancelled = false}——别的插件取消了也要取消事件本身（否则物品能被拖走），但不再执行动作。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Menu menu = menuOf(event.getView());
        if (menu == null) {
            return;
        }
        // 玩家自己背包里的普通点击交给原版：他得先能把物品拿起来，才谈得上「放到接收物品的格子上」。
        // 例外是 shift 点击——那会把物品直接塞进我们的容器，菜单里不该留东西，仍然拦下。
        Inventory clicked = event.getClickedInventory();
        if (clicked != null && clicked != event.getView().getTopInventory() && !event.isShiftClick()) {
            return;
        }
        // 先记下「是否已被别的插件取消」，再补上我们自己的取消：
        // 顺序反了就无从区分，会被自己的 setCancelled 掩盖掉。
        boolean cancelledByOther = event.isCancelled();
        event.setCancelled(true);

        // 别的插件已经否掉了这次点击 → 只保住菜单完整，不执行动作；正在派发中 → 防重入
        if (cancelledByOther || dispatching) {
            return;
        }

        int rawSlot = event.getRawSlot();
        // 落在玩家背包区的点击（rawSlot >= 菜单大小）只取消，不派发
        if (rawSlot < 0 || rawSlot >= menu.size()) {
            return;
        }
        MenuItem item = menu.itemAt(rawSlot);
        if (item == null) {
            return;
        }

        dispatching = true;
        try {
            // 这一格接收物品时优先算「投放」（顶掉这个格子的动作），两种手势都认：
            // ① 先把物品抓到光标上再点（getCursor）② 快捷栏选中那一格直接点（主手）——
            // 菜单把点击取消了，物品不会被抓到光标上，只认 getCursor 的话第二种手势永远没反应
            ItemStack dropped = event.getCursor();
            if (item.acceptsItems() && !notEmpty(dropped)) {
                dropped = menu.viewer().getInventory().getItemInMainHand();
            }
            if (item.acceptsItems() && notEmpty(dropped)) {
                item.onItem().accept(dropped.clone());
            } else {
                // 统一用菜单的 viewer 而不是 event.getWhoClicked()：菜单是为某一个玩家构建的视图，
                // 标题、进度、按钮语义都绑定在该玩家身上，动作里的 player 必须与之一致。
                item.action().accept(new MenuItem.ClickContext(menu.viewer(), event.getClick()));
            }
        } catch (RuntimeException e) {
            // 单个动作失败不能连累后续交互，但必须留下日志：否则玩家只会看到「点了没反应」
            plugin.getLogger().log(Level.SEVERE,
                    "菜单动作执行失败: " + menu.getClass().getSimpleName() + " slot=" + rawSlot, e);
        } finally {
            dispatching = false;
        }
    }

    /**
     * 拖拽：只在菜单区取消（菜单里没有一格允许放东西）；一次拖拽只要碰到「接收物品」的格子就算一次投放。
     * <p>
     * 整段都落在玩家背包里的拖拽交给原版——他得先把物品拖起来，才能拖到接收物品的格子上。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Menu menu = menuOf(event.getView());
        if (menu == null) {
            return;
        }
        boolean touchesMenu = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < menu.size()) {
                touchesMenu = true;
                break;
            }
        }
        if (!touchesMenu) {
            // 玩家在自己背包里拖动：不拦（否则连物品都拖不起来）
            return;
        }
        event.setCancelled(true);

        ItemStack dragged = event.getOldCursor();
        if (!notEmpty(dragged)) {
            return;
        }
        MenuItem target = itemTarget(menu, event.getRawSlots());
        if (target == null) {
            return;
        }
        dispatching = true;
        try {
            target.onItem().accept(dragged.clone());
        } catch (RuntimeException e) {
            // 与点击同一条原则：回调失败只记日志，不能把服务端的事件处理带崩
            plugin.getLogger().log(Level.SEVERE,
                    "菜单接收物品失败: " + menu.getClass().getSimpleName() + " slots=" + event.getRawSlots(), e);
        } finally {
            dispatching = false;
        }
    }

    /**
     * 这一批槽位里第一个「接收物品」的菜单格；一个都没有时返回 {@code null}。
     * <p>
     * 不要求每一格都接收：拖拽通常从玩家背包起手（背包格既不接收、也不在菜单区），
     * 要求「全都接收」等于这个手势永远用不了——反正事件一律取消、物品不会真的落地。
     */
    private static MenuItem itemTarget(Menu menu, Set<Integer> rawSlots) {
        for (int rawSlot : rawSlots) {
            if (rawSlot < 0 || rawSlot >= menu.size()) {
                continue;
            }
            MenuItem item = menu.itemAt(rawSlot);
            if (item != null && item.acceptsItems()) {
                return item;
            }
        }
        return null;
    }

    /** 光标上是否真的拿着东西：空手时可能是空气，也可能是 null。 */
    private static boolean notEmpty(ItemStack stack) {
        return stack != null && !stack.getType().isAir();
    }

    /**
     * 从视图解析出本库的菜单，不是我们的容器一律返回 {@code null}。
     * <p>
     * 归属判定只看 {@code InventoryHolder} 的类型，不看标题：标题会随语言文件变化、
     * 可能与其它插件的容器重名，属于展示层数据，不能当身份用。
     */
    private static Menu menuOf(InventoryView view) {
        if (view == null) {
            return null;
        }
        Inventory top = view.getTopInventory();
        if (top == null) {
            return null;
        }
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu.MenuHolder)) {
            return null;
        }
        return ((Menu.MenuHolder) holder).menu();
    }
}
