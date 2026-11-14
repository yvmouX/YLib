// 装配模块：CoreServices + META-INF/services/cn.yvmou.ylib.YLibServices。
//
// 为什么单独一个模块：CoreServices 同时 new 出 logger / config / command / message 四样东西，
// 它不属于任何一个特性域；塞进其中任何一个都会让那个模块反向依赖其余三个。
//
// 四个依赖都用 implementation（而不是 api）：它们是**内部实现**，不该出现在
// core:bootstrap 的编译传递面上。root 项目也只声明本模块，其余三个靠这里的传递依赖进来。
dependencies {
    api(project(":api"))
    implementation(project(":core:logger"))
    implementation(project(":core:config"))
    implementation(project(":core:command"))
    implementation(project(":core:message"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
}
