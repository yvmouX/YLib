# Menu API 设计稿

> Minecraft 插件(Paper/Bukkit)的箱子菜单声明式 API 设计。
> 核心思路:注解驱动、ASCII 布局、符号映射、物品构建三级阶梯。
> 本文定义完整规则:**解析规则、生命周期、符号→槽位映射、事件策略、填充机制、占位符求值时机、配置文件覆盖规则**。
> 可直接照抄的代码见 `MenuAPI-示例集.md`。

---

## 0. 十条硬性约定

| # | 约定 | 为什么 |
|---|---|---|
| 1 | **多字符符号必须加反引号,单字符符号可裸写** | 反引号是词法边界,去掉后 `##quests#create##` 无法无歧义切分 |
| 2 | **`Symbol → List<Slot>`**:一个符号可以落在 N 个槽位上,共用一份物品定义 | 边框这类"8 格同物"是常态;`onClick` 回调带 `Slot` 以便需要时区分 |
| 3 | **必须有填充机制** `fillEmpty()` | 空槽不处理就是视觉破绽,而边框填充是日常需求 |
| 4 | **事件策略必须显式且默认安全**(`interaction(...)`,默认只读全拦截) | 不写清这点 = 上线后被玩家拖走菜单物品 |
| 5 | **占位符在「打开时」求值**,不在构建时 | 构建期冻结的数值(如 `questCount`)永远不会更新 |
| 6 | **物品定义走 ItemStack 工厂**,不裸传 `Material` | `Material` 枚举跨版本剧烈变动(1.20.5 组件化),且无法表达 CMD / 玩家头 / 附魔光效 |
| 7 | **生命周期契约**:启动期解析校验 → 模板缓存 → 每次打开渲染 | 静态方法承载不了 viewer 状态与分页 |
| 8 | **点击类型覆盖全**:中键、数字键、丢弃键、F 键、双击、拖拽 | 只拦左/右键会被 shift、数字键、拖拽绕过 |
| 9 | **配置文件覆盖规则 + version 迁移**必须定死 | 「代码优先」还是「配置优先」必须二选一 |
| 10 | **文档里的示例代码必须能编译** | 示例是给人照抄的,语法错误就是 bug |

---

## 1. 设计原则

1. **布局是数据,不是代码。** 一行字符串 = 一行槽位,可读、可 diff、可换行重排。
2. **解析失败必须在启动期炸,而不是玩家点开菜单时炸。**
3. **符号是槽位组,不是槽位。** 一个符号可以落在 1 个或 N 个槽位上。
4. **构建期与渲染期分离。** 静态部分构建一次缓存;动态部分(占位符)每次打开求值。
5. **默认安全。** 不显式声明可修改 = 玩家不能动菜单里的任何物品。
6. **代码给默认值,配置可覆盖。** 覆盖粒度到「符号」,不能到「槽位」。

---

## 2. 完整示例

```java
@MenuConfig(
    generateDefault = true,        // true = 首次启动生成默认 yml;false = 不生成
    path = "gui/adminmenu.yml",    // 相对插件 dataFolder;省略扩展名等价于 .yml
    version = 1,                   // 配置版本号,用于迁移
    sessionTtl = 1800              // 玩家离线后 session(页码等)保留秒数,见 §10.4
)
public final class AdminMenu implements MenuTemplate {

    // ---------- 1) 布局 ----------
    @Override
    public String[] layout() {
        return new String[]{
        //   列 0    1    2    3    4    5    6    7    8
            "#########",
            "##`quests`#c#`create`##",
            "#########"
        };
    }

    // ---------- 2) 物品与行为 ----------
    @Override
    public void declare(MenuBuilder b) {

        // 边框:8 个槽位共用一个物品定义;需要区分具体格子时用回调里的 slot
        b.symbol('#').item("border").of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "))
            .onClick((slot, ctx) -> { /* slot.index() 可区分 8 个格子 */ });

        // 创建按钮:静态文案
        b.symbol('c').item("create_btn")
            .of(Items.of(Material.WRITABLE_BOOK)
                     .name("&f创建任务")
                     .lore("&7创建一个任务", "&7点击创建"))
            .onClick((slot, ctx) -> new CreateQuestMenu().open(ctx.viewer()));

        // 任务列表:动态占位符,打开时求值
        b.symbol("quests").item("quests_btn")
            .of(Items.of(Material.WRITABLE_BOOK)
                     .name("&f任务列表")
                     .lore("&7列出加载得起来的任务", "&7当前共 &e{count} &7个", "", "&7点击打开"))
            .ph("count", ctx -> questService.count(ctx.viewer()))   // ← 每次打开重新求值
            .onClick((slot, ctx) -> new QuestListMenu().page(0).open(ctx.viewer()));

        // ---------- 3) 兜底 ----------
        b.fillEmpty(Items.filler(Material.BLACK_STAINED_GLASS_PANE)); // 所有未声明槽位
        b.interaction(Interaction.READ_ONLY);                        // 默认即此,可显式写出
    }
}
```

---

## 3. 布局语法与槽位映射

### 3.1 语法规则

