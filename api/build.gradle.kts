dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    // 单测要在类路径上看见 Bukkit 类型才能构造 Material / ItemStack（只有编译期才会去读 Server 实现）
    testImplementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    // 文本渲染：MiniMessage 为主，兼容传统 & / § 颜色码。
    // 用 api 而非 implementation：TextRenderer 的公开签名里就有 Adventure 的 Component，
    // 消费方必须能在编译期解析这些类型，否则二次渲染时会出现版本/类加载错位。
    api("net.kyori:adventure-text-minimessage:4.26.1")
    api("net.kyori:adventure-text-serializer-legacy:4.26.1")
    api("net.kyori:adventure-text-serializer-plain:4.26.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // 假一个调度器来钉住「回调一律回主线程」这条契约（单测里装不上真调度器）；测试期依赖，不落进产物
    testImplementation("org.mockito:mockito-core:5.8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 刻意没有 project(":core")，这是本模块成立的前提，不要加：
    // 本模块是消费方唯一能看见的编译期表面。一旦 api 能引用 core，
    // 公开 API 就会重新开始渗入内部实现（LoggerImpl、CommandDispatcher…），
    // 而消费方的 IDE 补全里也会重新冒出这些东西。
    // 需要暴露新类型时，把它定义在 api 里，让 core 反过来依赖它。
    // 漏加依赖的后果是编译失败（api 单独编译时类路径上没有 core），不会静默发生。
}

tasks.test {
    useJUnitPlatform()
}
