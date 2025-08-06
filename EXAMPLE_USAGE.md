# YLib 使用示例

## 🚀 快速开始示例

### 1. 添加依赖

**Maven**
```xml
<dependency>
    <groupId>cn.yvmou</groupId>
    <artifactId>ylib</artifactId>
    <version>1.0.0-beta4</version>
</dependency>
```

**Gradle**
```gradle
dependencies {
    implementation 'cn.yvmou:ylib:1.0.0-beta4'
}
```

### 2. 基础插件示例

```java
public class MyPlugin extends JavaPlugin {
    
    private YLibAPI ylib;
    
    @Override
    public void onEnable() {
        // 自动检测服务器类型并初始化
        ylib = YLibFactory.create(this);
        
        // 记录启动信息
        ylib.getLoggerService().info("插件启动成功！");
        ylib.getLoggerService().info("服务器类型: " + ylib.getServerType().getDisplayName());
        
        // 注册命令
        ylib.getCommandManager().registerCommands("myplugin", new MyMainCommand());
        
        // 启动定时任务
        ylib.getSchedulerManager().runTaskTimer(20L, 20L, () -> {
            ylib.getLoggerService().info("定时任务执行中...");
        });
    }
    
    @Override
    public void onDisable() {
        if (ylib != null) {
            ylib.getLoggerService().info("插件正在关闭...");
        }
    }
    
    public YLibAPI getYLib() {
        return ylib;
    }
}
```

### 3. 命令示例

```java
@CommandOptions(
    name = "myplugin",
    description = "我的插件主命令",
    usage = "/myplugin [子命令]"
)
public class MyMainCommand implements SubCommand {
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§a欢迎使用我的插件！");
            sender.sendMessage("§e使用 /myplugin help 查看帮助");
            return true;
        }
        
        String subCommand = args[0];
        switch (subCommand) {
            case "info":
                sender.sendMessage("§e插件版本: 1.0.0");
                sender.sendMessage("§e服务器类型: " + getYLib().getServerType().getDisplayName());
                return true;
                
            case "reload":
                // 重载逻辑
                sender.sendMessage("§a重载完成！");
                return true;
                
            case "help":
                sender.sendMessage("§6=== 帮助信息 ===");
                sender.sendMessage("§e/myplugin info - 查看插件信息");
                sender.sendMessage("§e/myplugin reload - 重载插件");
                sender.sendMessage("§e/myplugin help - 显示此帮助");
                return true;
                
            default:
                sender.sendMessage("§c未知子命令: " + subCommand);
                sender.sendMessage("§e使用 /myplugin help 查看帮助");
                return false;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "reload", "help");
        }
        return Collections.emptyList();
    }
    
    private YLibAPI getYLib() {
        return ((MyPlugin) Bukkit.getPluginManager().getPlugin("MyPlugin")).getYLib();
    }
}
```

### 4. 配置示例

```java
@AutoConfiguration("myplugin")
public class MyPluginConfig {
    
    @ConfigValue("database.host")
    private String databaseHost = "localhost";
    
    @ConfigValue(value = "database.port", validation = "range:1024-65535")
    private int databasePort = 3306;
    
    @ConfigValue(value = "messages.welcome", description = "欢迎消息")
    private String welcomeMessage = "欢迎来到服务器！";
    
    @ConfigValue(value = "features.enabled", description = "启用的功能列表")
    private List<String> enabledFeatures = Arrays.asList("feature1", "feature2");
    
    @ConfigValue(value = "security.api-key", sensitive = true)
    private String apiKey = "";
    
    // getters and setters...
    public String getDatabaseHost() { return databaseHost; }
    public int getDatabasePort() { return databasePort; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public List<String> getEnabledFeatures() { return enabledFeatures; }
    public String getApiKey() { return apiKey; }
}
```

### 5. 事件监听器示例

```java
public class MyEventListener implements Listener {
    
    private final YLibAPI ylib;
    
    public MyEventListener(YLibAPI ylib) {
        this.ylib = ylib;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 发送欢迎消息
        ylib.getMessageService().sendMessage(player, 
            ylib.getConfigurationService().getString("messages.welcome", "欢迎！"));
        
        // 记录日志
        ylib.getLoggerService().info("玩家 " + player.getName() + " 加入了服务器");
        
        // 异步处理玩家数据
        ylib.getSchedulerManager().runTaskAsync(() -> {
            // 异步加载玩家数据
            loadPlayerData(player);
            
            // 回到主线程更新UI
            ylib.getSchedulerManager().runTask(() -> {
                updatePlayerUI(player);
            });
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ylib.getLoggerService().info("玩家 " + player.getName() + " 离开了服务器");
    }
    
    private void loadPlayerData(Player player) {
        // 异步加载玩家数据的逻辑
    }
    
    private void updatePlayerUI(Player player) {
        // 更新玩家UI的逻辑
    }
}
```

### 6. 错误处理示例

```java
public class SafeOperation {
    
    private final YLibAPI ylib;
    
    public SafeOperation(YLibAPI ylib) {
        this.ylib = ylib;
    }
    
    public void performRiskyOperation(Player player) {
        try {
            // 可能出错的操作
            riskyOperation(player);
            
        } catch (Exception e) {
            // 使用YLib的错误处理
            ErrorContext context = ErrorContext.builder()
                .operation("performRiskyOperation")
                .plugin(ylib.getPlugin())
                .addContext("player", player.getName())
                .build();
            
            ErrorHandlingResult result = ylib.getErrorHandler().handleError(e, context);
            
            if (result.isRecovered()) {
                player.sendMessage("§a操作已自动恢复");
            } else {
                player.sendMessage("§c操作失败: " + result.getUserMessage());
                
                // 显示恢复建议
                for (String suggestion : result.getSuggestions()) {
                    player.sendMessage("§e建议: " + suggestion);
                }
            }
        }
    }
    
    private void riskyOperation(Player player) {
        // 模拟可能出错的操作
        if (Math.random() < 0.3) {
            throw new RuntimeException("模拟的操作失败");
        }
    }
}
```

## 🎯 关键优势

### 1. 自动平台检测
```java
// 无需手动检测，YLib自动处理
ylib = YLibFactory.create(this);

// 可以检查当前平台
if (ylib.isFolia()) {
    // Folia特定功能
} else if (ylib.isPaper()) {
    // Paper特定功能
} else {
    // Spigot功能
}
```

### 2. 统一API
```java
// 所有平台使用相同的API
Task universalTask = ylib.getSchedulerManager().runTask(() -> {
    // 任务逻辑
});

// 自动使用最适合的调度器
// - Folia: 区域化调度器
// - Paper: Paper调度器  
// - Spigot: Bukkit调度器
```

### 3. 简化依赖管理
```xml
<!-- 只需要一个依赖，无需考虑平台 -->
<dependency>
    <groupId>cn.yvmou</groupId>
    <artifactId>ylib</artifactId>
    <version>1.0.0-beta4</version>
</dependency>
```

## 📦 构建和使用

### 构建统一JAR
```bash
./gradlew buildUnified
```

### 生成的JAR文件
```
build/libs/ylib-1.0.0-beta4.jar
```

### 在插件中使用
1. 将生成的JAR文件添加到你的插件项目中
2. 使用 `YLibFactory.create(this)` 初始化
3. 享受统一的API体验！

---

**YLib** - 让Minecraft插件开发更简单、更高效！ 