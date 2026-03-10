# 导航

[命令](/命令.md)

[配置](/配置.md)

[调度器](/调度器.md)

[日志](/日志.md)

# 本地使用

```groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}

repositories {
    mavenLocal()
}

dependencies {
    implementation("com.github.yvmouX:YLib:1.0.0")
}

shadowJar {
    archiveClassifier.set('')
    relocate 'cn.yvmou.ylib', '[包名].libs.cn.yvmou.ylib'
}
```

