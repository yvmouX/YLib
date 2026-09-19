# YLib 文档导航

YLib 是一个面向 Minecraft 插件开发的工具库，重点是**一套代码同时跑 Spigot / Paper / Folia / Canvas**。

第一次用建议按这个顺序读：**生命周期 → 调度器 → 命令系统 → 配置 → 多语言 → 菜单**。其余按需查。

| 文档 | 讲什么 | 什么时候看 |
|---|---|---|
| [生命周期](生命周期.md) | 装配顺序、配置的五步加载、重载与关停 | **接入时先看这篇**，尤其搞不清「配置何时生效」时 |
| [命令系统](命令系统.md) | 注解命令、参数类型、权限、`commands.yml`、Builder 模式 | 要写 `/xxx` 命令时 |
| [命令帮助](命令帮助.md) | 统一格式的 help 输出 | 不想手写帮助清单时 |
| [配置](配置.md) | 注解配置、类型映射、注释、验证规则、版本迁移 | 要定义或读配置文件时 |
| [多语言](多语言.md) | 语言文件、占位符、MiniMessage 与传统色码、`TextRenderer` | 要做 i18n，或要渲染任意文本时 |
| [菜单](菜单.md) | 箱子界面框架（布局就是一张文本图） | 要做 GUI 时 |
| [调度器](调度器.md) | Spigot / Paper / Folia / Canvas 统一调度器 | **只要你的插件要在 Folia / Canvas 上跑，先看这篇** |
| [日志](日志.md) | 日志级别、`{}` 占位符、把日志发给某个玩家 | 要打日志或给玩家回执时 |

接入方式（Gradle / Maven、`relocate` 注意事项、Java 版本要求）见仓库根目录的 [README](../README.md)。

---

## 本地使用（自己构建 YLib 时）

先在 YLib 仓库执行一次发布到本地仓库：

```bash
./gradlew publishToMavenLocal
```

然后在你的插件工程里：

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.3.0'
}

repositories {
    mavenLocal()
}

dependencies {
    // 版本号要和 YLib 的 gradle.properties 一致（当前是 1.0.0-beta10）
    implementation 'com.github.yvmouX:YLib:1.0.0-beta10'
}

shadowJar {
    archiveClassifier.set('')
    // YLib 用 ServiceLoader 找平台实现，这一步负责把 META-INF/services 一起重定位
    mergeServiceFiles()
    relocate 'cn.yvmou.ylib', '你的包名.lib.ylib'
}
```

用发布版（不自己构建）时把 `mavenLocal()` 换成 JitPack、版本号换成 release tag，见 [README](../README.md)。

> **`relocate` 别省。** YLib 是进程级单例，服务、命令、配置、任务都绑定在第一个初始化它的插件上。
> 两个插件各打包一份没重定位的 YLib，第二个插件 `YLib.init(this)` 时会直接抛
> `YLibException: YLib has already been initialized by another plugin: ...`。
