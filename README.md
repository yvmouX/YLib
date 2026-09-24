# YLib &nbsp; &nbsp; [![GitHub Release](https://img.shields.io/github/release/yvmouX/YLib.svg?style=flat)]() &nbsp; [![](https://jitpack.io/v/yvmouX/YLib.svg)](https://jitpack.io/#yvmouX/YLib)

This is a lib for my minecraft plugins to simplifies development and provides Folia/Canvas support implementations

## Description

**Java Version**: 8+ (Folia/Paper/Canvas 专用模块为 17+)

**Supported**:

- Canvas (Paper fork, Folia-compatible)
- Folia
- Paper
- Spigot

## YLib as a dependency

This method includes YLib inside your plugin jar.

### Gradle
<details>
  <summary>[Click to show]</summary>

```groovy
plugins {
    id("com.gradleup.shadow") version "9.0.0-rc3"
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation("com.github.yvmouX:YLib:VERSION")
}

shadowJar {
    // 建议保留：YLib 优先用 ServiceLoader 找平台实现，这一步顺带把 META-INF/services 里的类名一起重定位
    mergeServiceFiles()
    relocate("cn.yvmou.ylib", "YOUR_PACKAGE.lib.ylib")
}
```
</details>

### Maven
<details>
  <summary>[Click to show]</summary>

```xml

<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependencies>
<dependency>
    <groupId>com.github.yvmouX</groupId>
    <artifactId>YLib</artifactId>
    <version>VERSION</version>
    <scope>compile</scope>
</dependency>
</dependencies>

<build>
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.0</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>shade</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <relocations>
                <relocation>
                    <pattern>cn.yvmou.ylib</pattern>
                    <!-- !! Don't forget to replace -->
                    <shadedPattern>YOUR_PACKAGE.lib.ylib</shadedPattern>
                </relocation>
            </relocations>
            <transformers>
                <!-- 建议保留：YLib 优先用 ServiceLoader 找平台实现，这一步顺带把 META-INF/services 里的类名一起重定位 -->
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
            </transformers>
        </configuration>
    </plugin>
</plugins>
</build>
```
</details>

> **`relocate` 不要省。** YLib 是进程级单例，服务、命令、配置、任务全都绑定在**第一个**初始化它的插件上。
> 两个插件各打包一份没重定位的 YLib，第二个插件调 `YLib.init(this)` 时会直接抛
> `YLibException: YLib has already been initialized by another plugin: ...`。
> 重定位之后每个插件各有一份互不干扰的副本。

### 公共 API 与内部实现

YLib 的源码分成两层，**这是有意的边界，不是目录洁癖**：

| 模块 | 内容 | 你能依赖吗 |
| --- | --- | --- |
| `api` | 你 import 的一切：`YLib`、`CommandManager`、`MenuTemplate`/`MenuBuilder`/`Items`/`Menus`、`TextRenderer`、`Argument`/`CommandNode`、`ConfigurationManager`、`Logger`、`MessageService`… | **能** —— 这些在语义化版本承诺范围内 |
| `core` | `*Impl`、`CommandDispatcher`、`ConfigurationLoader`、`LoggerUtil`… | **不能** —— 内部实现，随时会变 |

这不是口头约定：**`core` 根本不在你的编译类路径上**（根项目的 `core` 是 `implementation`
依赖）。你在 IDE 里补全不到它，误 import 会直接编译失败，而不是等到运行时才炸。

> 需要什么就去 `api` 里找；找不到说明那是内部件，不该用。
> 想加功能时也请加在 `api` 模块——`api` 编译时看不见 `core`，这个约束由构建系统强制。

## How to use

Initialize YLib in your plugin's `onEnable`:

```java
import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.YLibException;

private YLib ylib;

@Override
public void onEnable() {
    try {
        ylib = YLib.init(this);
    } catch (YLibException e) {
        getLogger().severe("Failed to initialize YLib: " + e.getMessage());
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
}
```

### Scheduler

Platform-agnostic scheduler, behaves the same on Canvas / Folia / Paper / Spigot:

```java
// 20 ticks later
YLib.getYLib().getScheduler().runLater(() -> {
    getLogger().info("Hello!");
}, 20L);

// Entity-scoped repeating task
YLib.getYLib().getScheduler().runTimer(entity, () -> {
    /* Code */
}, 0L, null, 20L);
```

### Commands

