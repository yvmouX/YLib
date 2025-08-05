plugins {
    id("com.gradleup.shadow") version "8.3.8"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":core"))
    
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// Paper模块包含Paper特定的实现
tasks.jar {
    enabled = true
}

// Shadow JAR配置
tasks.shadowJar {
    archiveBaseName.set("ylib")
    archiveClassifier.set("")
    archiveVersion.set(project.version as String)
    
    // 重定位包名以避免冲突
    relocate("cn.yvmou.ylib", "cn.yvmou.ylib.internal")
    
    // 排除重复的文件
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
} 