| 规则 | 说明 |
|---|---|
| 一行字符串 = 一行槽位 | 第 n 行、第 m 列 → 槽位索引 `n * 9 + m` |
| **单字符符号**可裸写 | `#` `c` `_` 各占 **1 个槽位** |
| **多字符符号必须用反引号包裹** | `` `quests` `` 占 **1 个槽位**(反引号本身不占槽位) |
| 两个多字符符号相邻,必须留分隔 | `` `quests`#`create` `` ✅ / `` `quests``create` `` ❌ 启动报错 |
| 反引号必须成对 | 未闭合 → 启动报错并指出行列 |
| 同一个符号可重复出现 | 表示「同一物品占据多个槽位」,这是边框的标准用法 |
| 同一个符号不可对应多个物品 | `b.symbol('q')` 调两次 → 启动报错 |
| 布局中的每个符号都必须在 `declare()` 里声明 | 否则启动报错并指出未知符号与首次出现的列号 |

> 为什么保留反引号:`##\`quests\`#\`create\`##` 解析为 **9 格**;去掉反引号的 `##quests#create##` 解析为 **7 格**。反引号不是装饰,是词法边界。单字符符号免打引号只是省噪音的便利,不改变规则。

**最常见的笔误**是把多字符符号忘了加引号:

```java
"##quests#c#`create`##"   // ❌ 忘了反引号
// → 启动报错:第2行第3列:符号 'q' 未声明(是否想写 `quests`?)
```

解析器会把 `q`、`u`、`e`… 当成裸写单字符符号,在第 3 列立刻因「未声明符号」失败。报错信息里带上「是否想写 \`quests\`?」(对未声明字符做前缀匹配提示)能省掉一半的调试时间,建议实现时一并加上。

### 3.2 解析算法(单遍逐字符扫描)

```java
private static final char TICK = '`';

static Map<String, List<Slot>> parse(String[] layout, int columns) {
    Map<String, List<Slot>> bySymbol = new LinkedHashMap<>();

    for (int row = 0; row < layout.length; row++) {
        String line = layout[row];
        int col = 0;          // 槽位列号(不含反引号)
        int i = 0;            // 字符串游标(含反引号)
        int symbolStartCol = -1;
        String pending = null;

        while (i < line.length()) {
            char ch = line.charAt(i);

            if (ch == TICK) {
                if (pending == null) {                 // 开引号
                    pending = "";
                    symbolStartCol = col;
                } else {                               // 闭引号
                    if (pending.isEmpty())
                        throw new MenuConfigException("第%d行第%d列:空的反引号对", row + 1, symbolStartCol + 1);
                    int slotIndex = row * columns + col;
                    bySymbol.computeIfAbsent(pending, k -> new ArrayList<>())
                             .add(new Slot(slotIndex, row, col));
                    col++;
                    pending = null;
                }
                i++;
                continue;
            }

            if (pending != null) {                     // 引号内:收集符号名
                pending += ch;
                i++;
                continue;
            }

            if (Character.isWhitespace(ch)) {           // 空白仅用于源码对齐,不占槽位
                i++;
                continue;
            }

            // 裸写的单字符符号
            bySymbol.computeIfAbsent(String.valueOf(ch), k -> new ArrayList<>())
                     .add(new Slot(row * columns + col, row, col));
            col++;
            i++;
        }

        if (pending != null)
            throw new MenuConfigException("第%d行第%d列:反引号未闭合", row + 1, symbolStartCol + 1);

        if (col != columns)
            throw new MenuConfigException("第%d行共 %d 列,期望 %d 列", row + 1, col, columns);
    }
    return bySymbol;
}
```

**关键点:反引号在整个过程中只推进 `i`(字符串游标),不推进 `col`(槽位列号)。** 这是「反引号不占槽位」在实现上的唯一体现,写测试时优先覆盖这一条。

**这段解析不需要预先知道符号表**(所以不用「最长匹配」那一套):反引号内的一切是多字符符号,反引号外的一切必然是单字符符号 —— 因为「多字符必须加引号」已经是语法规则。好处是解析与 `declare()` 的执行顺序完全无关,`layout()` 可以独立单测。

### 3.3 数据结构

```java
/** 一个具体槽位。index 是 0..53 的行主序索引(Bukkit 槽位号)。 */
public record Slot(int index, int row, int col) {}

/** 解析结果:symbol -> 它占据的所有槽位 */
Map<String, List<Slot>> slotsBySymbol;

/** 声明结果:symbol -> 物品定义 */
Map<String, ItemSpec> itemsBySymbol;
```

这解决了「8 个边框槽只能共用一份行为」的问题:`#` 是 **8 个 `Slot` 共享 1 个 `ItemSpec`**。需要在不同格子上做不同事时,从回调参数拿 `slot`:

```java
b.symbol('#').item("border")
    .onClick((slot, ctx) -> {
        if (slot.row() == 0) { /* 顶边 */ }
        else if (slot.col() == 0) { /* 左边 */ }
    });
```

---

## 4. 生命周期契约

| 阶段 | 时机 | 做什么 | 失败后果 |
|---|---|---|---|
| **注册** | `onEnable` | 扫描 `@MenuConfig` 类,登记模板 | 启动失败 |
| **解析 + 校验** | 第一次被引用时(或 `onEnable` 主动预热) | 跑 `layout()`,执行 `declare()`,产出不可变 `MenuDefinition`;跑 §5 全部校验;读/生成 yml 并叠加 | **启动失败,打印行列级错误** |
| **缓存** | 紧随解析 | `MenuDefinition` 全局单例,供所有玩家复用 | — |
| **渲染** | **每次 `open(viewer)`** | 求值占位符 → 生成全新 `ItemStack` → `inventory.setItem(...)` | 记日志,该槽位留空 |
| **派发** | 玩家点击 | 按槽位查 `symbol`,按 `ClickType` 命中回调 | 未命中 → 按 `interaction` 策略处理 |
| **关闭** | `InventoryCloseEvent` | 标 `MenuSession` 为「未打开」,**不销毁** —— 状态归菜单所有(见 §10.4) | — |

