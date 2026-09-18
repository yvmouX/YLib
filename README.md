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

「布局即文本图」的箱子菜单框架：一行一串字符、一个字符一格，`#` 或 `` `名字` `` 就是槽位名；
一个名字可以占多格——静态槽位 `set(名字, 物品)` 整组同一物品，动态槽位 `fill(名字, 一串物品)` 按序填（列表就这么填）；
列表翻页用 `Paging` 的纯函数自己拼（切片与页码夹紧有单测），库里不塞基类，页面长什么样完全由你写。
宿主启用时调一次 `MenuListener.init(plugin)` 即可——它也顺带注册了聊天输入监听器，所以 `MenuItem.input(...)` 不用再单独注册。

```java
public final class ShopMenu extends Menu {

    private static final String[] SHAPE = {
            "#########",
            "#########",
            "`prev` `pages` `next`",
    };

    private int page;

    @Override
    protected void build() {
        layout(SHAPE);
        List<Goods> all = goods();                       // 整份列表只取一次
        int pageSize = slots("#").size();                // 每页几条 = 布局图里 # 的格数
        page = Paging.clampPage(page, all.size(), pageSize);
        fill("#", Paging.slice(all, page, pageSize).stream().map(this::card).toList());
        int totalPages = Paging.totalPages(all.size(), pageSize);
        set("pages", MenuItem.display(Material.PAPER, "&7" + (page + 1) + "/" + totalPages, lore -> { }));
        // prev / next 同理：到头了换成 MenuItem.display(GRAY_DYE, ...)
    }
}
```

想在聊天栏问一个值（改任务 id 这类），一个工厂方法就够；输入 `取消`/`cancel`、超时、退服都会放弃：

```java
set("id", MenuItem.input(Material.NAME_TAG, "&f任务 id", lore -> lore.add("&7左键编辑"),
        "只能用小写字母、数字与下划线",          // 输入提示
        () -> quest.id(),                       // 当前值；没有就给 null
        text -> {                               // 提交后自己 refresh() 或重开界面
            quest.id(text);
            refresh();
        }));
```

> 上面所有 `MenuItem` 工厂方法的 lore 参数都是 `Consumer<List<String>>`，要自己 `add`——
> 传 `List.of(...)` 编译不过，不想要 lore 就传 `lore -> { }`。

更详细的文档见 [文档/](文档/Home.md)：

| 文档 | 内容 |
|---|---|
| [命令系统](文档/命令系统.md) | 注解命令、参数类型、权限、`commands.yml`、Builder 模式 |
| [命令帮助](文档/命令帮助.md) | 统一格式的 help 输出 |
| [配置](文档/配置.md) | 注解配置、类型映射、验证规则、版本迁移 |
| [多语言](文档/多语言.md) | 语言文件、占位符、`TextRenderer` |
| [菜单](文档/菜单.md) | 箱子界面框架（布局即文本图） |
| [调度器](文档/调度器.md) | Spigot / Paper / Folia / Canvas 统一调度器 |
| [日志](文档/日志.md) | 日志级别、占位符、定向输出 |

版本变更见 [GitHub Releases](https://github.com/yvmouX/YLib/releases)（由工作流按提交自动生成）。

## Project structure

```
YLib/
├── api/                  # 对外暴露的接口 (Scheduler, Config, Command)
├── core/                 # 核心逻辑：API 定义、具体实现、文本渲染、菜单框架 (Java 8)
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
