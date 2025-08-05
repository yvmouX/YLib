dependencies {

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}

// API模块不包含具体实现，只包含接口定义
tasks.jar {
    enabled = true
} 