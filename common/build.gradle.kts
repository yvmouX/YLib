dependencies {
    api(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}

// 通用模块包含跨平台共享的功能
tasks.jar {
    enabled = true
} 