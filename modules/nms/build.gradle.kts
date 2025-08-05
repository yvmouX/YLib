plugins {
    id("com.gradleup.shadow") version "8.3.8"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":common"))
    
    // NMS依赖 - 根据版本动态添加
    compileOnly("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    exclude("org/jetbrains/annotations/**")
    exclude("org/intellij/lang/annotations/**")
    
    // 重定位包名以避免冲突
    relocate("cn.yvmou.ylib", "cn.yvmou.ylib.nms")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// NMS版本兼容性配置
val nmsVersions = listOf("1.19.4", "1.20.1", "1.20.2", "1.20.3", "1.20.4")

nmsVersions.forEach { version ->
    tasks.register("buildNms${version.replace(".", "")}") {
        dependsOn("build")
        group = "nms"
        description = "构建NMS ${version}版本"
    }
} 