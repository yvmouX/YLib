// 日志的底层实现：LoggerImpl / LoggerUtil。
//
// 它是 core 的地基：其余每个 core 子模块都收 Logger 当参数，因此本模块**不允许**依赖
// 任何其它 core 子模块——那会立刻成环。
dependencies {
    // 实现的公开签名里会出现 api 的契约类型（Logger 等），消费方要能解析出这些名字。
    // 与 api 模块相反，core 这边允许（也必须）看得见 api —— 依赖方向只能从 core 指向 api。
    api(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    // 日志是库的地基，它的行为必须在本模块内钉住：级别映射、色码剥离、{} 占位符、
    // 异常独立成条，以及「玩家提示不写控制台」。这些一旦回归，每个消费方都会踩到。
    // 单测需要在类路径上看见 Bukkit 类型（只有编译期才会去读 Server 实现）。
    testImplementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
