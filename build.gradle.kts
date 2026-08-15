plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.0"
}

tasks.shadowJar {
    // 设置输出目录
    //destinationDirectory.set(file("${project.rootDir}/example/run/plugins"))
    archiveClassifier.set("")
    // 合并各模块的 META-INF/services 文件，保证 ServiceLoader 在聚合 jar 中正常工作
    mergeServiceFiles()
    // 排除签名文件，避免冲突
    //exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// 根项目没有源码，其 jar 与 shadowJar 同名输出会引发发布任务的隐式依赖校验错误，直接禁用
tasks.jar {
    enabled = false
}

// 注册聚合源码的任务
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    val sources = subprojects.map { it.sourceSets.main.get().allSource }
    from(sources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

allprojects {
    apply(plugin = "java-library")

    group = "com.github.yvmouX"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    }

    java {
        when (project.path) {
            // Folia 和 Paper 必须是 Java 17+
            ":platform:folia", ":platform:paper" -> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            // 根项目作为容器，必须能容纳所有子模块，所以设为 21
            ":" -> {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            // 其他所有模块默认为 Java 8，保证最大兼容性
            else -> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }

    // 保留方法参数名，供反射注入（@Arg 未显式指定参数名时回退到参数名匹配）
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:23.0.0")
    }
}

dependencies {
    api(project(":api"))
    api(project(":core"))
    api(project(":platform:folia"))
    api(project(":platform:spigot"))
    api(project(":platform:paper"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = project.group as String
            artifactId = "YLib"
            version = project.version as String

            artifact(tasks.shadowJar)

            artifact(sourcesJar)

            pom {
                name.set("YLib")
                description.set("A Minecraft library for Folia servers (Spigot/Paper compatible)")
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
