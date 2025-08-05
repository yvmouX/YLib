# YLib 用户指南

## 🚀 快速开始

### 1. 添加依赖

#### Maven
```xml
<dependency>
    <groupId>cn.yvmou</groupId>
    <artifactId>ylib</artifactId>
    <version>1.0.0-beta4</version>
</dependency>
```

#### Gradle
```gradle
dependencies {
    implementation 'cn.yvmou:ylib:1.0.0-beta4'
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
        ylib.getLoggerService().info("插件已启用，使用 " + ylib.getServerType().getDisplayName() + " 平台");

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

## 🎯 核心功能

### 自动平台检测

YLib会自动检测你的服务器类型，无需手动配置：

```java
// 检查服务器类型
if (ylib.isFolia()) {
    // Folia特定功能
    ylib.getLoggerService().info("检测到Folia服务器，启用区域化调度器");
} else if (ylib.isPaper()) {
    // Paper特定功能
    ylib.getLoggerService().info("检测到Paper服务器，启用Paper优化功能");
} else {
    // Spigot功能
    ylib.getLoggerService().info("检测到Spigot服务器");
}

// 获取服务器信息
ServerType serverType = ylib.getServerType();
String serverVersion = ylib.getServerVersion();
String bukkitVersion = ylib.getBukkitVersion();
```

### 调度器系统

统一的调度器API，自动使用最适合的调度器：

```java
// 同步任务
Task syncTask = ylib.getSchedulerManager().runTask(() -> {
    getLogger().info("同步任务执行");
});

// 异步任务
Task asyncTask = ylib.getSchedulerManager().runTaskAsync(() -> {
    getLogger().info("异步任务执行");
});

// 延迟任务
Task delayedTask = ylib.getSchedulerManager().runTaskLater(20L, () -> {
    getLogger().info("延迟任务执行");
});

// 重复任务
Task repeatingTask = ylib.getSchedulerManager().runTaskTimer(20L, 40L, () -> {
    getLogger().info("重复任务执行");
});

// 取消任务
syncTask.cancel();
```

### 命令管理系统

```java
@CommandOptions(
    name = "mylib",
    description = "我的插件主命令",
    usage = "/mylib [子命令]"
)
public class MyMainCommand implements SubCommand {
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§a欢迎使用我的插件！");
            return true;
        }
        
        String subCommand = args[0];
        switch (subCommand) {
            case "info":
                sender.sendMessage("§e插件版本: 1.0.0");
                return true;
            case "reload":
                // 重载逻辑
                sender.sendMessage("§a重载完成！");
                return true;
            default:
                sender.sendMessage("§c未知子命令: " + subCommand);
                return false;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "reload");
        }
        return Collections.emptyList();
    }
}

// 注册命令
ylib.getCommandManager().registerCommands("mylib", new MyMainCommand());
```

### 配置管理

```java
// 基础配置操作
ConfigurationService config = ylib.getConfigurationService();

// 读取配置
String welcomeMessage = config.getString("messages.welcome", "欢迎！");
int maxPlayers = config.getInt("server.max-players", 20);
boolean debugMode = config.getBoolean("debug.enabled", false);

// 保存配置
config.set("messages.welcome", "新的欢迎消息");
config.saveConfig();

// 重载配置
config.reloadConfig();
```

### 日志系统

```java
LoggerService logger = ylib.getLoggerService();

// 不同级别的日志
logger.info("信息日志");
logger.warning("警告日志");
logger.severe("错误日志");
logger.debug("调试日志");

// 彩色日志
logger.info("§a绿色信息");
logger.warning("§e黄色警告");
logger.severe("§c红色错误");
```

### 消息系统

```java
MessageService messageService = ylib.getMessageService();

// 发送消息给玩家
Player player = // 获取玩家
messageService.sendMessage(player, "欢迎来到服务器！");

// 广播消息
messageService.broadcast("§a服务器重启完成！");

// 带占位符的消息
Map<String, String> placeholders = new HashMap<>();
placeholders.put("player", player.getName());
placeholders.put("time", new SimpleDateFormat("HH:mm:ss").format(new Date()));
messageService.sendMessage(player, "§e玩家 {player} 在 {time} 加入了游戏", placeholders);
```

## 🔧 高级功能

### 依赖注入容器

```java
ServiceContainer container = ylib.getServiceContainer();

// 注册服务
container.registerSingleton(MyService.class, new MyServiceImpl());

// 获取服务
MyService service = container.getRequiredService(MyService.class);

// 注册工厂
container.registerFactory(MyService.class, () -> new MyServiceImpl());

// 注册临时服务
container.registerTransient(MyService.class, () -> new MyServiceImpl());
```

### 自动配置系统

```java
@AutoConfiguration("myplugin")
public class MyPluginConfig {
    
    @ConfigValue("database.host")
    private String databaseHost = "localhost";
    
    @ConfigValue(value = "server.port", required = true)
    private int serverPort;
    
    @ConfigValue(value = "features.enabled", description = "启用的功能列表")
    private List<String> enabledFeatures = Arrays.asList("feature1", "feature2");
    
    @ConfigValue(value = "security.password", sensitive = true)
    private String password;
    
    // getters and setters...
}

// 注册配置
ConfigurationManager configManager = ylib.getConfigurationManager();
MyPluginConfig config = configManager.registerConfiguration(MyPluginConfig.class);
```

### 错误处理

```java
YLibErrorHandler errorHandler = ylib.getErrorHandler();

