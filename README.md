# YLib - Minecraft 插件开发通用库

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.7+-green.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

YLib 是一个专为 Minecraft 服务器插件开发设计的通用库，支持多种服务器类型（Folia、Spigot、Paper）的统一API。

## ✨ 特性

- 🎯 **跨平台兼容**: 统一的API支持Folia、Spigot、Paper服务器
- 🏗️ **模块化设计**: 清晰的模块分离，便于维护和扩展
- 🔧 **开箱即用**: 提供调度器、命令管理、配置管理等核心功能
- 🛡️ **类型安全**: 使用Java泛型和注解确保类型安全
- 📚 **完整文档**: 详细的使用文档和示例代码
- 🚀 **快速构建**: 简化的构建流程，专注于功能开发
- 🤖 **自动检测**: 运行时自动检测服务器类型，无需手动选择

## 🚀 快速开始

### 1. 添加依赖

YLib提供统一的JAR文件，自动适配不同服务器类型：

```xml
<dependency>
    <groupId>cn.yvmou</groupId>
    <artifactId>ylib</artifactId>
    <version>1.0.0-beta4</version>
</dependency>
```

### 2. 初始化YLib

使用统一的API，YLib会自动检测服务器类型：

```java
public class MyPlugin extends JavaPlugin {
    private YLibAPI ylib;
    
    @Override
    public void onEnable() {
        // 自动检测服务器类型并初始化
        ylib = YLibFactory.create(this);
        
        // 记录启动日志
        ylib.getLoggerService().info("插件启动成功，使用 " + ylib.getServerType().getDisplayName() + " 平台");
        
        // 注册命令
        ylib.getCommandManager().registerCommands("mylib", new MySubCommand());
    }
    
    @Override
    public void onDisable() {
        ylib.getLoggerService().info("插件已禁用");
    }
}
```

### 3. 使用核心功能

```java
// 调度器 - 自动使用最适合的调度器
Task task = ylib.getSchedulerManager().runTask(() -> {
    getLogger().info("任务执行");
});

// 配置管理
String message = ylib.getConfigurationService().getString("messages.welcome", "欢迎!");

// 日志记录
ylib.getLoggerService().info("插件运行中");

// 检查服务器类型
if (ylib.isFolia()) {
    // Folia特定功能
} else if (ylib.isPaper()) {
    // Paper特定功能
} else {
    // Spigot功能
}
```

## 📖 文档

- [用户指南](YLIB_USER_GUIDE.md) - 完整的使用指南和示例
- [使用示例](EXAMPLE_USAGE.md) - 详细的代码示例和最佳实践

## 🏗️ 项目架构

```
YLib/
├── modules/
│   ├── api/          # API接口定义
│   ├── common/       # 通用功能模块
│   ├── core/         # 核心功能模块
│   ├── folia/        # Folia服务器实现
│   ├── spigot/       # Spigot服务器实现
│   ├── paper/        # Paper服务器实现
│   └── nms/          # NMS兼容性模块（待实现）
```

## 🔧 构建

### 环境要求

- Java 8+ (核心模块)
- Java 17+ (Folia/Paper模块，可选)
- Gradle 8.7+

### 兼容性说明

YLib采用智能兼容性策略：

- **核心模块** (api, common, core, spigot): 使用Java 8，支持最老的Minecraft服务器
- **现代模块** (folia, paper): 使用Java 17，支持最新的服务器特性
- **统一JAR**: 包含所有模块，自动适配不同环境

### 构建命令

```bash
# 构建统一的YLib JAR文件
./gradlew buildUnified

# 构建所有模块（开发用）
./gradlew buildAll

# 构建核心模块
./gradlew buildYLib

# 运行测试
./gradlew test

# 快速构建（跳过测试和Javadoc）
./gradlew build -x test -x javadoc
```

### 生成的JAR文件

构建完成后，在根目录的 `build/libs/` 文件夹中会生成统一的JAR文件：

```
build/libs/ylib-1.0.0-beta4.jar   # 统一的YLib JAR文件
```

这个JAR文件包含了所有平台的实现，会根据运行时的服务器环境自动选择正确的实现。

## 📦 模块说明

| 模块 | 描述 | 状态 |
|------|------|------|
| `api` | API接口定义 | ✅ 完成 |
| `common` | 通用功能模块 | ✅ 完成 |
| `core` | 核心功能模块 | ✅ 完成 |
| `folia` | Folia服务器实现 | ✅ 完成 |
| `spigot` | Spigot服务器实现 | ✅ 完成 |
| `paper` | Paper服务器实现 | ✅ 完成 |
| `nms` | NMS兼容性模块 | 🚧 开发中 |

## 🎯 核心功能

### 自动平台检测
- 运行时自动检测服务器类型
- 无需手动选择或配置
- 支持Folia、Paper、Spigot

### 调度器系统
- 统一的任务调度API
- 支持同步、异步、延迟、重复任务
- 跨平台兼容（Folia区域化调度器）

### 命令管理系统
- 统一的命令注册和管理
- 支持子命令和Tab补全
- 自动参数验证

### 配置管理
- 统一的配置读取和保存
- 支持默认配置
- 类型安全的配置访问

### 日志系统
- 统一的日志记录API
- 支持不同级别的日志
- 彩色日志输出

### 消息系统
- 统一的消息发送API
- 支持占位符替换
- 广播消息功能

## 🚧 开发状态

### 已完成 ✅
- 多模块构建系统
- 核心功能实现
- 调度器系统
- 命令管理系统
- 平台特定实现
- 统一JAR文件生成
- 自动平台检测
- 基础测试和文档

### 进行中 🚧
- NMS兼容性模块
- 高级功能开发
- 更多单元测试
- API文档完善

### 计划中 📋
- 缓存系统
- 性能监控
- 更多示例项目
- Maven发布

## 🤝 贡献

欢迎提交Issue和Pull Request！

### 开发环境设置

1. 克隆仓库
```bash
git clone https://github.com/your-username/YLib.git
cd YLib
```

2. 导入到IDE
3. 运行测试确保环境正常
```bash
./gradlew test
```

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！

---

**YLib** - 让Minecraft插件开发更简单、更高效！