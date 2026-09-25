rootProject.name = "YLib"

include("api")
// core 按特性分模块（与 platform/ 同构）。依赖方向见各模块的 build.gradle.kts：
//   logger ← config ← command
//   logger ← message
//   (上面四个) ← bootstrap
include("core:logger")
include("core:config")
include("core:command")
include("core:message")
include("core:bootstrap")
include("platform:canvas")
include("platform:folia")
include("platform:spigot")
include("platform:paper")
