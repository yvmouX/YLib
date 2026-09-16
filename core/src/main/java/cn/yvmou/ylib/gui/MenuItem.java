package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MenuItem {

    /** 点击上下文：动作只需要知道「谁点的」与「怎么点的」。 */
    public static final class ClickContext {

        private final Player player;
        private final ClickType clickType;

        public ClickContext(Player player, ClickType clickType) {
            this.player = player;
            this.clickType = clickType;
        }

        public Player player() {
            return player;
        }

        public ClickType clickType() {
            return clickType;
        }

        /** 左键（含 Shift+左键）。 */
        public boolean isLeft() {
            return clickType != null && clickType.isLeftClick();
        }

        /** 右键（含 Shift+右键）。 */
        public boolean isRight() {
            return clickType != null && clickType.isRightClick();
        }

        /** 任一 Shift 组合。 */
        public boolean isShift() {
            return clickType != null && clickType.isShiftClick();
        }

        /** Shift + 右键（常用于「删除」这类危险动作）。 */
        public boolean isShiftRight() {
            return isShift() && isRight();
        }
    }

    private final ItemStack icon;
    private final Consumer<ClickContext> action;
    /** 这一格是否接收玩家手上的物品（只有 {@link #acceptItem} 造出来的才是）。 */
    private final boolean acceptsItems;
    /** 收到物品时的回调；不接收物品时为 {@code null}。 */
    private final Consumer<ItemStack> onItem;

    public MenuItem(ItemStack icon, Consumer<ClickContext> action) {
        this(icon, action, false, null);
    }

    private MenuItem(ItemStack icon, Consumer<ClickContext> action, boolean acceptsItems, Consumer<ItemStack> onItem) {
        this.icon = (icon == null ? new ItemStack(Material.PAPER) : icon).clone();
        // 空动作而不是 null：调用方（含背景板）不必再判空，避免每次都写 context -> { }
        this.action = action == null ? emptyAction() : action;
        this.acceptsItems = acceptsItems;
        this.onItem = onItem;
    }

    private static Consumer<ClickContext> emptyAction() {
        return new Consumer<ClickContext>() {
            @Override
            public void accept(ClickContext context) {
            }
        };
    }

    /** 图标（已克隆，改它不影响菜单里那一份）。 */
    public ItemStack icon() {
        return icon.clone();
    }

    /** 点击动作；没设过动作时是空动作。 */
    public Consumer<ClickContext> action() {
        return action;
    }

    /** 是否接收玩家拖入 / 拿在手上的物品；只给同包的 {@link MenuListener} 用。 */
    boolean acceptsItems() {
        return acceptsItems;
    }

    /** 收到物品时的回调；{@link #acceptsItems()} 为 false 时是 {@code null}。 */
    Consumer<ItemStack> onItem() {
        return onItem;
    }

    // ---------- 静态工厂 ----------

    /** 完整创建 */
    public static MenuItem of(Material material, String name, Consumer<List<String>> lore, Consumer<ClickContext> action) {
        return new MenuItem(renderIcon(material, name, lore), action);
    }

    /** 只展示、不响应点击的物品（头部信息、禁用态的分页按钮）。 */
    public static MenuItem display(Material material, String name, Consumer<List<String>> lore) {
        return of(material, name, lore, null);
    }

    /**
     * 点一下就在聊天栏问值：左键右键都算，提交后把文本交给 {@code onValue}。
     * <p>
     * {@code onValue} 里要自己 {@link Menu#refresh()} 或重开界面：库里不知道该刷新哪个菜单，
     * 而玩家进聊天栏时界面已经关了。{@code current} 为 {@code null} 表示没有当前值。
     */
    public static MenuItem input(final Material icon, final String name, Consumer<List<String>> lore,
                                 final String hint, final Supplier<String> current,
                                 final Consumer<String> onValue) {
        return of(icon, name, lore, new Consumer<ClickContext>() {
            @Override
            public void accept(ClickContext context) {
                String now = current == null ? null : current.get();
                PlayerInput.ask(context.player(), name, hint, now, onValue, null);
            }
        });
    }

    /**
     * 接收玩家拖入（或拿在手上点击）的物品：图标与名字照旧，onItem 收到那份 ItemStack 的克隆。
     * 物品不会被消耗、也不会真的放进菜单：事件仍然取消，拖拽会被中止、物品回到玩家手里。
     * <p>
     * 空手点击照常走 {@code action}（例如右键恢复默认图标）；手上拿着物品时优先算投放，不触发它。
     */
    public static MenuItem acceptItem(Material icon, String name, Consumer<List<String>> lore,
                                      Consumer<ItemStack> onItem, Consumer<ClickContext> action) {
        return new MenuItem(renderIcon(icon, name, lore), action, true, onItem);
    }

    /**
     * 背景填充物：灰色玻璃板 + 一个空格名字。
     */
    public static MenuItem filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** 自定义材质的背景填充物 */
    public static MenuItem filler(Material material) {
        return of(material, " ", lore -> {}, null);
    }

    /**
     * 材质名 → 材质，解析失败回退 {@link Material#PAPER}。
     * <p>
     * 配置里把材质名写错（或写了新版本才有的方块）不该让整个界面开不出来，
     * 显示成纸反而能让管理员一眼看出「这条配置的图标有问题」。
     */
    public static Material material(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Material.PAPER;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        return material == null ? Material.PAPER : material;
    }

    // ---------- 实例方法 ----------

    /**
     * 复制一份并改成指定数量，用于让图标本身承载信息（例如目标进度 3/64 显示成 3 个物品）。
     * <p>
     * 数量会被夹到 {@code [1, getMaxStackSize()]}：超出堆叠上限的数量在客户端上
     * 会显示成乱码般的数字，也不符合原版渲染规则。
     */
    public MenuItem withAmount(int amount) {
        ItemStack copy = icon.clone();
        copy.setAmount(Math.max(1, Math.min(amount, Math.max(1, copy.getMaxStackSize()))));
        return new MenuItem(copy, action, acceptsItems, onItem);
    }

    /**
     * 复制一份并加上附魔光效（附魔 + 隐藏附魔标记）：物品要表达「已选中 / 可点击」时最直观的写法。
     * <p>
     * 附魔只是个载体，名字与 lore 都不会显示它。取附魔走 {@code getByKey} 而不是
     * {@code Enchantment.UNBREAKING} 这类字段：那些字段名在 1.20.5 前后改过，
     * 编译期写死会在另一个版本上变成 {@code NoSuchFieldError}。
     */
    public MenuItem glow() {
        ItemStack copy = icon.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return new MenuItem(copy, action, acceptsItems, onItem);
        }
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (enchantment != null) {
            meta.addEnchant(enchantment, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        copy.setItemMeta(meta);
        return new MenuItem(copy, action, acceptsItems, onItem);
    }

    // ---------- 内部实现 ----------
    // 接受回调并渲染图标
    private static ItemStack renderIcon(Material material, String name, Consumer<List<String>> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (name != null) {
            meta.setDisplayName(TextRenderer.render(name));
        }
        List<String> loreLines = new ArrayList<>();
        if (lore != null) {
            lore.accept(loreLines);
        }
        if (!loreLines.isEmpty()) {
            List<String> renderedLore = new ArrayList<>();
            for (String line : loreLines) {
                // 空串是「空行间隔」，要留着（调用方常 lore.add("") 分段），只滤掉 null
                if (line != null) {
                    renderedLore.add(TextRenderer.render(line));
                }
            }
            meta.setLore(renderedLore);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
