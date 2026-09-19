dependencies {
    // 实现的公开签名里会出现 api 的契约类型（CommandManager 等），消费方要能解析出这些名字。
    // 与 api 模块相反，这里允许（也必须）看得见 api —— 依赖方向只能是从 core 指向 api。
    api(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    // 单测需要在类路径上看见 Bukkit 类型才能调 PlayerInput 的公开方法（只有编译期才会去读 Server 实现）
    testImplementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    // 文本渲染：MiniMessage 为主，兼容传统 & / § 颜色码。
    // Adventure 4.x 全线是 Java 8 字节码（5.x 才需要 Java 21），因此本模块可以继续停留在 Java 8。
    // 这里重新声明是因为 :api 用的是 api(...)：库的使用者若在编译期直接写 MiniMessage，
    // 必须能从本模块解析出这两个类型名，否则会出现版本/类加载错位。
    api("net.kyori:adventure-text-minimessage:4.26.1")
    api("net.kyori:adventure-text-serializer-legacy:4.26.1")
    api("net.kyori:adventure-text-serializer-plain:4.26.1")

    // 文本渲染是库的核心能力，行为必须在库内钉住：
    // 「MiniMessage 遇到 § 会整串放弃解析」「& 与 § 混排」这类事实一旦回归，
    // 所有消费方都会以不同形式踩到，放在消费方测试里只能各测各的。
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // 假一个调度器来钉住「回调一律回主线程」这条契约（单测里装不上真调度器）；测试期依赖，不落进产物
    testImplementation("org.mockito:mockito-core:5.8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