```java
import cn.yvmou.ylib.command.annotation.Command;
import cn.yvmou.ylib.command.annotation.SubCommand;
import org.bukkit.command.CommandSender;

@Command(name = "mycmd", description = "Example command")
public class MyCommand {

    @SubCommand("test")
    public void test(CommandSender sender) {
        sender.sendMessage("Hello!");
    }
}

// 注册（插件 onEnable 中）
ylib.getCommandManager().register(new MyCommand());
```

### Configuration

```java
import cn.yvmou.ylib.config.AutoConfiguration;
import cn.yvmou.ylib.config.ConfigValue;

@AutoConfiguration(value = "database", configFile = "database.yml")
public class DatabaseConfig {

    @ConfigValue("host")
    private String host = "localhost";

    @ConfigValue("port")
    private int port = 3306;
}

// 注册（插件 onEnable 中）
ylib.getConfigurationManager().registerConfiguration(DatabaseConfig.class);
```

### Messages (i18n)

```java
import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.message.MessageSettings;

MessageService messages = ylib.createMessageService(MessageSettings.builder()
        .defaultLanguage("en")
        .availableLanguages("en", "zh_CN")
        .filePattern("lang_%s.yml")      // 语言文件名模式
        .languageFolder("lang")          // 语言文件目录，默认 lang/（可用 languageFolder("") 回到插件根目录）
        .prefixKey("prefix")
        .useClientLocale(false)
        .build());

messages.send(sender, "greeting", player.getName());
```

Text is rendered by MiniMessage with legacy color codes still supported, so `<green>`,
`&a` and `§a` all work (and may be mixed) in language files and code. The message service
returns already-rendered `§` strings; render arbitrary text with `TextRenderer`:

```java
import cn.yvmou.ylib.text.TextRenderer;

String line = TextRenderer.render("<yellow>Mining</yellow> &8| &f50%");
String plain = TextRenderer.strip("<yellow>Mining</yellow>");
```

### Menus (chest GUI)

**布局即 ASCII 图，行为即声明。** 一个菜单类实现 `MenuTemplate`，只有两件事要写：`layout()` 给一张图，
`declare(b)` 说清每个符号是什么、点了做什么。宿主启用时调一次 `Menus.init(plugin)`（不调则界面能开、点了没反应），
注册期就把布局、符号、分页容量、材质名、yml 版本全部校验完——**菜单写错在启动期炸，不是玩家点开时炸**。

```java
@MenuConfig(path = "gui/adminmenu.yml", version = 1, sessionTtl = 1800)
public final class AdminMenu implements MenuTemplate {

    private final QuestService questService;

    public AdminMenu(QuestService questService) {
        this.questService = questService;
    }

    @Override
    public String[] layout() {                       // 每行 9 个符号，正好 9 格
        return new String[]{
            "#########",
            "##`quests`#c#`create`##",
            "#########"
        };
    }

    @Override
    public void declare(MenuBuilder b) {
        b.symbol('#').item("border")                 // 8 个槽位共用一份物品定义
            .of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "));

        b.symbol('c').item("create_btn")
            .of(Items.of(Material.WRITABLE_BOOK).name("&f创建任务").lore("&7点击创建"))
            .onClick((slot, ctx) -> new CreateQuestMenu(questService).open(ctx.viewer()));

        b.symbol("quests").item("quests_btn")        // 多字符符号用反引号
            .of(Items.of(Material.WRITABLE_BOOK)
                     .name("&f任务列表")
                     .lore("&7当前共 &e{count} &7个"))
            .ph("count", ctx -> questService.count(ctx.viewer()))   // 打开时求值
            .onClick((slot, ctx) -> new QuestListMenu(questService).open(ctx.viewer()));

        b.fillEmpty(Items.filler());                 // 没被符号占用的格子
        b.interaction(Interaction.READ_ONLY);        // 默认即此：玩家动不了菜单里的任何物品
    }
}
```

| 要点 | 说明 |
|---|---|
| 布局与格数 | **一行的格数 = 它的符号数**（默认 9）。`#` 是 1 格，`` `quests` `` 也是 1 格；**空白算 0 格**，只用于对齐 |
| 符号 → 槽位 | 一个符号可以占多格（边框的标准用法），回调里用 `slot.row()` / `slot.col()` 区分 |
| 占位符 | `ph(名字, ctx -> 值)`，**每次打开 / 刷新求值**，绝不在构建期冻结 |
| 点击 | `onLeftClick` / `onRightClick` / `onShiftLeftClick` / `onClick(ClickKind, …)` / 按槽位号 / 全量兜底，优先级从精确到宽泛 |
| 事件策略 | `READ_ONLY`（默认）/ `ALLOW_MOVE` / `ALLOW_TAKE`；拦点击、拖拽、漏斗抽取、丢入四种路径 |
| 配置文件 | `@MenuConfig` 一键生成 / 读取：缺文件生成、已存在不覆盖、写了的以文件为准、缺的回落代码默认值、坏项只回退那一项 |
| 分页 | `pageItems(perPage, source)`——`perPage` 必须等于该符号在布局里的格数，不符就启动报错 |
| Session | 由「玩家 × 菜单」决定，**`close()` 不销毁**，因此「看完详情返回列表还在第 3 页」天然成立 |

