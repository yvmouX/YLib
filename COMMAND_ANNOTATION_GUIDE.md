# 强制使用 @CommandOptions 注解指南

## 概述

从 YLib 1.0.0-beta6 开始，框架强制要求所有子命令必须使用 `@CommandOptions` 注解。如果忘记添加注解，插件将在启动时抛出异常并停止加载，从而提醒开发者及时修复。

## 使用方法

### 1. 基本用法

```java
import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {

    @CommandOptions(
        name = "reload",           // 必填：子命令名称
        permission = "myplugin.reload",  // 可选：权限节点
        onlyPlayer = false,        // 可选：是否仅玩家可执行
        alias = {"rl", "r"},       // 可选：命令别名
        usage = "/plugin reload", // 可选：使用方法
        description = "重新加载插件配置" // 可选：命令描述
    )
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 命令逻辑
        sender.sendMessage("配置已重新加载！");
        return true;
    }
}
```

### 2. 最小配置（仅必填项）

```java
public class HelpCommand implements SubCommand {

    @CommandOptions(name = "help")
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("这是帮助命令");
        return true;
    }
}
```

### 3. 完整配置示例

```java
public class SetWarpCommand implements SubCommand {

    @CommandOptions(
        name = "setwarp",
        permission = "myplugin.warp.set",
        onlyPlayer = true,
        alias = {"sw", "createwarp"},
        usage = "/warp setwarp <名称>",
        description = "设置一个新的传送点",
        register = true
    )
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("用法: /warp setwarp <名称>");
            return false;
        }
        // 设置传送点逻辑
        return true;
    }
}
```

## 注册命令

```java
@Override
public void onEnable() {
    YLib yLib = YLib.getInstance();
    
    // 现在会自动验证注解，如果缺少注解会抛出异常
    yLib.getSimpleCommandManager().registerCommands("myplugin", 
        new ReloadCommand(),
        new HelpCommand(),
        new SetWarpCommand()
    );
}
```

## 常见错误

### 错误 1：忘记添加注解

**错误代码：**
```java
public class ReloadCommand implements SubCommand {
    // 缺少 @CommandOptions 注解
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return true;
    }
}
```

**错误信息：**
```
[YLib] 子命令类 ReloadCommand 必须在其 execute 方法上使用 @CommandOptions 注解
```

### 错误 2：name 属性为空

**错误代码：**
```java
@CommandOptions(name = "") // name 为空
```

**错误信息：**
```
[YLib] 子命令类 ReloadCommand 的 @CommandOptions 注解中的 name 属性不能为空
```

### 错误 3：方法签名错误

**错误代码：**
```java
@CommandOptions(name = "test")
public boolean execute(Player player) { // 缺少 String[] args 参数
    return true;
}
```

**错误信息：**
```
[YLib] 子命令类 TestCommand 必须实现 execute(CommandSender, String[]) 方法
```

## 验证机制

新的验证机制会在以下情况下自动执行：

1. **插件启动时**：当调用 `registerCommands()` 方法时
2. **配置初始化时**：当 `initCommandConfig()` 被调用时
3. **严格检查**：所有验证失败都会抛出 `IllegalStateException`，阻止插件加载

## 向后兼容性

- **破坏性变更**：这是一个破坏性更新，旧版本不强制要求注解的代码需要添加注解
- **迁移步骤**：
  1. 为所有实现了 `SubCommand` 的类添加 `@CommandOptions` 注解
  2. 确保每个注解都有有效的 `name` 属性
  3. 测试插件启动是否正常

## 最佳实践

1. **总是提供有意义的 name**：使用小写字母和描述性名称
2. **合理使用权限**：为管理命令设置适当的权限节点
3. **添加别名**：为常用命令提供简短的别名
4. **完善 usage**：提供清晰的使用说明
5. **使用 description**：帮助玩家理解命令用途

## 示例项目结构

```
src/main/java/com/yourplugin/commands/
├── ReloadCommand.java      // @CommandOptions(name = "reload")
├── HelpCommand.java        // @CommandOptions(name = "help")
├── SetWarpCommand.java     // @CommandOptions(name = "setwarp")
├── DelWarpCommand.java     // @CommandOptions(name = "delwarp")
└── ListWarpCommand.java    // @CommandOptions(name = "listwarp")
```