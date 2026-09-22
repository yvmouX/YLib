package cn.yvmou.ylib.gui.appearance;

import java.util.List;

/**
 * 一个菜单物品的**外观**：材质、名称、lore 模板。
 * <p>
 * 它只描述「长什么样」，点了干什么仍在代码里（{@code MenuItem} 的动作）——因此改外观不用改代码，
 * 加按钮仍要改代码。这条界线是刻意的：动作、权限、数据校验都不是配置能表达的东西。
 * <p>
 * 实现它的是**配置类**（如 {@code @AutoConfiguration} 标注的 YAML 映射类），因此这里只给读访问器，
 * 不给构造器、也不规定字段怎么存：{@link MenuAppearances#validate} 只认这三个方法。
 */
public interface MenuItemAppearance {

    /** 材质名；认不出来时 {@code MenuItem.material} 会退回纸（界面上看得出来，且启动校验会先拦下）。 */
    String material();

    /** 物品名；{@code {名字}} 占位符在这里生效。 */
    String name();

    /** lore 的每一行一条；{@code {名字}} 占位符在这里生效，取不到时兜成空表。 */
    List<String> lore();
}
