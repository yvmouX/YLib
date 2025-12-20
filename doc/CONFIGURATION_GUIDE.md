# YLib 配置系统使用指南

YLib 提供了一个强大且易于使用的配置管理系统，基于注解驱动，支持自动加载、验证、热重载以及**配置版本自动迁移**功能。

## 目录

- [快速开始](#快速开始)
- [核心注解](#核心注解)
  - [@AutoConfiguration](#autoconfiguration)
  - [@ConfigValue](#configvalue)
- [自动备份与迁移](#自动备份与迁移)
- [验证规则](#验证规则)
- [使用 ConfigurationManager](#使用-configurationmanager)
  - [注册配置](#注册配置)
  - [获取配置](#获取配置)
  - [重载配置](#重载配置)
  - [保存配置](#保存配置)
  - [监听变更](#监听变更)

## 快速开始

### 1. 定义配置类

创建一个普通的 Java 类，并添加注解：

```java
import cn.yvmou.ylib.api.config.AutoConfiguration;
import cn.yvmou.ylib.api.config.ConfigValue;

// 定义配置文件名称、版本号和基本信息
@AutoConfiguration(configFile = "database.yml", version = "1.0.0")
public class DatabaseConfig {

    @ConfigValue(value = "host", description = "数据库主机地址")
    private String host = "localhost";

    @ConfigValue(value = "port", validation = "range:1-65535")
    private int port = 3306;

    @ConfigValue(value = "username", required = true)
    private String username;

    @ConfigValue(value = "password", sensitive = true)
    private String password;

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    // ...
}
```

### 2. 注册并使用

在你的插件启动逻辑中注册该配置类：

```java
public class MyPlugin extends JavaPlugin {
    
    private ConfigurationManager configManager;

    @Override
    public void onEnable() {
        // 初始化管理器（通常由 YLib 核心提供）
        // configManager = YLib.getInstance().getConfigurationManager(); 
        
        // 注册配置
        DatabaseConfig dbConfig = configManager.registerConfiguration(DatabaseConfig.class);
        
        // 使用配置
        getLogger().info("Connecting to database at " + dbConfig.getHost());
    }
}
```

## 核心注解

### @AutoConfiguration

标记在类上，用于定义配置文件的元数据。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `value` | 配置组名称，若为空则使用类名（去Config后缀） | `""` |
| `configFile` | 配置文件路径（相对于插件数据文件夹） | **必需** |
| `autoCreate` | 文件不存在时是否自动创建 | `true` |
| `version` | **配置版本号**，用于触发自动迁移（见下文） | `"1.0.0"` |

### @ConfigValue

标记在字段上，用于映射 YAML 配置文件中的值。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `value` | YAML 路径（如 `database.host`） | **必需** |
| `description` | 字段描述，会作为注释写入配置文件 | `""` |
| `required` | 是否为必需字段，若缺失会报错 | `false` |
| `validation` | 验证规则表达式 | `""` |
| `sensitive` | 是否敏感信息（日志中会脱敏显示） | `false` |

## 自动备份与迁移

YLib 内置了智能的配置迁移功能，解决了**更新插件后配置文件缺少新字段**的痛点。

### 工作原理

1.  开发者在 `@AutoConfiguration` 中更新 `version`（例如从 `"1.0.0"` 改为 `"1.1.0"`）。
2.  插件启动时，系统检测到代码中的 `version` 高于文件中的 `config-version`。
3.  系统自动执行以下操作：
    *   **备份**：将旧的配置文件重命名（例如 `config_backup_v1.0.0.yml`）。
    *   **生成**：使用代码中的最新结构（包含新字段和注释）生成新的配置文件。
    *   **迁移**：读取备份文件，将用户修改过的值填入新文件。
    *   **完成**：新文件即拥有了最新的结构，又保留了用户的设置。

### 如何使用

当你需要增加新字段或修改注释时：

1.  在配置类中添加新字段。
2.  修改 `@AutoConfiguration` 的 `version` 版本号。

```java
@AutoConfiguration(configFile = "config.yml", version = "1.1.0") // 升级版本号
public class MainConfig {
    // ... 旧字段 ...

    @ConfigValue(value = "new_feature.enabled", description = "这是新功能的开关")
    private boolean newFeatureEnabled = true; // 新增字段
}
```

下次用户更新插件时，`new_feature.enabled` 会自动出现在他们的配置文件中，且原有配置不会丢失。

## 验证规则

`validation` 属性支持以下规则，多个规则用逗号分隔（如 `"min:10,max:100"`）：

| 规则 | 格式 | 示例 | 说明 |
|------|------|------|------|
| 最小值 | `min:N` | `min:1` | 数值必须大于等于 N |
| 最大值 | `max:N` | `max:100` | 数值必须小于等于 N |
| 范围 | `range:N-M` | `range:1-100` | 数值必须在 N 到 M 之间 |
| 长度 | `length:N` | `length:32` | 字符串长度必须等于 N |
| 正则 | `regex:P` | `regex:^[a-z]+$` | 字符串必须匹配正则表达式 P |
| 枚举 | `enum:A\|B\|C` | `enum:TCP\|UDP` | 值必须是指定列表中的一个 |

## 使用 ConfigurationManager

### 注册配置

```java
MyConfig config = configManager.registerConfiguration(MyConfig.class);
```

注册时会自动：
1. 解析注解元数据。
2. 检查版本号，必要时执行**自动备份与迁移**。
3. 读取配置文件（如果存在）。
4. 如果文件不存在且 `autoCreate=true`，则生成包含默认值和注释的配置文件。
5. 验证配置值的合法性。

### 获取配置

```java
// 如果已注册，直接获取实例
MyConfig config = configManager.getConfiguration(MyConfig.class);

// 获取必需的配置（如果未注册会抛出异常）
MyConfig config = configManager.getRequiredConfiguration(MyConfig.class);
```

### 重载配置

```java
// 重载单个配置
boolean success = configManager.reloadConfiguration(MyConfig.class);

// 重载所有配置
int count = configManager.reloadAllConfigurations();
```

重载操作会重新读取文件，更新实例中的字段值，并触发监听器。

### 保存配置

```java
// 将内存中的对象值保存回文件
configManager.saveConfiguration(MyConfig.class);
```

### 监听变更

当配置被重载时，可以收到通知。系统会提供**变更前**和**变更后**的配置对象供对比：

```java
configManager.addConfigurationListener(MyConfig.class, (oldConfig, newConfig) -> {
    // oldConfig 是重载前的快照
    // newConfig 是重载后的新对象
    
    if (oldConfig.getPort() != newConfig.getPort()) {
        getLogger().info("端口已从 " + oldConfig.getPort() + " 变更为 " + newConfig.getPort());
        // 执行重启服务等操作...
        server.restart(newConfig.getPort());
    }
});
```