**线程**:解析期在 `onEnable` 主线程;渲染与派发必须在主线程。需要异步取数据的(数据库查任务数)请在占位符函数外先 `CompletableFuture` 预取,或干脆用缓存的数值 —— **不要在 `ph()` 里做阻塞 IO**。

**模板生命周期**:模板实例在 `onEnable` 里 `new` 出来(可以持有 Service 依赖),`Menus.register(plugin, menu)` 触发一次解析 + 校验,产出不可变 `MenuDefinition` 并缓存。`layout()` / `declare()` **只在注册期各跑一次**,之后不再调用 —— 所以「按玩家不同」只能写在 `ph()` / `visible()` / `onClick()` 内部,不能写在 `declare()` 的 if 分支里。

---

## 5. 启动期校验清单

任何一条不过 → **抛 `MenuConfigException` 并中止插件启用**(而不是记个 warning 继续跑)。

| # | 校验 | 报错信息示例 |
|---|---|---|
| 1 | `layout()` 非空,行数 1..6 | `菜单行数 7 超出 1..6` |
| 2 | 每行槽位列数 == `columns`(默认 9) | `第2行共 8 列,期望 9 列` |
| 3 | 反引号成对 | `第2行第10列:反引号未闭合` |
| 4 | 反引号对非空 | `第2行第12列:空的反引号对` |
| 5 | 单字符符号不与多字符符号互为前缀 | `symbol('q')` 与 `symbol("quests")` 同时存在 → `符号 'q' 与 'quests' 前缀冲突(裸写时无法区分)` |
| 6 | 布局中每个符号都在 `declare()` 中声明 | `符号 'rs' 未声明(首次出现在第2行第3列)` |
| 7 | 同一符号只声明一次 | `符号 '#' 被重复声明` |
| 8 | `declare()` 中声明的符号都在布局里出现 | 记 warning 即可(便于临时隐藏按钮) |
| 9 | 每个已声明符号至少占据 1 个槽位 | `符号 'x' 声明了但没有出现在布局中` |
| 10 | 占位符名不与内置冲突、无空名 | `占位符名不能为空` |
| 11 | yml 中出现的符号都在布局里 | `配置项 items.foo 未在布局中使用,已忽略` |
| 12 | yml 的 `version` ≤ 当前 `@MenuConfig.version` | 版本更高 → 拒绝加载该文件 |
| 13 | `layout`（代码里的与 yml 覆盖后的）能被解析：行数 1..6、每行 == `columns()`、反引号成对 | `第2行共 8 列,期望 9 列` |
| 14 | 布局里出现的符号都在 `declare()` 里声明过 | `符号 'quests' 未声明(首次出现在第1行第3列)` |
| 15 | 同一个符号不能同时有 `pageItems` 和固定 `of(...)` | `符号 'l' 同时声明了 pageItems 与静态物品,语义冲突` |

> 原表里的 13 / 14 是「`perPage` == 该符号的槽位数」与「`perPage` ≥ 1」。页容量改成
> **由布局推导**之后这两个数不可能对不上，两条校验随之删除（见 §10.1）。
> 而「布局写错」现在分两种来源：**代码里的布局**（`layout()`）写错仍然抛；**yml 里的 `layout`**
> 写错则整份回退代码布局 + 记 warning（配置脏数据不该让插件起不来）。

**建议**:再加一个 `MenuTemplate#dryRun()`(或 JUnit 里的 `MenuTestKit.assertValid(AdminMenu.class)`),让这些校验能在 CI 里跑,不用起服务端。

---

## 6. 物品定义(ItemStack 工厂)

### 6.0 版本基线:Paper 1.20.1+

**最低支持 `Paper 1.20.1`**。这一条决定了本节每个方法的具体实现,先列清楚哪些能用、哪些是雷。

**为什么是 1.20.1 这个位置:**

- **Adventure `Component` 已经有了。** 1.20.1 的 `ItemMeta` 里 `displayName(Component)` / `lore(List<Component>)` 都在(`setDisplayName(String)` / `setLore(List<String>)` 从 1.19 起就标了废弃)。所以我**不必**为了兼容老版本退回 String API;
- **但 1.20.5 的组件体系还没有。** `ItemStack#getData` / `DataComponentTypes` / `setCustomModelDataComponent` 这些 1.20.5+ 才有的东西,**一律不能用**。

**雷区清单(按这条基线,以下都不能碰):**

| 不能用 | 从哪个版本起才有 | 基线内替代 |
|---|---|---|
| `setCustomModelDataComponent(...)` | 1.20.5 | `setCustomModelData(Integer)`(1.20.1 存在、未废弃) |
| `ItemMeta#itemName(Component)` | 1.20.5 引入的概念 | 只设 `displayName(Component)`,别用 itemName |
| `ItemStack#getData` / `DataComponentTypes` | 1.20.5 | `ItemMeta` 常规 API |
| `Enchantment.UNBREAKING` / `.SHARPNESS` / `.EFFICIENCY` | 1.20.5 改的名 | **`Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"))`** |
| `EntityDamageEvent.getDamageSource()` | 1.20.5 | 本项目用不到 |
| Dialog API(`io.papermc.paper.dialog`) | 1.21.6 | 菜单类插件用不到 |

