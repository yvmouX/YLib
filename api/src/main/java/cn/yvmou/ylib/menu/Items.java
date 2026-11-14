package cn.yvmou.ylib.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 物品构建器：**「四级阶梯」的唯一入口**。
 *
 * <table>
 *   <caption>四级阶梯（模板里写死材质、yml 里读材质、接管成品物品、材质随数据变）</caption>
 *   <tr><th>入口</th><th>什么时候用</th></tr>
 *   <tr><td>{@link #of(Material)}</td><td>模板里写死材质（编译期常量）</td></tr>
 *   <tr><td>{@link #of(String)}</td><td>材质名来自 yml —— 启动期解析，认不出来就报错</td></tr>
 *   <tr><td>{@link #of(ItemStack)}</td><td>直接接管一个成品物品（带 NBT / 组件的外部物品）</td></tr>
 *   <tr><td>{@link #of(java.util.function.Function)}</td><td>材质**打开时才算**：图标跟着数据走（「当前选中的那件东西」）</td></tr>
 * </table>
 *
 * <b>为什么必须有一个 {@code ItemStack} 直传重载</b>：否则框架会被 {@code Material} 枚举绑死，
 * 也就无法处理带自定义数据的外部物品。
 * <p>
 * <b>为什么名字与 lore 收 {@code &} 字符串而不是 {@code Component}</b>：模板、yml、占位符里到处都是
 * {@code &7}，让使用者每次手动 {@code Component.text(...)} 是负担；转换点在本类内部只有一处，
 * 换成 Component 实现也只是这一处的事，对外不必把 Adventure 泄漏成 API 依赖。
 * <p>
 * {@code name} / {@code lore} 里可以写 {@code {名字}} 占位符，由 {@link SymbolBinder#ph} 提供取值。
 * <b>材质不走占位符</b>：它是物品的身份而不是文案，需要它随数据变就用 {@link #of(java.util.function.Function)}——
 * 材质在「每次渲染」时求值，与占位符同一时机。
 */
public final class Items {

    /** 材质；{@link #of(ItemStack)} 与 {@link #of(java.util.function.Function)} 造出来的是 {@code null}。 */
    private final Material material;

    /**
     * 动态材质：每次渲染按当前上下文算一个材质；其余路径造出来的是 {@code null}。
     * <p>
     * 与 {@link #material} 是**同一个概念的两个来源**，同时只有一个有值（构造处保证）。
     */
    private final java.util.function.Function<MenuContext, Material> materialSource;

    /** 直接接管的成品物品；其余路径造出来的是 {@code null}。 */
    private final ItemStack stack;

    /** yml 里写的材质名；只有 {@link #of(String)} 造的才有。 */
    private final String configName;

    private String name;
    private java.util.List<String> lore;
    private int amount = 1;
    private Integer customModelData;
    private boolean glow;
    private ItemFlag[] flags;
    private Consumer<ItemMeta> metaMutator;

    private Items(Material material, java.util.function.Function<MenuContext, Material> materialSource,
                  ItemStack stack, String configName) {
        this.material = material;
        this.materialSource = materialSource;
        this.stack = stack;
        this.configName = configName;
    }

    /** 模板里写死材质时用。 */
    public static Builder of(Material material) {
        if (material == null) {
            throw new MenuConfigException("物品材质是 null：界面里摆了这一格，就得给它一个材质");
        }
        return new Builder(new Items(material, null, null, null));
    }

    /**
     * yml 读来的材质走这里：**字符串 + 启动期解析 + 改名建议**。
     * <p>
     * {@code Material} 的成员会改名、会增删（{@code GRASS} → {@code SHORT_GRASS}），而这属于**数据变更**
     * 而不是 API 变更——服主升级服务端后，他 yml 里那个名字就废了。把解析放在启动期并给出建议，
     * 能把这类问题变成一条可读的报错，而不是运行期的一个异常或一张纸。
     */
    public static Builder of(String configName) {
        Material material = resolve(configName);
        if (material == null) {
            throw new MenuConfigException("未知材质 '" + configName + "'" + suggest(configName));
        }
        return new Builder(new Items(material, null, null, configName));
    }

    /** 直接接管一个成品物品（带 NBT / 组件的外部物品）；{@code null} 会被拒绝。 */
    public static Builder of(ItemStack stack) {
        if (stack == null) {
            throw new MenuConfigException("物品是 null：要留空请用 fillEmpty，而不是 of(null)");
        }
        return new Builder(new Items(null, null, stack.clone(), null));
    }

    /**
     * **材质每次渲染才算**：这一格的图标要跟着数据走时用它。
     *
     * <pre>{@code
     * // 「图标」那一格显示的就是玩家当前选的材质
     * b.symbol("icon").of(Items.of(ctx -> Items.resolveOr(draft(ctx).icon(), Material.PAPER)))
     *         .ph("value", ctx -> draft(ctx).icon())
     *         .onItemDrop((slot, ctx, dropped) -> setIcon(dropped.getType()));
     * }</pre>
     *
     * <h2>为什么不是「往材质里写占位符」</h2>
     * 名字与 lore 是文案，写 {@code {名字}} 天经地义；材质是物品的身份，而 {@link #of(String)} 的
     * 「启动期解析、认不出来就报错」正是它最值钱的地方——让材质也走占位符，就等于把它推迟到运行期，
     * 写错一个名字只会在玩家打开界面时变成一张纸。
     * <p>
     * <b>求值失败（返回 {@code null} 或抛异常）回落纸并记警告</b>，与占位符求值失败的处理一致：
     * 一个写坏的取值函数不该让整个界面开不出来。
     * <p>
     * <b>配置文件里的 {@code material} 对这类格子不生效</b>（那一格的外观是数据，不是外观层）；
     * 但 {@code name} / {@code lore} 照常可覆盖。
     */
    public static Builder of(java.util.function.Function<MenuContext, Material> materialSource) {
        if (materialSource == null) {
            throw new MenuConfigException("材质取值函数是 null：静态材质请用 of(Material)");
        }
        return new Builder(new Items(null, materialSource, null, null));
    }

    /** 背景板：灰色玻璃板 + 一个空格名字。 */
    public static Builder filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** 自定义材质的背景板。 */
    public static Builder filler(Material material) {
        return of(material).name(" ");
    }

    /**
     * 从一个**已有的物品定义**上继续改（配置层用它起步）。
     * <p>
     * 拿代码默认值当底、再按 yml 里写了什么逐项覆盖；「缺的字段回落默认值」因此不需要额外逻辑——
     * 没被覆盖的字段本来就还是默认值。
     */
    public static Builder basedOn(Items base) {
        return new Builder(base);
    }

    /**
     * 合并：以 {@code defaults} 为底，把 {@code from} 上**显式设过**的字段叠上去。
     * <p>
     * 这是「配置优先于代码」这条规则的唯一实现点，也是材质覆盖能生效的原因——覆盖材质不是另起一个底，
     * 而是「新材质 + 默认值的其余字段」。写成由一个静态方法统一合并，是为了避免「先换底再覆盖」
     * 那类顺序陷阱：那种写法会在「同时改了材质和别的字段」时把新材质静默丢掉。
     *
     * @param defaults 代码默认值；{@code null} 时等价于没有底
     * @param from     本次显式设过的字段
     */
    public static Items merge(Items defaults, Items from) {
        Items bottom = defaults;
        Items top = from;
        // 材质只有**一个来源**：静态与动态是同一个槽的两种给法，一并取用，免得出现
        // 「material() 说石头、materialFor() 说钻石」这种自相矛盾的一份定义
        boolean topGivesMaterial = top.material != null || top.materialSource != null;
        Items merged = new Items(
                topGivesMaterial ? top.material : (bottom == null ? null : bottom.material),
                topGivesMaterial ? top.materialSource : (bottom == null ? null : bottom.materialSource),
                top.stack != null ? top.stack : (bottom == null ? null : bottom.stack),
                top.configName != null ? top.configName : (bottom == null ? null : bottom.configName));
        merged.name = top.name != null ? top.name : (bottom == null ? null : bottom.name);
        merged.lore = top.lore != null ? top.lore : (bottom == null ? null : bottom.lore);
        merged.amount = top.amount != 1 ? top.amount : (bottom == null ? 1 : bottom.amount);
        merged.customModelData = top.customModelData != null
                ? top.customModelData : (bottom == null ? null : bottom.customModelData);
        merged.glow = top.glow || (bottom != null && bottom.glow);
        merged.flags = top.flags != null ? top.flags : (bottom == null ? null : bottom.flags);
        merged.metaMutator = top.metaMutator != null
                ? top.metaMutator : (bottom == null ? null : bottom.metaMutator);
        return merged;
    }

    // ---------- 材质解析（唯一实现） ----------

    /**
     * 材质名 → 材质；认不出来（含空值）返回 {@code null}。
     * <p>
     * 大小写不敏感：管理员手写 yml 时不会总记得全大写。它是唯一的解析实现——校验与渲染都走它，
     * 两边各写一套的话，「校验说认得出来、渲染却退回纸」这种错迟早出现。
     */
    public static Material resolve(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException malformed) {
            // matchMaterial 对畸形名字会抛；这不值得把插件启用打断，按「认不出来」处理
            return null;
        }
    }

    /** 解析材质名；认不出来退回 {@code fallback}——数据里的名字不该让界面开不出来。 */
    public static Material resolveOr(String name, Material fallback) {
        Material material = resolve(name);
        return material == null ? fallback : material;
    }

    /**
     * 「未知材质」的改名建议。
     * <p>
     * <b>刻意不做完整的材质别名映射表</b>：那需要跟着每个服务端版本维护一份历史别名库，成本远大于收益。
     * 这里只认几个跨版本改名里最常见、且**光看旧名字猜不出新名字**的，以及「只差一截」的近似名字。
     */
    public static String suggest(String wrong) {
        if (wrong == null || wrong.trim().isEmpty()) {
            return "";
        }
        String upper = wrong.trim().toUpperCase(Locale.ROOT);
        String renamed = renamed(upper);
        if (renamed != null) {
            return "（该材质在新版本被改名为 " + renamed + "）";
        }
        String guess = closest(upper);
        return guess == null ? "" : "（是否想写 " + guess + "?）";
    }

    /** 跨版本改名：只放「光看旧名字猜不出新名字」的那几个。 */
    private static String renamed(String upper) {
        if ("GRASS".equals(upper)) {
            return "SHORT_GRASS";
        }
        if ("SCUTE".equals(upper)) {
            return "TURTLE_SCUTE";
        }
        if ("GRASS_PATH".equals(upper)) {
            return "DIRT_PATH";
        }
        if ("SIGN".equals(upper)) {
            return "OAK_SIGN";
        }
        if ("WALL_SIGN".equals(upper)) {
            return "OAK_WALL_SIGN";
        }
        return null;
    }

    /** 在材质表里按「包含」找最短的近似名字。 */
    private static String closest(String upper) {
        String best = null;
        for (Material candidate : Material.values()) {
            String candidateName = candidate.name();
            if (!candidateName.contains(upper)) {
                continue;
            }
            // 不加这层排序的话，GRASS 会先撞上 GRASS_BLOCK，而 SHORT_GRASS 才是想说的那个
            if (best == null || candidateName.length() < best.length()) {
                best = candidateName;
            }
        }
        return best;
    }

    // ---------- 读取（配置层与渲染层用） ----------

    /** 静态材质；由 {@link #of(ItemStack)} 与 {@link #of(java.util.function.Function)} 造的返回 {@code null}。 */
    public Material material() {
        return material;
    }

    /** 材质是不是「每次渲染才算」（{@link #of(java.util.function.Function)}）。配置文件据此不覆盖材质。 */
    public boolean dynamicMaterial() {
        return materialSource != null;
    }

    /**
     * 这一次渲染该用哪个材质：静态材质的格子原样返回，动态材质的格子在这里求值。
     * <p>
     * <b>静态优先</b>：两种来源只会有一个有值（{@link #merge} 保证），万一都设了，静态的那个说了算——
     * 「配置里写了材质就钉住它」是可预期的，反过来则是「改了配置却没反应」。
     * <p>
     * 求值返回 {@code null} 或抛异常一律回落纸并记警告——与占位符求值失败的处理一致：
     * 一个写坏的取值函数不该让整个界面开不出来。
     */
    public Material materialFor(MenuContext ctx) {
        if (material != null) {
            return material;
        }
        if (materialSource == null) {
            // of(ItemStack)：没有材质可言（渲染走的是 rawStack 那条路）
            return null;
        }
        try {
            Material resolved = materialSource.apply(ctx);
            if (resolved != null) {
                return resolved;
            }
            cn.yvmou.ylib.menu.internal.MenuLog.warn("动态材质返回了 null，先用纸顶上");
        } catch (RuntimeException failure) {
            cn.yvmou.ylib.menu.internal.MenuLog.warn("动态材质求值失败：" + failure);
        }
        return Material.PAPER;
    }

    /** yml 里写的那串材质名；由 {@link #of(Material)} 造的返回 {@code null}。 */
    public String configName() {
        return configName;
    }

    /** 直接接管的成品物品；不是这条路径构造的返回 {@code null}。 */
    public ItemStack rawStack() {
        return stack;
    }

    public String name() {
        return name;
    }

    public java.util.List<String> lore() {
        return lore;
    }

    public int amount() {
        return amount;
    }

    public Integer customModelData() {
        return customModelData;
    }

    public boolean glowing() {
        return glow;
    }

    public ItemFlag[] itemFlags() {
        return flags;
    }

    public Consumer<ItemMeta> metaMutator() {
        return metaMutator;
    }

    /**
     * 渲染成一个全新的 {@link ItemStack}。
     * <p>
     * <b>每次调用都必须产出新实例</b>：{@code ItemStack} 与 {@code ItemMeta} 可变，把构建期的实例缓存后
     * 塞给多个玩家，会让 A 玩家看到的 lore 变成 B 玩家的数值、甚至附魔互相污染。这也是占位符必须在
     * 「打开时」而不是「构建期」求值的原因。
     *
     * @param displayName 已求值 / 已替换占位符的名字；{@code null} 表示不设
     * @param displayLore 已求值 / 已替换占位符的 lore
     */
    public ItemStack render(String displayName, java.util.List<String> displayLore) {
        return render(null, displayName, displayLore);
    }

    /**
     * 带上下文的渲染：动态材质（{@link #of(java.util.function.Function)}）在这里取到「这次该用哪个材质」。
     * <p>
     * 渲染期从容器里取物品的地方（{@code MenuView}）走这个重载；{@link Builder#stack()} 那种
     * 「就地要一个成品」的场景没有上下文，走上面那个（动态材质会求值成纸并记警告）。
     */
    public ItemStack render(MenuContext ctx, String displayName, java.util.List<String> displayLore) {
        ItemStack built;
        if (stack != null) {
            built = stack.clone();
        } else {
            built = cn.yvmou.ylib.menu.internal.MenuItems.render(materialFor(ctx), displayName, displayLore);
        }
        if (customModelData != null) {
            ItemMeta meta = built.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                built.setItemMeta(meta);
            }
        }
        if (amount != 1) {
            built.setAmount(Math.max(1, Math.min(amount, Math.max(1, built.getMaxStackSize()))));
        }
        if (flags != null && flags.length > 0) {
            ItemMeta meta = built.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(flags);
                built.setItemMeta(meta);
            }
        }
        if (metaMutator != null) {
            ItemMeta meta = built.getItemMeta();
            if (meta != null) {
                metaMutator.accept(meta);
                built.setItemMeta(meta);
            }
        }
        if (glow) {
            built = cn.yvmou.ylib.menu.internal.MenuItems.glow(built);
        }
        return built;
    }

    /**
     * 链式构建器：记录**显式设过**的字段，{@link #build()} 时再落到 {@link Items} 上。
     * <p>
     * 「显式设过」这件事必须记在 builder 自己的字段里（{@code null} = 没设过），不能塞进 {@link Items}
     * 用哨兵表示——yml 合并要能区分「我没设过 name」与「我把 name 设成了空串」。
     */
    public static final class Builder {

        /** 作为默认值的底；{@code null} 表示没有底（全部由本 builder 给出）。 */
        private final Items base;

        private String name;
        private java.util.List<String> lore;
        private int amount = 1;
        private Integer customModelData;
        private boolean glow;
        private ItemFlag[] flags;
        private Consumer<ItemMeta> metaMutator;

        private Builder(Items items) {
            this.base = items;
        }

        /** 显示名，支持 {@code &} 色码与 {@code {名字}} 占位符。 */
        public Builder name(String legacyText) {
            this.name = legacyText;
            return this;
        }

        /**
         * 覆盖材质（配置里改了 {@code material} 时用）。
         * <p>
         * 只能用在**按材质构造**的 builder 上：由 {@link #of(ItemStack)} 接管的成品物品没有材质可换，
         * 调用它当场抛——那比静默忽略好，因为「改了配置却没反应」是最难查的一类问题。
         * 传入 {@code null} 表示「不覆盖」，保持原材质。
         * <p>
         * 动态材质（{@link #of(java.util.function.Function)}）的格子不会走到这里：配置里的 {@code material}
         * 对它不生效（那一格的外观是数据），配置层会记一条警告说明这件事。
         */
        public Builder material(Material replacement) {
            if (replacement == null) {
                return this;
            }
            if (base == null || base.stack != null) {
                throw new MenuConfigException("这是 of(ItemStack) 接管的成品物品，没有材质可以覆盖");
            }
            return new Builder(new Items(replacement, null, null, replacement.name()));
        }

        /** lore，每个参数一行；元素内含 {@code \n} 也当换行，空串是空行。 */
        public Builder lore(String... legacyLines) {
            this.lore = legacyLines == null || legacyLines.length == 0
                    ? null : java.util.Arrays.asList(legacyLines);
            return this;
        }

        /** 数量；会自动夹在堆叠上限内（可用来显示进度 3/64）。 */
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        /** CustomModelData（1.20.1 存在且未废弃的 Integer 版；组件版是 1.20.5+，本库不用）。 */
        public Builder customModelData(int value) {
            this.customModelData = Integer.valueOf(value);
            return this;
        }

        /** 附魔光效（附魔 + 隐藏附魔行）：表达「已选中 / 可用」最直观的写法。 */
        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        /** 手动指定要隐藏的物品标记（附魔行、属性行、药水效果……）。 */
        public Builder flags(ItemFlag... flags) {
            this.flags = flags == null ? null : flags.clone();
            return this;
        }

        /**
         * 逃生舱：需要用到本库没暴露的新 API（1.20.5+ 的组件能力等）时自己写。
         * <p>
         * 明确一点：<b>这属于调用方自己承担的版本风险</b>——里面的调用会在哪个版本可用由你负责。
         * 留这个口子好过框架为了「兼容」而拒绝一切新 API。
         */
        public Builder meta(Consumer<ItemMeta> mutator) {
            this.metaMutator = mutator;
            return this;
        }

        /**
         * 合成最终物品：**本次显式设过的字段**优先，其余回落到 {@code defaults}。
         * <p>
         * 配置层的收尾一步：{@code Items.merge(代码默认值, builder.build())}。
         */
        public Items resolve(Items defaults) {
            return Items.merge(defaults, build());
        }

        /**
         * 合成最终物品，以构造时的底为默认值（{@code of(...)} 那条路径用）。
         */
        public Items resolve() {
            return Items.merge(base, build());
        }

        /**
         * 把「默认底 + 本次显式设过的字段」合成一个不可变的 {@link Items}。
         */
        public Items build() {
            Items merged = new Items(
                    base == null ? null : base.material,
                    base == null ? null : base.materialSource,
                    base == null ? null : base.stack,
                    base == null ? null : base.configName);
            merged.name = name != null ? name : (base == null ? null : base.name);
            merged.lore = lore != null ? lore : (base == null ? null : base.lore);
            merged.amount = amount != 1 ? amount : (base == null ? 1 : base.amount);
            merged.customModelData = customModelData != null
                    ? customModelData : (base == null ? null : base.customModelData);
            merged.glow = glow || (base != null && base.glow);
            merged.flags = flags != null ? flags : (base == null ? null : base.flags);
            merged.metaMutator = metaMutator != null ? metaMutator : (base == null ? null : base.metaMutator);
            return merged;
        }

        /** 渲染为成品 ItemStack：名字与 lore 用定义里这份（调用方已把占位符求值好）。 */
        public ItemStack stack() {
            Items items = build();
            return items.render(items.name(), items.lore());
        }
    }
}