// 处理异常
try {
    // 可能出错的代码
    riskyOperation();
} catch (Exception e) {
    ErrorContext context = ErrorContext.builder()
        .operation("riskyOperation")
        .plugin(plugin)
        .build();
    
    ErrorHandlingResult result = errorHandler.handleError(e, context);
    if (result.isRecovered()) {
        getLogger().info("错误已自动恢复");
    } else {
        getLogger().severe("错误处理失败: " + result.getUserMessage());
    }
}
```

## 📊 错误统计

```java
// 获取错误统计
ErrorStatistics stats = ylib.getErrorHandler().getErrorStatistics();

// 查看统计信息
getLogger().info("总错误数: " + stats.getTotalErrors());
getLogger().info("可恢复错误: " + stats.getRecoverableErrors());
getLogger().info("严重错误: " + stats.getSevereErrors());

// 重置统计
ylib.getErrorHandler().resetErrorStatistics();
```

## 🔄 配置热重载

```java
// 添加配置监听器
ConfigurationManager configManager = ylib.getConfigurationManager();

configManager.addConfigurationListener(MyPluginConfig.class, (oldConfig, newConfig) -> {
    getLogger().info("配置已更新");
    
    // 处理配置变更
    if (!oldConfig.getDatabaseHost().equals(newConfig.getDatabaseHost())) {
        // 重新连接数据库
        reconnectDatabase(newConfig.getDatabaseHost());
    }
});
```

## 🎨 最佳实践

### 1. 插件主类设计

```java
public class MyPlugin extends JavaPlugin {
    
    private YLibAPI ylib;
    private MyPluginConfig config;
    
    @Override
    public void onEnable() {
        try {
            // 初始化YLib
            ylib = YLibFactory.create(this);
            
            // 加载配置
            config = ylib.getConfigurationManager().registerConfiguration(MyPluginConfig.class);
            
            // 注册命令
            setupCommands();
            
            // 注册监听器
            setupListeners();
            
            // 启动任务
            startTasks();
            
            ylib.getLoggerService().info("插件启动成功！");
            
        } catch (Exception e) {
            getLogger().severe("插件启动失败: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (ylib != null) {
            ylib.getLoggerService().info("插件正在关闭...");
        }
    }
    
    private void setupCommands() {
        ylib.getCommandManager().registerCommands("myplugin", new MyMainCommand());
    }
    
    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new MyEventListener(), this);
    }
    
    private void startTasks() {
        // 启动定时任务
        ylib.getSchedulerManager().runTaskTimer(20L, 20L, () -> {
            // 每分钟执行一次
            performMaintenance();
        });
    }
}
```

### 2. 错误处理

```java
public class SafePlugin {
    
    private final YLibAPI ylib;
    
    public SafePlugin(YLibAPI ylib) {
        this.ylib = ylib;
    }
    
    public void performOperation() {
        try {
            // 业务逻辑
            doSomething();
            
        } catch (Exception e) {
            // 使用YLib的错误处理
            ErrorContext context = ErrorContext.builder()
                .operation("performOperation")
                .plugin(ylib.getPlugin())
                .build();
            
            ErrorHandlingResult result = ylib.getErrorHandler().handleError(e, context);
            
            if (!result.isRecovered()) {
                // 记录错误并通知管理员
                ylib.getLoggerService().severe("操作失败: " + result.getUserMessage());
                notifyAdmins("插件出现错误，请检查日志");
            }
        }
    }
}
```

### 3. 配置管理

```java
@AutoConfiguration("myplugin")
public class MyPluginConfig {
    
    @ConfigValue("database.host")
    private String databaseHost = "localhost";
    
    @ConfigValue(value = "database.port", validation = "range:1024-65535")
    private int databasePort = 3306;
    
    @ConfigValue(value = "security.api-key", sensitive = true)
    private String apiKey = "";
    
    @ConfigValue(value = "features.enabled", description = "启用的功能列表")
    private List<String> enabledFeatures = Arrays.asList("feature1", "feature2");
    
    // 验证方法
    public boolean isValid() {
        return databasePort >= 1024 && databasePort <= 65535 
            && !databaseHost.isEmpty();
    }
    
    // getters and setters...
}
```

## 🔧 构建

### 环境要求

- Java 8+
- Gradle 8.7+

## ❓ 常见问题

### Q: 如何检查服务器类型？
A: 使用 `ylib.getServerType()` 或 `ylib.isFolia()`、`ylib.isPaper()`、`ylib.isSpigot()` 方法。

### Q: 如何处理配置变更？
A: 使用 `ConfigurationManager.addConfigurationListener()` 监听配置变更。

### Q: 如何自定义错误处理？
A: 实现 `ErrorListener` 接口并注册到 `YLibErrorHandler`。

### Q: 如何添加自定义服务？
A: 使用 `ServiceContainer.registerSingleton()` 或 `registerFactory()` 注册服务。

### Q: 如何优化性能？
A: 使用异步调度器处理耗时操作，合理使用缓存，避免在主线程执行I/O操作。

## 📚 API参考

### 核心接口

- `YLibAPI` - 主API接口
- `SchedulerManager` - 调度器管理
- `CommandManager` - 命令管理
- `ConfigurationService` - 配置服务
- `LoggerService` - 日志服务
- `MessageService` - 消息服务

### 高级功能

- `ServiceContainer` - 依赖注入容器
- `ConfigurationManager` - 配置管理器
- `YLibErrorHandler` - 错误处理器

### 枚举和异常

- `ServerType` - 服务器类型枚举
- `YLibException` - YLib异常类
- `ErrorSeverity` - 错误严重程度

---

**YLib** - 让Minecraft插件开发更简单、更高效！