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
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

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

    public MenuItem(ItemStack icon, Consumer<ClickContext> action) {
        this.icon = (icon == null ? new ItemStack(Material.PAPER) : icon).clone();
        // 空动作而不是 null：调用方（含背景板）不必再判空，避免每次都写 context -> { }
        this.action = action == null ? new Consumer<ClickContext>() {
            @Override
            public void accept(ClickContext context) {
            }
        } : action;
    }

    /** 图标（已克隆，改它不影响菜单里那一份）。 */
    public ItemStack icon() {
        return icon.clone();
    }

    /** 点击动作；没设过动作时是空动作。 */
    public Consumer<ClickContext> action() {
        return action;
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
        return new MenuItem(copy, action);
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
            return new MenuItem(copy, action);
        }
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (enchantment != null) {
            meta.addEnchant(enchantment, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        copy.setItemMeta(meta);
        return new MenuItem(copy, action);
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
                if (line != null && line.trim().isEmpty()) {
                    renderedLore.add(TextRenderer.render(line));
                }
            }
            meta.setLore(renderedLore);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
