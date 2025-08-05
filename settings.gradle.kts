rootProject.name = "YLib"

// 多模块项目设置
include(":core")
include(":api")
include(":folia")
include(":spigot")
include(":paper")
include(":nms")
include(":common")

// 设置项目描述
project(":core").projectDir = file("modules/core")
project(":api").projectDir = file("modules/api")
project(":folia").projectDir = file("modules/folia")
project(":spigot").projectDir = file("modules/spigot")
project(":paper").projectDir = file("modules/paper")
project(":nms").projectDir = file("modules/nms")
project(":common").projectDir = file("modules/common")
