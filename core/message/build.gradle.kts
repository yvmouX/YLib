// 多语言消息的实现：MessageFormatter / MessageServiceImpl。
//
// 文本渲染（MiniMessage / 传统色码）住在 api 的 TextRenderer 里，本模块只负责
// 「语言文件 → 取值 → 交给 TextRenderer 渲染」这一段，因此只依赖 core:logger。
dependencies {
    api(project(":api"))
    implementation(project(":core:logger"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

    testImplementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
