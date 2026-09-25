// 注解命令的实现：Dispatcher / ManagerImpl / AnnotationParser / config / wrapped。
//
// 命令的配置走 commands.yml，因此依赖 core:config（它也顺带把 core:logger 传递进来）。
dependencies {
    api(project(":api"))
    implementation(project(":core:config"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}