```java
// 分页：perPage 必须与 l 在布局里出现的次数一致
b.symbol('l').item("quest_entry")
    .pageItems(4, (viewer, page) -> questService.page(viewer, page, 4))
    .onClick((slot, ctx) -> {
        int global = ctx.page() * ctx.perPage() + ctx.indexInSymbol(slot);
        Quest q = questService.at(ctx.viewer(), global);
        if (q != null) {
            new QuestDetailMenu(questService, q).open(ctx.viewer());
        }
    });

b.symbol('p').item("prev").of(Items.of(Material.ARROW).name("&f上一页")).previousPage();
b.symbol('q').item("pageinfo").of(Items.of(Material.PAPER).name("&f第 &e{page}/{pages} &7页")).pageIndicator();
b.symbol('n').item("next").of(Items.of(Material.ARROW).name("&f下一页")).nextPage();
```

> 物品一律由 `Items` 三级阶梯给出：`Items.of(Material)`（模板写死）、`Items.of(String)`（yml 读来的材质名，
> 启动期解析 + 改名建议）、`Items.of(ItemStack)`（接管成品物品）。名字与 lore 收 `&` 色码字符串，
> 里面可以写 `{占位符}`；**不带 MiniMessage**——玩家昵称里的 `<` 会被标签解析吃掉。

完整的规则、校验清单与配置格式见 [文档/菜单.md](文档/菜单.md)。

更详细的文档见 [文档/](文档/Home.md)：

| 文档 | 内容 |
|---|---|
| [生命周期](文档/生命周期.md) | 装配顺序、配置的五步加载、热重载与关停 |
| [命令系统](文档/命令系统.md) | 注解命令、参数类型、权限、`commands.yml`、Builder 模式 |
| [命令帮助](文档/命令帮助.md) | 统一格式的 help 输出 |
| [配置](文档/配置.md) | 注解配置、类型映射、注释（`refreshComment` / `@keep`）、验证规则、版本迁移 |
| [多语言](文档/多语言.md) | 语言文件、占位符、`TextRenderer` |
| [菜单](文档/菜单.md) | 箱子界面框架（布局即文本图、声明式物品与行为） |
| [调度器](文档/调度器.md) | Spigot / Paper / Folia / Canvas 统一调度器 |
| [日志](文档/日志.md) | 日志级别、占位符、定向输出 |

版本变更见 [GitHub Releases](https://github.com/yvmouX/YLib/releases)（由工作流按提交自动生成）。

## Project structure

```
YLib/
├── api/                  # 对外暴露的一切：契约 + 框架 + 公共工具（Java 8）
│                         #   scheduler/config/command/logger/message 的接口与注解，
│                         #   menu 菜单框架（MenuTemplate/Items/Menus）、TextRenderer、
│                         #   Argument/CommandNode
│                         #   注意：本模块编译期看不见 core，边界由构建强制
├── core/                 # 只放内部实现（Java 8）：*Impl / Dispatcher / Loader / Parser / Validator
│                         #   对消费方编译期不可见（根项目里是 implementation 依赖）
├── platform/             # 平台适配层
│   ├── canvas/           # Canvas 专用实现 (Java 17, 复用 Folia 调度实现)
│   ├── folia/            # Folia 专用实现 (Java 17)
│   ├── paper/            # Paper 专用实现 (Java 17)
│   └── spigot/           # Spigot 基础实现 (Java 8)
├── 文档/                 # 中文文档
└── build.gradle.kts      # 统一管理版本和发布逻辑
```

`core` 与 `api` 停留在 Java 8：文本渲染所用的 Adventure **4.x** 全线是 Java 8 字节码，
因此不需要为了 MiniMessage 抬升消费方的 Java 门槛（5.x 才需要 Java 21）。
菜单框架同样只用 1.13 起就存在的方法，因此能在 1.13 ~ 最新版的服务端上跑。
