package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.utils.LegacyCodes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 把「文本 + 成品物品」写进 {@link ItemStack} 的唯一一处实现。
 *
 * <h2>为什么要靠反射探测「有没有 Component 版 API」</h2>
 * 本库的编译基线是 {@code spigot-api 1.19.4}，但目标运行范围更宽。问题在于物品名 / lore 的设置方法
 * 在两端**恰好相反**：
 * <ul>
 *   <li>Bukkit 自 1.16 起给 {@code ItemMeta} 加了 Adventure 的 {@code displayName(Component)} /
 *       {@code lore(List&lt;Component&gt;)}，同时把 {@code setDisplayName(String)} 标为废弃；</li>
 *   <li>而 Spigot 后来**把它们删了**：1.21.8 的 {@code ItemMeta} 上只有 {@code setDisplayName(String)}，
 *       没有 {@code displayName}。</li>
 * </ul>
 * 也就是说没有任何一个方法能同时编译通过又覆盖两端。因此这里在**类加载时探测一次**，
 * 有 Component 版就用它（1.16+ Paper / 较新的 Spigot），否则退回 String 版（1.13 起都有）。
 * <p>
 * 探测结果只算一次，因此渲染热路径上没有任何反射开销。
 */
public final class MenuItems {

    /** Component 版的 {@code displayName(Component)}；不存在时为 {@code null}。 */
    private static final Method SET_COMPONENT_NAME = find("displayName", Component.class);

    /** Component 版的 {@code lore(List)}；不存在时为 {@code null}。 */
    private static final Method SET_COMPONENT_LORE = find("lore", List.class);

    /** 传统版的 {@code setDisplayName(String)}；理论上总有，留着兜底判断。 */
    private static final Method SET_STRING_NAME = find("setDisplayName", String.class);

    /** 传统版的 {@code setLore(List)}。 */
    private static final Method SET_STRING_LORE = find("setLore", List.class);

    private MenuItems() {
    }

    /**
     * 按名字与 lore 构造一个物品。
     *
     * @param material 图标；{@code null} 时退回纸（材质认不出来时不该让界面开不出来）
     * @param name     显示名，支持 {@code &} 色码；{@code null} 表示不设
     * @param lore     每行一条，支持 {@code &} 色码；{@code null} / 空表表示不设
     */
    public static ItemStack render(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (name != null) {
            applyName(meta, name);
        }
        if (lore != null && !lore.isEmpty()) {
            applyLore(meta, split(lore));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** 给一个成品物品套上光效（附魔 + 隐藏附魔行），返回**克隆**，不改原物品。 */
    public static ItemStack glow(ItemStack source) {
        ItemStack copy = source == null ? new ItemStack(Material.PAPER) : source.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        // 附魔常量一律走 getByKey：DURABILITY / DAMAGE_ALL / DIG_SPEED 这些旧字段名在 1.20.5 被删掉了，
        // 硬编码任何一个都会把插件钉死在某段版本上
        Enchantment glint = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (glint == null) {
            return copy;
        }
        meta.addEnchant(glint, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        copy.setItemMeta(meta);
        return copy;
    }

    /** 光效的「有没有」判断：只需读附魔表，因此不必克隆物品。 */
    public static boolean isGlowing(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Enchantment glint = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        return glint != null && stack.getEnchantmentLevel(glint) > 0;
    }

    // ---------- 文本写入（两端各一条路） ----------

    private static void applyName(ItemMeta meta, String name) {
        if (SET_COMPONENT_NAME != null) {
            invoke(meta, SET_COMPONENT_NAME, component(LegacyCodes.toSection(name)));
            return;
        }
        if (SET_STRING_NAME != null) {
            invoke(meta, SET_STRING_NAME, LegacyCodes.toSection(name));
        }
    }

    private static void applyLore(ItemMeta meta, List<String> lines) {
        List<String> rendered = new ArrayList<String>(lines.size());
        for (String line : lines) {
            rendered.add(LegacyCodes.toSection(line));
        }
        if (SET_COMPONENT_LORE != null) {
            List<Component> components = new ArrayList<Component>(rendered.size());
            for (String line : rendered) {
                components.add(component(line));
            }
            invoke(meta, SET_COMPONENT_LORE, components);
            return;
        }
        if (SET_STRING_LORE != null) {
            invoke(meta, SET_STRING_LORE, rendered);
        }
    }

    /** {@code §} 字符串 → Component。走 legacySection 序列化器，因为它认的正是 {@code §} 色码。 */
    private static Component component(String legacy) {
        if (legacy == null || legacy.isEmpty()) {
            return Component.empty();
        }
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(legacy);
    }

    /**
     * 拆行：一条字符串里的 {@code \n} 也算换行（写多行说明时用得上）；{@code null} 行被丢掉，
     * 空串保留（它是「空行间隔」，调用方常用来分段）。
     */
    private static List<String> split(List<String> lore) {
        List<String> lines = new ArrayList<String>();
        for (String each : lore) {
            if (each == null) {
                continue;
            }
            if (each.indexOf('\n') < 0) {
                lines.add(each);
                continue;
            }
            int start = 0;
            while (start <= each.length()) {
                int end = each.indexOf('\n', start);
                if (end < 0) {
                    lines.add(each.substring(start));
                    break;
                }
                lines.add(each.substring(start, end));
                start = end + 1;
            }
        }
        return lines;
    }

    private static Method find(String name, Class<?> parameter) {
        try {
            return ItemMeta.class.getMethod(name, parameter);
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }

    private static void invoke(ItemMeta meta, Method method, Object argument) {
        try {
            method.invoke(meta, argument);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // 探测时方法在，调用时却失败（服务端实现不完整 / 参数被拒，例如空 Component）：
            // 不设这个名字，也不打断整个界面——但必须留痕，否则「lore 不显示」无从排查
            MenuLog.warn("物品文本写入失败（" + method.getName() + "）：" + failure);
        }
    }
}
