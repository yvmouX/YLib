dependencies {
    api(project(":api"))
    
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// 通用模块包含跨平台共享的功能
tasks.jar {
    enabled = true
} 