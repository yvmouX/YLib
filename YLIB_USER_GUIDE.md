# YLib 使用指南

<div align="center">

![YLib Logo](https://img.shields.io/badge/YLib-v1.0.0--beta5-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8--1.21+-green?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Spigot%20|%20Paper%20|%20Folia-orange?style=for-the-badge)

**YLib** - 现代化的 Minecraft 插件开发库  
让插件开发变得更简单、更高效、更可靠

</div>

---

## 📋 目录

1. [简介](#简介)
2. [快速开始](#快速开始)
3. [核心功能](#核心功能)
   - [依赖注入](#依赖注入)
   - [自动配置](#自动配置)
   - [错误处理](#错误处理)
   - [命令系统](#命令系统)
   - [调度器](#调度器)
   - [消息系统](#消息系统)
4. [高级特性](#高级特性)
5. [最佳实践](#最佳实践)
6. [示例项目](#示例项目)
7. [常见问题](#常见问题)
8. [API参考](#api参考)

---

## 🎯 简介

YLib 是一个专为 Minecraft 插件开发设计的现代化开发库，支持 Spigot、Paper 和 Folia 平台。它提供了一套完整的工具和服务，让插件开发变得更加简单和高效。

### ✨ 主要特性

- 🔄 **自动平台检测** - 无需手动配置，自动适配不同服务器平台
- 🏗️ **依赖注入** - 现代化的服务管理和依赖注入容器
- ⚙️ **自动配置** - 基于注解的配置管理，约定优于配置
- 🛡️ **错误处理** - 用户友好的错误消息和智能恢复机制
- 📦 **统一调度器** - 跨平台的任务调度API
- 🎨 **消息系统** - 强大的消息格式化和发送功能
- 🔧 **工具集合** - 丰富的实用工具和辅助函数

### 🎮 支持的平台

| 平台 | 版本支持 | 状态 |
|------|----------|------|
| **Spigot** | 1.8.8 - 1.21+ | ✅ 完全支持 |
| **Paper** | 1.8.8 - 1.21+ | ✅ 完全支持 |
| **Folia** | 1.19.4+ | ✅ 完全支持 |

---

## 🚀 快速开始

### 1. 添加依赖

#### Maven
```xml
<dependency>
    <groupId>cn.yvmou</groupId>
    <artifactId>ylib-spigot</artifactId> <!-- 或 ylib-paper, ylib-folia -->
    <version>1.0.0-beta5</version>
</dependency>
```

#### Gradle
```gradle
dependencies {
    implementation 'cn.yvmou:ylib-spigot:1.0.0-beta5' // 或 ylib-paper, ylib-folia
}
```

### 2. 基础设置

```java
public class MyPlugin extends JavaPlugin {
    
    private YLibAPI ylib;
    
    @Override
    public void onEnable() {
        // 初始化YLib - 自动检测平台
        ylib = YLibFactory.create(this);
        
        // 开始使用YLib的功能
        ylib.getLoggerService().info("插件已启用，使用 " + ylib.getServerType().name() + " 平台");
        
        // 注册命令、监听器等
        setupCommands();
        setupListeners();
    }
    
    @Override
    public void onDisable() {
        // YLib会自动清理资源
        ylib.getLoggerService().info("插件已禁用");
    }
    
    public YLibAPI getYLib() {
        return ylib;
    }
}
```

---

## 🔧 核心功能

### 🏗️ 依赖注入

YLib提供了一个轻量级但功能强大的依赖注入容器，让服务管理变得简单。

#### 基本使用

```java
// 1. 定义服务接口
public interface EconomyService {
    double getBalance(Player player);
    boolean withdraw(Player player, double amount);
    boolean deposit(Player player, double amount);
}

// 2. 实现服务
public class SimpleEconomyService implements EconomyService {
    private final DataStorage storage;
    private final LoggerService logger;
    
    public SimpleEconomyService(DataStorage storage, LoggerService logger) {
        this.storage = storage;
        this.logger = logger;
    }
    
    // 实现方法...
}

// 3. 注册服务
@Override
public void onEnable() {
    ylib = YLibFactory.create(this);
    
    // 注册服务
    ServiceContainer container = ylib.getServiceContainer();
    
    // 单例注册
    container.registerSingleton(DataStorage.class, new FileDataStorage(this));
    
    // 工厂注册（自动解析依赖）
    container.registerFactory(EconomyService.class, () -> {
        DataStorage storage = container.getRequiredService(DataStorage.class);
        LoggerService logger = container.getRequiredService(LoggerService.class);
        return new SimpleEconomyService(storage, logger);
    });
}

// 4. 使用服务
public class MoneyCommand implements CommandExecutor {
    private final ServiceContainer container;
    
    public MoneyCommand(ServiceContainer container) {
        this.container = container;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 获取所需服务
        EconomyService economy = container.getRequiredService(EconomyService.class);
        MessageService message = container.getRequiredService(MessageService.class);
        
        // 使用服务...
    }
}
```

#### 服务生命周期

YLib支持三种服务生命周期：

1. **Singleton（单例）**
   ```java
   container.registerSingleton(EconomyService.class, new SimpleEconomyService());
   ```

2. **Factory（工厂）**
   ```java
   container.registerFactory(EconomyService.class, () -> new SimpleEconomyService());
   ```

3. **Transient（瞬态）**
   ```java
   container.registerTransient(EconomyService.class, () -> new SimpleEconomyService());
   ```

#### 自动资源清理

实现`AutoCloseable`的服务会在插件禁用时自动清理：

```java
public class DatabaseService implements AutoCloseable {
    private final Connection connection;
    
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
```

### ⚙️ 自动配置

YLib提供了基于注解的配置管理系统，让配置变得简单而强大。

#### 基本使用

```java
// 1. 定义配置类
@AutoConfiguration("database")
public class DatabaseConfig {
    
    @ConfigValue(value = "host", defaultValue = "localhost")
    private String host;
    
    @ConfigValue(value = "port", defaultValue = "3306", min = 1, max = 65535)
    private int port;
    
    @ConfigValue(value = "username", defaultValue = "root")
    private String username;
    
    @ConfigValue(value = "password")
    private String password;
    
    // Getter方法...
}

// 2. 注册配置
@Override
public void onEnable() {
    ylib = YLibFactory.create(this);
    
    // 注册配置类
    DatabaseConfig config = ylib.getConfigurationManager().registerConfiguration(DatabaseConfig.class);
    
    // 使用配置
    String connectionUrl = String.format("jdbc:mysql://%s:%d/mydb",
        config.getHost(), config.getPort());
}
```

#### 配置验证

```java
@AutoConfiguration("shop")
public class ShopConfig {
    
    @ConfigValue(
        value = "min-price",
        defaultValue = "0.01",
        min = 0.01,
        description = "最低商品价格"
    )
    private double minPrice;
    
    @ConfigValue(
        value = "currency-symbol",
        defaultValue = "$",
        regex = "^[\\$¥€£]$",
        description = "货币符号"
    )
    private String currencySymbol;
    
    @ConfigValue(
        value = "shop-type",
        defaultValue = "NORMAL",
        enumValues = {"NORMAL", "ADMIN", "BLACK_MARKET"},
        description = "商店类型"
    )
    private String shopType;
}
```

#### 配置重载和监听

```java
// 注册配置监听器
ylib.getConfigurationManager().addConfigurationListener(DatabaseConfig.class, 
    (oldConfig, newConfig) -> {
        // 配置变更时的处理逻辑
        reconnectDatabase(newConfig);
    });

// 重载配置
ylib.getConfigurationManager().reloadConfiguration(DatabaseConfig.class);
```

### 🛡️ 错误处理

YLib提供了强大的错误处理机制，让错误信息更加用户友好。

#### 基本使用

```java
public void riskyOperation(Player player) {
    ErrorContext context = new ErrorContext("PlayerService", "teleport", "MyPlugin")
        .addContext("player", player.getName())
        .addContext("world", player.getWorld().getName());
    
    try {
        // 可能出错的操作
        teleportPlayer(player);
        
    } catch (Exception e) {
        // 使用错误处理器
        YLibErrorHandler.ErrorHandlingResult result = 
            ylib.getErrorHandler().handleError(e, context);
        
        // 显示用户友好消息
        player.sendMessage("§c" + result.getUserMessage());
        
        // 显示恢复建议
        for (String suggestion : result.getSuggestions()) {
            player.sendMessage("§e建议: " + suggestion);
        }
    }
}
```

#### 增强异常

```java
// 创建带有详细信息的异常
throw new YLibException(
    "传送失败",                // 技术错误消息
    originalException,         // 原始异常
    context,                  // 错误上下文
    "无法传送到目标位置",       // 用户友好消息
    ErrorSeverity.ERROR,      // 错误严重程度
    "TELEPORT_FAILED"         // 错误代码
)
.addRecoverySuggestion("请确保目标位置是安全的")
.addRecoverySuggestion("尝试传送到其他位置");
```

#### 错误监听

```java
public class ErrorMonitor implements YLibErrorHandler.ErrorListener {
    
    @Override
    public void onError(Throwable error, ErrorContext context, 
                       YLibErrorHandler.ErrorHandlingResult result) {
        // 记录错误
        logError(error, context);
        
        // 通知管理员
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            if (ylibError.getSeverity().isMoreSevereThan(ErrorSeverity.ERROR)) {
                notifyAdmins(result.getUserMessage());
            }
        }
    }
}

// 注册监听器
ylib.getErrorHandler().addErrorListener(new ErrorMonitor());
```

### 📦 命令系统

YLib提供了一个强大而简洁的命令注册和管理系统。

#### 基本使用

```java
// 1. 定义子命令
@CommandOptions(
    name = "reload",
    permission = "myplugin.admin.reload",
    onlyPlayer = false,
    alias = {"rl", "restart"},
    usage = "/myplugin reload",
    description = "重新加载插件配置"
)
public class ReloadCommand implements SubCommand {
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 重载逻辑
        sender.sendMessage("§a插件配置已重新加载！");
        return true;
    }
}

// 2. 注册命令
@Override
public void onEnable() {
    ylib = YLibFactory.create(this);
    
    // 注册命令
    ylib.getCommandManager().registerCommands("myplugin",
        new ReloadCommand(),
        new InfoCommand(),
        new HelpCommand()
    );
}
```

#### 命令配置

YLib会自动生成`commands.yml`配置文件：

```yaml
_info:
- YLib 命令配置文件
- 该文件用于配置插件命令的各种选项

commands:
  myplugin:
    reload:
      permission: myplugin.admin.reload
      onlyPlayer: false
      register: true
      usage: /myplugin reload
      alias:
      - rl
      - restart
```

#### Tab补全支持

```java
@CommandOptions(name = "teleport", permission = "myplugin.teleport")
public class TeleportCommand implements SubCommand {
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回在线玩家列表
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
```

### 📅 调度器

YLib提供了跨平台的统一调度器API，自动适配不同服务器平台。

#### 基本使用

```java
// 延迟任务
ylib.getSchedulerManager().runTaskLater(() -> {
    // 延迟执行的代码
}, 20); // 1秒后

// 重复任务
UniversalTask task = ylib.getSchedulerManager().runTaskTimer(() -> {
    // 每20秒执行一次
}, 0, 20 * 20);

// 异步任务
ylib.getSchedulerManager().runTaskAsync(() -> {
    // 异步执行的代码
    
    // 回到主线程
    ylib.getSchedulerManager().runTask(() -> {
        // 主线程代码
    });
});

// 取消任务
task.cancel();
```

#### Folia区域调度

```java
// 获取区域调度器
RegionScheduler scheduler = ylib.getSchedulerManager().getRegionScheduler();

// 在特定区块执行任务
scheduler.execute(chunk, () -> {
    // 在指定区块执行的代码
});
```

### 💬 消息系统

YLib提供了强大的消息格式化和发送功能。

#### 基本使用

```java
MessageService message = ylib.getMessageService();

// 基础消息
message.sendMessage(player, "&a欢迎来到服务器！");

// 带参数的消息
message.sendMessage(player, 
    "&b玩家 &e{player} &b加入了服务器！在线人数: &c{count}",
    "player", player.getName(),
    "count", Bukkit.getOnlinePlayers().size());

// 多行消息
message.sendMessage(player, Arrays.asList(
    "&6==================",
    "&a  欢迎来到服务器！",
    "&b  玩家: &e{player}",
    "&b  等级: &c{level}",
    "&6==================="
), "player", player.getName(), "level", player.getLevel());

// 广播消息
message.broadcast("&e玩家 &b{player} &e加入了游戏！", 
    "player", player.getName());

// 控制台消息
message.sendConsole("&a玩家 {player} 已连接", 
    "player", player.getName());
```

---

## 🎨 高级特性

### 🔄 自动平台检测

```java
public class PlatformSpecificFeature {
    
    private final YLibAPI ylib;
    
    public void initializeFeatures() {
        ServerType serverType = ylib.getServerType();
        
        switch (serverType) {
            case FOLIA:
                setupFoliaFeatures();
                break;
            case PAPER:
                setupPaperFeatures();
                break;
            case SPIGOT:
                setupSpigotFeatures();
                break;
        }
    }
}
```

### 📊 错误统计和监控

```java
public class HealthMonitor implements YLibErrorHandler.ErrorListener {
    
    public void reportHealth() {
        YLibErrorHandler.ErrorStatistics stats = 
            ylib.getErrorHandler().getErrorStatistics();
        
        ylib.getLoggerService().info("=== 插件健康报告 ===");
        ylib.getLoggerService().info("总错误数: " + stats.getTotalErrors());
        ylib.getLoggerService().info("恢复率: " + 
            String.format("%.1f%%", stats.getRecoveryRate() * 100));
    }
}
```

---

## 💡 最佳实践

### 1. 插件结构建议

```
src/main/java/com/yourname/yourplugin/
├── YourPlugin.java                 # 主插件类
├── config/
│   ├── DatabaseConfig.java        # 数据库配置
│   ├── MessagesConfig.java        # 消息配置
│   └── FeatureConfig.java         # 功能配置
├── services/
│   ├── PlayerService.java         # 玩家服务
│   ├── EconomyService.java        # 经济服务
│   └── DatabaseService.java       # 数据库服务
├── commands/
│   ├── MainCommand.java           # 主命令
│   └── AdminCommand.java          # 管理命令
├── listeners/
│   ├── PlayerListener.java        # 玩家事件监听
│   └── WorldListener.java         # 世界事件监听
└── utils/
    ├── PlayerUtils.java           # 玩家工具
    └── MessageUtils.java          # 消息工具
```

### 2. 配置管理最佳实践

```java
// 将相关配置分组
@AutoConfiguration("database")
public class DatabaseConfig {
    // 数据库相关配置
}

@AutoConfiguration("messages")
public class MessagesConfig {
    // 消息相关配置
}

// 在主类中统一注册
@Override
public void onEnable() {
    ylib = YLibFactory.create(this);
    
    // 注册所有配置
    DatabaseConfig dbConfig = ylib.getConfigurationManager()
        .registerConfiguration(DatabaseConfig.class);
    MessagesConfig msgConfig = ylib.getConfigurationManager()
        .registerConfiguration(MessagesConfig.class);
    
    // 注册到服务容器
    ylib.getServiceContainer().registerSingleton(DatabaseConfig.class, dbConfig);
    ylib.getServiceContainer().registerSingleton(MessagesConfig.class, msgConfig);
}
```

### 3. 错误处理最佳实践

```java
public class BestPracticeExample {
    
    public void handlePlayerJoin(Player player) {
        ErrorContext context = new ErrorContext("PlayerManager", "handlePlayerJoin", "MyPlugin")
            .addContext("player", player.getName())
            .addContext("uuid", player.getUniqueId().toString());
        
        try {
            // 加载玩家数据
            PlayerData data = loadPlayerData(player);
            
            // 发送欢迎消息
            sendWelcomeMessage(player, data);
            
        } catch (Exception e) {
            YLibErrorHandler.ErrorHandlingResult result = 
                ylib.getErrorHandler().handleError(e, context);
            
            player.sendMessage("§c" + result.getUserMessage());
            
            if (!result.shouldContinue()) {
                player.kickPlayer("服务器遇到问题，请稍后重试");
            }
        }
    }
}
```

---

## 🎮 示例项目

### 完整的插件示例

```java
public class ExamplePlugin extends JavaPlugin {
    
    private YLibAPI ylib;
    
    @Override
    public void onEnable() {
        try {
            // 初始化YLib
            ylib = YLibFactory.create(this);
            
            // 加载配置
            loadConfigurations();
            
            // 注册服务
            registerServices();
            
            // 设置命令
            setupCommands();
            
            // 注册监听器
            setupListeners();
            
            getLogger().info("插件启动成功！");
            
        } catch (Exception e) {
            ErrorContext context = new ErrorContext("ExamplePlugin", "onEnable", getName());
            ylib.getErrorHandler().handleError(e, context);
            
            getLogger().severe("插件启动失败，禁用插件");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void loadConfigurations() {
        DatabaseConfig dbConfig = ylib.getConfigurationManager()
            .registerConfiguration(DatabaseConfig.class);
        MessagesConfig msgConfig = ylib.getConfigurationManager()
            .registerConfiguration(MessagesConfig.class);
        
        ylib.getServiceContainer().registerSingleton(DatabaseConfig.class, dbConfig);
        ylib.getServiceContainer().registerSingleton(MessagesConfig.class, msgConfig);
    }
    
    private void registerServices() {
        ServiceContainer container = ylib.getServiceContainer();
        
        // 注册数据服务
        container.registerSingleton(DataStorage.class, new FileDataStorage(this));
        
        // 注册经济服务
        container.registerFactory(EconomyService.class, () -> {
            DataStorage storage = container.getRequiredService(DataStorage.class);
            LoggerService logger = container.getRequiredService(LoggerService.class);
            return new EconomyService(storage, logger);
        });
    }
    
    private void setupCommands() {
        ylib.getCommandManager().registerCommands("example",
            new ReloadCommand(ylib),
            new EconomyCommand(ylib),
            new AdminCommand(ylib)
        );
    }
    
    private void setupListeners() {
        getServer().getPluginManager().registerEvents(
            new PlayerListener(ylib), this);
    }
    
    @Override
    public void onDisable() {
        if (ylib != null) {
            ylib.disable(); // 自动清理所有资源
        }
    }
    
    public YLibAPI getYLib() {
        return ylib;
    }
}
```

---

## ❓ 常见问题

### Q: YLib 支持哪些 Minecraft 版本？
A: YLib 支持 Minecraft 1.8.8 到 1.21+ 的所有版本，兼容 Spigot、Paper 和 Folia 平台。

### Q: 如何选择正确的依赖？
A: 根据你的目标平台选择：
- 如果只支持 Spigot: `ylib-spigot`
- 如果只支持 Paper: `ylib-paper`  
- 如果只支持 Folia: `ylib-folia`
- 如果需要支持多平台: 选择最低要求的版本，YLib 会自动适配

### Q: YLib 会影响插件性能吗？
A: YLib 设计时充分考虑了性能，使用轻量级的实现和懒加载机制。对插件性能的影响微乎其微。

### Q: 可以在现有插件中集成 YLib 吗？
A: 可以！YLib 设计为非侵入式，可以逐步集成到现有插件中，不需要重写整个插件。

### Q: 如何调试 YLib 相关问题？
A: 启用调试日志：
```java
ylib.getLoggerService().setDebugEnabled(true);
```

### Q: YLib 的配置文件保存在哪里？
A: 配置文件保存在插件的数据文件夹中，文件名格式为 `{配置类名}.yml`。

---

## 🤝 贡献和支持

### 获取帮助
- 📧 邮件: support@yvmou.cn
- 💬 QQ群: [加入我们的QQ群]
- 🐛 问题反馈: [GitHub Issues](https://github.com/yvmou/YLib/issues)

### 贡献代码
1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 发起 Pull Request

### 许可证
YLib 使用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

---

<div align="center">

**感谢使用 YLib！**

如果这个项目对你有帮助，请给我们一个 ⭐ Star！

[GitHub](https://github.com/yvmou/YLib) | [文档](https://docs.yvmou.cn/ylib) | [示例](https://github.com/yvmou/YLib-Examples)

</div>