**两个必须写进实现的细节:**

**① 附魔常量一定不要硬编码。** 1.20.1 里是 `Enchantment.DURABILITY` / `DAMAGE_ALL` / `DIG_SPEED`,1.20.5 改成了 `UNBREAKING` / `SHARPNESS` / `EFFICIENCY` —— **旧名字在新版被删了**,硬编码任何一个都会把插件钉死在某段版本上。`glow()` 的实现必须走 key:

```java
private static final Enchantment GLINT =
    Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));   // 名字中立,跨版本安全

// glow 的本质是"加一个附魔 + 用 ItemFlag 隐藏附魔行"
if (glow && GLINT != null) {
    meta.addEnchant(GLINT, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
}
```

同理,yml 里读到的附魔也走 `getByKey`,`getByName` 是废弃 API。

**② `Material` 一律走字符串解析,不写死在模板里。** 模板里出现 `Material.WRITABLE_BOOK` 是可以的(编译期常量),但**从 yml 读材质必须用字符串**并在启动期解析:

```java
public Builder material(String configName) {
    Material m = Material.matchMaterial(configName);   // 大小写不敏感
    if (m == null) {
        throw new MenuConfigException("未知材质 '" + configName + "'" + suggest(configName));
        // 1.20.1+ 的报错信息应带上建议,例如 GRASS → SHORT_GRASS
    }
    return of(m);
}
```

原因:`Material` 枚举的**成员会改名、会增删**(不是 API 变更,是数据变更)。`GRASS` → `SHORT_GRASS` 这类改名发生在服务端更新里 —— 服主升级服务端后,他写的 yml 里那个名字就废了。**字符串 + 启动期解析 + 改名建议**,能把这类问题变成一条可读的报错,而不是运行时异常。

### 6.1 工厂签名(已按 1.20.1 基线定稿)

```java
public final class Items {
    public static Builder of(Material material) { ... }   // 模板里写死材质时用
    public static Builder of(String configName)  { ... }   // yml 读来的材质走这里
    public static Builder of(ItemStack stack)    { ... }   // 直接接管已有物品(兼容旧代码 / NBT)

    public static final class Builder {
        public Builder name(String legacyText);            // & 色码,内部 LegacyComponentSerializer 转 Component
        public Builder lore(String... legacyLines);        // 每个元素一行,也接受元素内含 \n
        public Builder amount(int n);
        public Builder customModelData(int cmd);           // 1.20.1 用 Integer 版;组件版是 1.20.5+ 不用
        public Builder glow(boolean glow);                 // 附魔 + HIDE_ENCHANTS,附魔走 getByKey
        public Builder head(String playerName);            // SkullMeta#setOwningPlayer(OfflinePlayer)
        public Builder flags(ItemFlag... flags);
        public Builder meta(Consumer<ItemMeta> mutator);   // 逃生舱:需要用的新 API 由调用方自己承担版本风险
        public ItemStack build();
    }
}
```

**为什么 `name` / `lore` 收 legacy `&` 字符串而不是 `Component`:**

- 模板、yml、占位符里到处都是 `&7`,让使用者每次手动 `Component.text(...)` 是负担;
- 转换点在工厂内部**只有一处**,换成 Component 实现也只是这一行的事;
- 对外不暴露 `Component`,框架就不必把 Adventure 作为 API 依赖泄漏给使用者。

**为什么保留 `meta(Consumer<ItemMeta>)` 这个逃生舱:** 1.20.5+ 的组件能力(如复杂 CustomModelData、`itemModel`)想用的人一定有。留一个口子让他自己写,并明确"这属于调用方自己承担的版本风险" —— 好过框架为了兼容而拒绝一切新 API。

### 6.2 两条铁律

**必须有一个 `ItemStack` 直传重载**,否则框架会被 `Material` 枚举绑死,也无法处理带 NBT / 组件的外部物品。

**渲染期:每次打开都要 `build()` 出新 `ItemStack`,绝不能把构建期的实例缓存后塞给多个玩家。** `ItemStack` 与 `ItemMeta` 可变,复用会导致 A 玩家看到的 lore 变成 B 玩家的数值、甚至附魔互相污染。这也顺带解释了为什么 `ph()` 不能在构建期求值。

### 6.3 颜色/文本

`&7` legacy 码和 `<gray>` MiniMessage **二选一,不要混**(混用时 `&` 与 `<` 同时出现会让"这行是什么格式"变得不可判断)。本设计走 legacy:若配合 `{}` 自研替换,注意 **别用 `java.text.MessageFormat`** —— 它会把文本里的单引号当转义符吃掉,中文文案与玩家昵称极易踩坑。用简单字符串替换即可:

```java
static String resolve(String raw, Map<String, Supplier<String>> ph) {
    String s = raw;
    for (var e : ph.entrySet())
        s = s.replace("{" + e.getKey() + "}", e.getValue().get());
    return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
}
```

---

## 7. 交互与点击类型

### 7.1 事件策略(默认安全)

