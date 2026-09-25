// 日志与消息文本的底层实现：LoggerImpl / LoggerUtil / MessageImpl。
//
// 它是 core 的地基：其余每个 core 子模块都收 Logger 当参数，因此本模块**不允许**依赖
// 任何其它 core 子模块——那会立刻成环。
dependencies {
    // 实现的公开签名里会出现 api 的契约类型（Logger 等），消费方要能解析出这些名字。
    // 与 api 模块相反，core 这边允许（也必须）看得见 api —— 依赖方向只能从 core 指向 api。
    api(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}
