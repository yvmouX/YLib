dependencies {
    
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// API模块不包含具体实现，只包含接口定义
tasks.jar {
    enabled = true
} 