```java
public enum Interaction {
    READ_ONLY,     // 默认:取消所有点击/拖拽/移动,不允许把物品拿出或放入
    ALLOW_MOVE,    // 允许玩家自由整理(仅适合"仓库/背包"类菜单)
    ALLOW_TAKE,    // 只允许拿走,不允许放入
}
```

`READ_ONLY` 下必须拦截的不只是 `click`:

| 事件 | 必须处理的原因 |
|---|---|
| `InventoryClickEvent` | 普通点击 |
| `InventoryDragEvent` | 拖拽可以把菜单物品分配到多个槽位,只拦 click 会被绕过 |
| `InventoryMoveItemEvent` | 漏斗/其他容器抽走菜单物品 |
| `InventoryPickupItemEvent` | 玩家丢弃物品落到菜单上 |

以及 `InventoryClickEvent` 里必须显式取消的 **`InventoryAction`**:
`MOVE_TO_OTHER_INVENTORY`(shift 点击塞进玩家背包)、`COLLECT_TO_CURSOR`(双击收集)、`HOTBAR_SWAP` / `HOTBAR_MOVE_AND_READD`(数字键换位)、`SWAP_WITH_CURSOR`、`DROP_*`(丢弃键)。

> 判断「点的是菜单还是玩家背包」用 `event.getClickedInventory() == menuInventory`,**不要**用 `event.getRawSlot() < size` ——后者在 `getView().getTopInventory()` 与 `getInventory()` 语义不同的服务端/代理环境下会出错。

### 7.2 点击类型

点击类型必须把**键位**和**修饰键**分成两个维度,不能混在一个枚举里 —— 否则 `ShiftClick` 到底是左键还是右键都说不清。用枚举 + 显式组合:

```java
public enum ClickKind { LEFT, RIGHT, MIDDLE, NUMBER_KEY, DROP, SWAP_OFFHAND, DOUBLE_CLICK, UNKNOWN }
public record ClickType(ClickKind kind, boolean shift, boolean control) {}

// 便捷入口(内部映射到上面的组合)
b.symbol('c').item("create_btn").of(...)
    .onLeftClick(ctx -> ...)            // LEFT
    .onRightClick(ctx -> ...)           // RIGHT
    .onShiftLeftClick(ctx -> ...)       // LEFT + shift
    .onClick((slot, ctx) -> ...)        // 全量兜底,优先级最低
    .onClick(ClickKind.MIDDLE, (slot, ctx) -> ...);   // 精确指定
```

**优先级**:精确 `ClickKind` 匹配 > 便捷入口 > `onClick` 全量兜底 > 未命中则按 `interaction` 策略处理(READ_ONLY 下静默取消)。

`ctx` 提供:`viewer()`(Player)、`clickType()`、`event()`、`menu()`(可 `close()` / `refresh()` / `open(other)`)、`session()`(存放分页等 per-viewer 状态)、`symbolSlots()`(当前符号的槽位列表)。

---

## 8. 填充机制

```java
b.fillEmpty(Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" "));   // 兜底:所有未被符号占用的槽位
```

槽位归属的判定顺序:

1. 被任一符号占据的槽位 → 用该符号的物品;
2. 其余槽位 → 用 `fillEmpty` 的物品;
3. 未设 `fillEmpty` → 留空(空气)。

**注意**:`fillEmpty` 的填充物**不参与点击派发**(没有符号 → 没有回调),点击它等同于点到空槽。若希望边框可点击,就老实用 `#` 声明成符号 —— 这也是为什么「显式符号」和「兜底填充」要并存而不是二选一。

---

## 9. 配置文件

### 9.1 生成规则

- `generateDefault = true`:**文件不存在时**写入默认骨架;**已存在则绝不覆盖**。
- `generateDefault = false`:不生成,但**仍会尝试读取**同名文件做覆盖(方便"配置由运维预置"的场景)。
- `path` 必须以 `res://` 之外的相对路径书写,基准是 `plugin.getDataFolder()`;也可以显式写 `res://gui/adminmenu.yml` 表示打进 jar 的**只读内置配置**(作为默认值来源,不落地到磁盘)。
- 生成时对每个符号写入当前代码默认值,便于服主直接改。**不写注释**(阶段 88 的决定:语义放文档,
  文件保持干净、可 diff)。

### 9.2 覆盖规则

**配置优先于代码,覆盖粒度是「符号」,不是「槽位」。**

```yaml
version: 1
items:
  border:                                   # ← 对应 b.symbol('#').item("border")
    material: GRAY_STAINED_GLASS_PANE
    name: " "
  create_btn:
    material: WRITABLE_BOOK
    name: "&f创建任务"
    lore:
      - "&7创建一个任务"
      - "&7点击创建"
    # 可选:把行为改成声明式的(适合不会写代码的服主)
    actions:
      - type: open_menu
        menu: CreateQuestMenu
  quests_btn:
    material: WRITABLE_BOOK
    name: "&f任务列表"
    lore:
      - "&7列出加载得起来的任务"
      - "&7当前共 &e{count} &7个"
      - ""
      - "&7点击打开"
    actions:
      - type: message
        text: "&7共 {count} 个任务"
      - type: console_command
        command: "quests list {player}"
```

