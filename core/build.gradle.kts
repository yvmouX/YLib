dependencies {
    implementation(project(":api"))
    implementation(project(":common"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}

// 核心模块包含通用功能和工具类
tasks.jar {
    enabled = true
} 