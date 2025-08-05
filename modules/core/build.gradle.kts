dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// 核心模块包含通用功能和工具类
tasks.jar {
    enabled = true
} 