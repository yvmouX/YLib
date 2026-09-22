package cn.yvmou.ylib.gui.appearance;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 一个菜单的**外观**：逻辑名字、标题、布局表与物品表。配合 {@code Menu}，菜单的
 * 「标题 / 尺寸 / 摆哪些物品 / 每格长什么样」全部由它决定，代码里只剩「点了做什么」。
 * <p>
 * 它是接口而不是某个具体的配置类：库只认这几个方法，**怎么读出来的库不管**——
 * 纯代码的外观用 {@code MenuAppearances.create(...)} 造，
 * 「代码定义默认值 + 配置文件覆盖」的外观交给 {@code MenuAppearances.file(...)} 管理。
 * 因此这个接口不带任何加载器的痕迹，三个约定：
 * <ul>
 *   <li>{@link #path()} 是**逻辑菜单名**（如 {@code admin.main}），与文件名无关——语言键前缀靠它，
 *       这样文件改名不会把已经翻译好的文案弄丢；</li>
 *   <li>{@link #items()} 的键就是布局表里的槽位名，两张表共用一套名字，不会各写各的；</li>
 *   <li>{@link #fileName()} 只用于报错与报告，不是身份的一部分。</li>
 * </ul>
 * <p>
 * {@link #keyPrefix()} 一族默认实现把「语言键怎么拼」定成了一处（{@code gui.<逻辑名>.…}）。
 * 这是本库唯一一条**命名约定**：默认如此，但整体覆盖 {@link #keyPrefix()} 就能换成别的拼法，
 * 库里的取值逻辑不会因此改一行。
 */
public interface MenuAppearance {

    /** 背景板的名字：它不在布局表里（填的是没被占用的格子），因此不走「按布局名取物品」那条路。 */
    String FILLER = "filler";

    /** 逻辑菜单名（如 {@code admin.main}）：语言键前缀是 {@code gui.<它>.…}。 */
    String path();

    /** 标题模板；{@code {名字}} 占位符在这里生效，语言键 {@link #titleKey()} 存在时优先用语言文件。 */
    String title();

    /** 布局表，一行一串字符（见 {@code SlotLayout}）：同时决定了容器大小与每个名字占哪些格子。 */
    String[] layout();

    /** 物品表（键 = 布局里的槽位名）；配置里写成空值时兜成空表，调用方不必判空。 */
    Map<String, ? extends MenuItemAppearance> items();

    /**
     * **数据驱动的文案模板**：不挂在某一格上、由代码按当前数据填的那些（列表条目的 lore、详情页的行模板、
     * 「未填」这类状态词……）。键是 {@link #key(String)} 的后缀，值是多行模板（单行也写成一行）。
     * <p>
     * 有了它，{@code textOf(后缀, 占位符)} 与 {@code loreOf(后缀, 占位符)} 不必再由调用方把模板传一遍——
     * 模板从这里取，代码里因此**不再出现任何文案**，也就不会再出现「代码里的兜底文案」和「配置里的模板」
     * 两份互相漂移的文本。语言文件里同名键存在时仍然优先。
     * <p>
     * 默认空表：只有 {@code items} 的菜单不用实现它。
     */
    default Map<String, List<String>> texts() {
        return Collections.emptyMap();
    }

    /**
     * 这份外观的**出处**（如 {@code gui/admin-main.yml}），只用于报错与报告——
     * 报错时管理员手里只有文件，给他文件名而不是一个他不一定认识的逻辑菜单名。
     * <p>
     * 文件管理的外观（{@code MenuAppearances.file}）返回真正的文件名；纯代码的外观（{@code MenuAppearances.create}）
     * 没有文件，退回逻辑菜单名（没设就是「代码里的外观」）。
     */
    default String fileName() {
        return path() == null || path().isEmpty() ? "代码里的外观" : path();
    }

    /**
     * 语言键前缀，默认 {@code gui.<逻辑名>}。
     * <p>
     * 整个菜单的文案键都从它拼出来（{@link #titleKey()}、{@link #itemKey(String)}、{@link #key(String)}），
     * 所以换一套命名法只需要覆盖这一个方法。
     */
    default String keyPrefix() {
        return "gui." + path();
    }

    /** 标题的语言键。 */
    default String titleKey() {
        return keyPrefix() + ".title";
    }

    /** 某个槽位物品的外观语言键前缀（其下还有 {@code .name} 与 {@code .lore}）。 */
    default String itemKey(String name) {
        return keyPrefix() + ".items." + name;
    }

    /** 数据驱动文案的语言键：排序维度名、空列表提示这类不在物品表里的字符串用它。 */
    default String key(String suffix) {
        return keyPrefix() + "." + suffix;
    }
}