`actions` 支持的声明式类型(`type`):`message` / `player_command` / `console_command` / `open_menu` / `close` / `refresh` / `next_page` / `prev_page`。**若 yml 里写了 `actions`,则由 actions 完全接管该符号的点击行为,代码里的 `.onClick()` 被忽略**(并在启动日志里提示),避免两处逻辑打架。

### 9.3 合并语义

| 情况 | 行为 |
|---|---|
| yml 缺某个符号 | 用代码默认值 |
| yml 缺某个字段(如只写了 `name`) | 该字段用代码默认值,其余字段合并 |
| yml 有布局里不存在的符号 | 忽略 + 启动 warning |
| 布局里有符号但 yml 没有 | 用代码默认值,不报错 |
| yml `version` < 代码 `version` | 跑迁移链,升版后**备份原文件**再写回 |
| yml `version` > 代码 `version` | 拒绝加载,提示服务端插件需升级 |
| yml 语法错误 | 启动失败,打印行号(不要静默降级) |

### 9.4 多语言(预留)

`path` 里若含占位 `{locale}`,则按玩家 locale 加载 `gui/adminmenu_zh_CN.yml` / `gui/adminmenu_en_US.yml`,缺失则回退到默认文件。本文只留接口,不强制实现。

---

## 10. 分页菜单

任务列表这类「数量不定」的菜单需要专门的分页支持,否则只能手写槽位计算。

### 10.1 页容量(`perPage`)由布局决定

```java
.pageItems(4, (viewer, page) -> questService.page(viewer, page, 4))
```
↑ **这是重构时最初的写法，已被推翻**：`perPage` 不再由使用者声明，而是「该符号在布局里占几格」。
现在的签名是：

```java
b.symbol('l').pageItems((viewer, page, perPage) -> questService.page(viewer, page, perPage));
//                                    ↑ 框架按布局算好交给你，代码里不该再写一个常量
```

**为什么改掉原来的「显式声明」**：那个数（每页几条）与「符号在布局里占几格」本该是同一个数，
声明两遍就多了一处可以不一致的地方，于是不得不加一条启动期校验去拦「改了布局忘了改 perPage」。
把冗余去掉之后，这条校验连同它的报错一起消失——**布局改了容量自动跟着改，不存在忘不忘**。
数据源仍然拿得到 `perPage`（它要切片），只是这个数从框架来，不从常量来。

> 「总条数」仍然由数据源自己报：实现 `PageSource.total(viewer)` 才有确定的 `pageCount()`，
> 用 `Fetcher`（lambda）就是「未知」，此时页码显示 `?`，「下一页」按「这一页取满了 `perPage` 条」判断。
> `PageSource` 刻意**不是**函数式接口（`total()` 没有默认值），因此它不会和 `Fetcher` 抢 lambda。

### 10.2 逐槽位渲染 + 显式清空(末页不满的安全保证)

渲染该符号时,**遍历它的每一个槽位**,按下标取数据:

```java
List<ItemStack> items = source.fetch(viewer, page);          // 业务侧返回,长度可 < slotCount
List<Slot> slots = definition.slotsOf(symbol);
for (int i = 0; i < slots.size(); i++) {
    ItemStack stack = (i < items.size()) ? items.get(i) : AIR;   // ← 缺的写空气,不是"跳过"
    inventory.setItem(slots.get(i).index(), stack);
}
```

规则写死三条:

1. **条目不足 → 显式写空气**。绝不能沿用上一次渲染的残留,否则上一页的任务会留在屏幕上,玩家点它会打开一个不属于本页的任务;
2. **条目超出 `perPage` → 截断**,并打一条 warning(`符号 'l' 每页返回 7 条,超出容量 4,已截断`);
3. **页内顺序 = 槽位顺序**。`items.get(i)` 固定写进 `slots.get(i)`,`slots` 的顺序是布局里从左到右、从上到下,不随渲染变化。

### 10.3 槽位 → 数据下标

框架提供 `ctx.indexInSymbol(slot)`,等价于 `symbolSlots().indexOf(slot)` 但由框架直接算出,使用者不必自己反查:

```java
.onClick((slot, ctx) -> {
    int global = ctx.page() * ctx.perPage() + ctx.indexInSymbol(slot);   // 全局下标
    Quest q = questService.at(ctx.viewer(), global);
    if (q != null) new QuestDetailMenu(questService, q).open(ctx.viewer());
});
```

### 10.4 Session 生命周期:状态归「菜单」所有,不归「一次打开」

这是分页设计里最关键的一条。**`MenuSession` 由 (玩家 × 菜单) 决定,不随 `close()` 销毁。**

| 事件 | 对 session 的影响 |
|---|---|
| `open(player)` 首次打开 | 新建 session(page = 0) |
| `close(player)` | **保留**。只标记"当前未打开" |
| 再次 `open(player)` 同一个菜单 | **复用**,页码停在关闭前的位置 ← "看完详情返回列表还在第 3 页"靠这条实现 |
| 玩家退出登录 / 被踢 | 按 `@MenuConfig.sessionTtl`(默认 30 分钟)回收;或立即回收(可配) |
| `menu.resetSession(player)` | 显式清空(想让"重开就回到第一页"时用) |

由此推出几条必须写进实现的行为:

