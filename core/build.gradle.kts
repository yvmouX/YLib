dependencies {
    implementation(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    // 文本渲染：MiniMessage 为主，兼容传统 & / § 颜色码。
    // Adventure 4.x 全线是 Java 8 字节码（5.x 才需要 Java 21），因此本模块可以继续停留在 Java 8。
    // 用 api 而非 implementation：聚合 jar 里已包含它们，消费方需要能解析出这两个类型名，
    // 否则消费方编译期看不到 MiniMessage，二次渲染时会出现版本/类加载错位。
    api("net.kyori:adventure-text-minimessage:4.26.1")
    api("net.kyori:adventure-text-serializer-legacy:4.26.1")
    api("net.kyori:adventure-text-serializer-plain:4.26.1")

    // 文本渲染是库的核心能力，行为必须在库内钉住：
    // 「MiniMessage 遇到 § 会整串放弃解析」「& 与 § 混排」这类事实一旦回归，
    // 所有消费方都会以不同形式踩到，放在消费方测试里只能各测各的。
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}