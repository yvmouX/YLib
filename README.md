# YLib &nbsp; &nbsp; [![GitHub Release](https://img.shields.io/github/release/yvmouX/YLib.svg?style=flat)]()

This is a lib for my minecraft plugins to simplifies development and provides Folia support implementations

## Description

**Java Version**: 8+ (Folia/Paper 专用模块为 17+)

**Supported**:

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
    // 合并 META-INF/services 文件，YLib 的 ServiceLoader 依赖它
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
                <!-- 合并 META-INF/services 文件，YLib 的 ServiceLoader 依赖它 -->
                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
            </transformers>
        </configuration>
    </plugin>
</plugins>
</build>
```
</details>

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

Platform-agnostic scheduler, behaves the same on Folia / Paper / Spigot:

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

更详细的文档见 [文档/](文档/Home.md)。

## Project structure

```
YLib/
├── api/                  # 对外暴露的接口 (Scheduler, Config, Command)
├── core/                 # 核心逻辑：API 定义、具体实现 (Java 8)
├── platform/             # 平台适配层
│   ├── folia/            # Folia 专用实现 (Java 17)
│   ├── paper/            # Paper 专用实现 (Java 17)
│   └── spigot/           # Spigot 基础实现 (Java 8)
├── 文档/                 # 中文文档
└── build.gradle.kts      # 统一管理版本和发布逻辑
```