- **`MenuSession` 必须有两个维度**:"这次打开"的临时状态(如渲染缓存)和"这个菜单"的持久状态(页码、排序方式、筛选条件)。前者 `close()` 清,后者 `close()` 留。§11 的 `MenuSession` 提供 `page()` / `get(key)` / `put(key, value)` 都属于后者;
- **内存上限**:一个玩家 × 每个菜单一份,`get/put` 的 value 应限制为轻量值(页码、枚举、字符串)。**不要把 `List<ItemStack>` 或数据库结果塞进 session** —— 那样等于把玩家数据常驻内存,50 人 × 20 个菜单就是泄漏;
- **必须有回收入口**:退出登录清 + TTL 兜底 + `/menu session clear [player]` 管理命令。只靠 `close()` 是不够的,因为 `close()` 不再销毁 session 了。

### 10.5 翻页动作

```java
// 上一页:边界判断在业务侧(框架不知道总页数)
b.symbol('p').item("prev").of(Items.of(Material.ARROW).name("&f上一页"))
    .onClick(ctx -> {
        if (ctx.page() > 0) { ctx.page(ctx.page() - 1); ctx.menu().refresh(); }
    });

// 下一页:用数据源自己的 total 判断,不要靠"返回空列表"来猜
b.symbol('n').item("next").of(Items.of(Material.ARROW).name("&f下一页"))
    .onClick(ctx -> {
        int next = ctx.page() + 1;
        if (next < ctx.pageCount()) { ctx.page(next); ctx.menu().refresh(); }
    });

// 页码指示器:整页共享一个值,用 ph 而不是 pageItems
b.symbol('q').item("pageinfo").of(Items.of(Material.PAPER).name("&f第 &e{page}/{pages} &7页"))
    .ph("page",  ctx -> String.valueOf(ctx.page() + 1))     // 显示用 1-based
    .ph("pages", ctx -> ctx.pageCount() < 0 ? "?" : String.valueOf(ctx.pageCount()));
```

- **`pageCount()` 从哪来**:`PageSource` 增加 `default int total(Player viewer) { return -1; }`。`-1` = 未知(无限流/游标式数据),此时页码显示 `?` 且"下一页"用"返回长度 < perPage 即到底"判断;
- **不要用 `catch` 越界来代替边界判断**:`page(n)` 写入时框架只做 `n >= 0` 校验,上界由 `pageCount()` 决定;
- **翻页只用 `refresh()`,不 `open()`**:`open()` 会重建背包(闪屏、丢光标物品),`refresh()` 原地重写槽位。

### 10.6 完整示例

```java
@Override
public String[] layout() {
    return new String[]{
    //   0    1    2    3    4    5    6    7    8
        "#########",
        "#*     *#",     // 标题栏
        "#*l***l*#",     // l 在 col2 / col6 → 每页 4 条(两行)
        "#*l***l*#",
        "#*p*q*n*#",     // 导航行:列号与列表行错开,避免看错
        "#########"
    };
}

@Override
public void declare(MenuBuilder b) {
    b.symbol('*').item("empty").of(Items.of(Material.AIR));

    // perPage = 4 必须与布局里 l 的出现次数一致,否则启动期报错
    b.symbol('l').item("quest_entry")
        .of(Items.of(Material.PAPER))
        .pageItems(4, (viewer, page) -> questService.page(viewer, page, 4))
        .onClick((slot, ctx) -> {
            int global = ctx.page() * ctx.perPage() + ctx.indexInSymbol(slot);
            Quest q = questService.at(ctx.viewer(), global);
            if (q != null) new QuestDetailMenu(questService, q).open(ctx.viewer());
        });

    b.symbol('p').item("prev").of(Items.of(Material.ARROW).name("&f上一页"))
        .onClick(ctx -> { if (ctx.page() > 0) { ctx.page(ctx.page() - 1); ctx.menu().refresh(); } });

    b.symbol('q').item("pageinfo").of(Items.of(Material.PAPER).name("&f第 &e{page}/{pages} &7页"))
        .ph("page",  ctx -> String.valueOf(ctx.page() + 1))
        .ph("pages", ctx -> ctx.pageCount() < 0 ? "?" : String.valueOf(ctx.pageCount()));

    b.symbol('n').item("next").of(Items.of(Material.ARROW).name("&f下一页"))
        .onClick(ctx -> {
            int next = ctx.page() + 1;
            if (next < ctx.pageCount()) { ctx.page(next); ctx.menu().refresh(); }
        });

    b.interaction(Interaction.READ_ONLY);
}
```

---

## 11. 接口骨架

