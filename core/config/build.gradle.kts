// 注解配置的实现：ConfigurationLoader / ManagerImpl / Metadata / Parser / Validator。
//
// Loader 会在日志里说明「哪个键补了默认值」「迁移到哪个版本」，因此依赖 core:logger。
dependencies {
    api(project(":api"))
    implementation(project(":core:logger"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    // 配置行为必须在本模块内钉住：注释刷新、缺失键补齐、版本迁移三条都靠单测守住。
    // 单测需要在类路径上看见 Bukkit 类型（只有编译期才会去读 Server 实现）。
    testImplementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // 假一个 Plugin / Server 来跑「注释刷新」「重载」这两条真实路径
    testImplementation("org.mockito:mockito-core:5.8.0")
    // ReloadRefreshCommentTest 走 ServiceLocator 找 CoreServices，把「装配 → 注册 → 重载」
    // 整条真实链路跑一遍（而不是自己 new ManagerImpl）。那是 bootstrap 的类，故只在**测试期**依赖它：
    // testImplementation 不进主编译类路径，因此不会构成 config ←→ bootstrap 的循环。
    testImplementation(project(":core:bootstrap"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
