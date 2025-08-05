plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
    checkstyle
    pmd
}

group = "cn.yvmou"
version = "1.0.0-beta4"

// 所有子项目的通用配置
allprojects {
    group = rootProject.group
    version = rootProject.version
    
    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "spigot-repo"
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
    }
}

// 子项目配置
subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        withSourcesJar()
        withJavadocJar()
    }
    
    // 代码质量检查配置
    checkstyle {
        toolVersion = "10.12.5"
    }
    
    pmd {
        toolVersion = "6.55.0"
    }
    
    // 测试配置
    tasks.test {
        useJUnitPlatform()
    }
    
    // 发布配置
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = project.group as String
                artifactId = "ylib-${project.name}"
                version = project.version as String
                
                from(components["java"])
                
                pom {
                    name.set("YLib-${project.name}")
                    description.set("YLib ${project.name} module")
                    url.set("https://github.com/yvmouX/YLib")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("yvmouX")
                            name.set("yvmouX")
                            email.set("yvmou@outlook.com")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:https://github.com/yvmouX/YLib.git")
                        developerConnection.set("scm:git:git@github.com:yvmouX/YLib.git")
                        url.set("https://github.com/yvmouX/YLib")
                    }
                }
            }
        }
    }
}

// 根项目特定配置
dependencies {
    // 根项目不包含具体实现，只包含文档和构建脚本
}

// 创建统一的构建任务
tasks.register("buildYLib") {
    dependsOn(":api:build", ":common:build", ":core:build")
    group = "build"
    description = "构建YLib核心模块"
}

// 创建平台特定的构建任务
tasks.register("buildFolia") {
    dependsOn("buildYLib", ":folia:build")
    group = "build"
    description = "构建Folia版本"
}

tasks.register("buildSpigot") {
    dependsOn("buildYLib", ":spigot:build")
    group = "build"
    description = "构建Spigot版本"
}

tasks.register("buildPaper") {
    dependsOn("buildYLib", ":paper:build")
    group = "build"
    description = "构建Paper版本"
}

tasks.register("buildAll") {
    dependsOn("buildFolia", "buildSpigot", "buildPaper")
    group = "build"
    description = "构建所有平台版本"
}

// 创建统一的JAR任务
tasks.register("createUnifiedJar") {
    dependsOn("buildAll")
    group = "build"
    description = "创建统一的YLib JAR文件"
    
    doLast {
        println("统一JAR文件创建完成")
        println("请使用以下命令来构建特定平台的JAR文件:")
        println("  ./gradlew :folia:shadowJar    # Folia版本")
        println("  ./gradlew :spigot:shadowJar   # Spigot版本")
        println("  ./gradlew :paper:shadowJar    # Paper版本")
    }
}

// 发布任务
tasks.register("publishFolia") {
    dependsOn(":folia:publish")
    group = "publishing"
    description = "发布Folia版本"
}

tasks.register("publishSpigot") {
    dependsOn(":spigot:publish")
    group = "publishing"
    description = "发布Spigot版本"
}

tasks.register("publishPaper") {
    dependsOn(":paper:publish")
    group = "publishing"
    description = "发布Paper版本"
}

tasks.register("publishAll") {
    dependsOn("publishFolia", "publishSpigot", "publishPaper")
    group = "publishing"
    description = "发布所有平台版本"
}