```java
public interface MenuTemplate {
    String[] layout();                                 // 实例方法:模板可以持有 Service
    void declare(MenuBuilder b);
    default int columns() { return 9; }
    default String title() { return ""; }
}

public final class MenuBuilder {
    public SymbolBinder symbol(char c);                // 单字符符号,布局里可裸写
    public SymbolBinder symbol(String name);           // 多字符符号,布局里须用反引号
    public void fillEmpty(Items.Builder item);         // 未被符号占用的槽位的兜底物品
    public void interaction(Interaction mode);         // 默认 READ_ONLY
    public void delayedRefresh(boolean enabled);       // 默认 false;ALLOW_MOVE 时必须 false
    public void onOpen(OpenHandler h);                 // (viewer, inventory, session)
    public void onClose(CloseHandler h);               // (viewer, inventory, reason, session)
    public MenuDefinition build();
}

public final class SymbolBinder {
    public SymbolBinder item(String configKey);        // yml 覆盖用的键
    public SymbolBinder of(Items.Builder builder);     // 模板中含 {占位符}
    public SymbolBinder of(ItemStack stack);           // 成品,不再解析占位符
    public SymbolBinder ph(String name, Function<Ctx, String> value);   // 打开时求值
    public SymbolBinder visible(Predicate<Ctx> p);     // false → 该符号所有槽位回落到 fillEmpty
    public SymbolBinder onClick(ClickHandler h);                        // 兜底,优先级最低
    public SymbolBinder onClick(ClickKind kind, ClickHandler h);
    public SymbolBinder onClick(Map<Integer, ClickHandler> bySlot);     // 按 Bukkit 槽位号
    public SymbolBinder onLeftClick(ClickHandler h);
    public SymbolBinder onRightClick(ClickHandler h);
    public SymbolBinder onShiftLeftClick(ClickHandler h);
    public SymbolBinder onShiftRightClick(ClickHandler h);
    public SymbolBinder pageItems(Fetcher fetcher);                     // 每页条数由布局给,见 §10
    public SymbolBinder pageItems(PageSource source);                   // 同上,另报总条数
}

/** 分页取数的便捷写法(函数式接口)。 */
@FunctionalInterface
public interface Fetcher {
    List<ItemStack> fetch(Player viewer, int page, int perPage);
}

/**
 * 分页数据源。**刻意不是函数式接口**——total() 没有默认值,因此它不会和 Fetcher 抢 lambda:
 * 「总条数不知道」不是一种要传 -1 的状态,而是「你该用 Fetcher」。
 */
public interface PageSource {
    List<ItemStack> fetch(Player viewer, int page, int perPage);
    int total(Player viewer);
}

@FunctionalInterface
public interface ClickHandler {
    void handle(Slot slot, Ctx ctx);
}

public interface Ctx {
    Player viewer();
    ClickType clickType();
    InventoryClickEvent event();
    MenuControl menu();               // close() / refresh() / isOpen(player) / open(player)
    MenuSession session();            // 跨「打开」的持久状态,见 §10.4
    List<Slot> symbolSlots();         // 当前符号占据的槽位(未声明的符号为 null)

    // —— 分页便捷入口(等价于 session() 里对应方法,少写一层)——
    int  page();                      // 当前页,0-based
    void page(int page);              // 写入时校验 >= 0
    int  perPage();                   // 当前符号声明的每页条数
    int  pageCount();                 // ceil(total / perPage);total 未知时返回 -1
    int  indexInSymbol(Slot slot);    // 槽位 → 页内下标,免去自己 indexOf
}

/** (玩家 × 菜单) 维度的持久状态。close() 不销毁,见 §10.4。 */
public interface MenuSession {
    int  page();
    void page(int page);
    <T> T get(String key);            // 只能放轻量值:页码、枚举、字符串
    void put(String key, Object value);
    void reset();                     // 清空(回第一页、丢自定义键)
}
```

**优先级**:精确 `ClickKind` 匹配 > 便捷入口(`onLeftClick` 等)> `onClick` 全量兜底 > 未命中则按 `interaction` 策略处理(`READ_ONLY` 下静默取消)。

---

## 12. 待定问题(需要你拍板)

1. **配置优先级**:本文定的是「配置覆盖代码」。若你希望代码是唯一真相、yml 只作导出快照,则 §9.3 整张表要反过来,并且 `generateDefault` 应改名为 `exportDefault`。
2. **`actions` 声明式行为要不要做?** 做了对服主友好,但要设计一套 action DSL 和安全边界(命令注入、`{player}` 转义)。
3. **是否需要 `MenuDefinition` 热重载?** 支持 `/menu reload` 就得处理旧 `InventoryHolder` 上已打开的菜单如何迁移,复杂度不低,建议先不做。

---

## 13. 版本基线(已定)

**最低支持 `Paper 1.20.1`。** 完整约束见 §6.0,摘要:

| 项 | 结论 |
|---|---|
| 平台 | **Paper**(不是 Spigot)。Adventure `Component` 是 Paper 侧的 API,靠它才能用 `displayName(Component)` |
| 文本 | `&` legacy 码 → 工厂内部 `LegacyComponentSerializer` 转 `Component` |
| 物品数据 | 一律走 `ItemMeta`;**不用** 1.20.5+ 的 `DataComponentTypes` / `getData` |
| CustomModelData | `setCustomModelData(Integer)`(1.20.1 存在且未废弃) |
| 附魔 | 必须 `Enchantment.getByKey(NamespacedKey.minecraft(...))`,**禁止**硬编码 `Enchantment.DURABILITY` 之类的常量(1.20.5 已改名并把旧名删除) |
| `Material` | 模板内可写枚举;yml 读来的**必须走字符串 + 启动期解析 + 改名建议** |
| 逃生舱 | `Items.Builder.meta(Consumer<ItemMeta>)` 允许调用方自行使用新 API,版本风险自负 |
| CI | 至少跑一次 1.20.1 与一个 1.20.5+ 版本的实际启动测试,确认 `glow()` 与 yml 材质解析两端都不炸 |

**由此明确不做的事**(避免过度设计):

- 不做 1.19 及以下的兼容分支;
- 不做 Spigot 的 `setDisplayName(String)` 回退路径;
- 不做 `Material` 名字的完整映射表 —— 只做"未知材质时给改名建议",不维护历史别名库;
- 不做多版本 NMS / 反射适配层。
