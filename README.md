# YLib &nbsp; &nbsp; [![GitHub Release](https://img.shields.io/github/release/yvmouX/YLib.svg?style=flat)]()

This is a lib for my minecraft plugins to simplifies development and provides Folia support implementations

## Description

**Java Version**: 17+

**Supported**:

- Folia
- Paper
- Spigot

## YLib as a dependency

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
    relocate("com.github.yvmouX.ylib", "YOUR_PACKAGE.lib.ylib")
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
                        <pattern>com.github.yvmouX.yliib</pattern>
                        <!-- !! Don't forget to replace -->
                        <shadedPattern>YOUR_PACKAGE.lib.folialib</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>
```
</details>


## How to use

```java
private static YLib ylib;

@Override
public void onEnable() {
        ylib = new YLib(this);
}

public static YLib getYLib() {
        return ylib;
}
```

```java
# 除了下面示例的两个功能，不建议使用其他的了。因为我正在重构它。
# In addition to the two functions shown below, it is not recommended to use others. Because I am 
# refactoring it.
    
# 调度器
# Scheduler
Main.getYLib().getScheduler().runTask(() -> {
    /* Code */
})
# 注册命令
# registerCommands
getYLib().getCommandManager().registerCommands(
                "yess",
                new ReloadCmd(this),
);

public class ReloadCmd implements SubCommand {
    private final Y plugin;
    public ReloadCmd(Y plugin) { this.plugin = plugin; }

    @Override
    @CommandOptions(name = "reload", permission = "yess.command.reload", onlyPlayer = true, alias = {}, register = true, usage = "/yess reload")
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "成功重载插件！");
        return true;
    }
}
```