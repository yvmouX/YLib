plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.0"
}

tasks.shadowJar {
    // 设置输出目录
    destinationDirectory.set(file("${project.rootDir}/example/run/plugins"))
    archiveClassifier.set("")
    // 排除签名文件，避免冲突
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// 注册聚合源码的任务
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    val sources = subprojects.map { it.sourceSets.main.get().allSource }
    from(sources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 注册空的 Javadoc 任务（或聚合任务，视需求而定，这里先给一个空的以免报错）
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // 如果需要聚合 Javadoc，可以在这里配置，目前留空仅作为占位符
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

allprojects {
    apply(plugin = "java-library")

    group = "com.github.yvmouX"
    version = "1.0.0-alpha.8"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    }

    java {
        // 打印调试信息，确认每个项目匹配到了什么版本
        println("Configuring JDK for project: ${project.name} (${project.path})")
        
        when (project.path) {
            // Folia 和 Paper 必须是 Java 17+
            ":platform:folia", ":platform:paper" -> {
                println("  -> Target: Java 17 (Folia/Paper specific)")
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            // 根项目作为容器，必须能容纳所有子模块，所以设为 21
            ":" -> {
                println("  -> Target: Java 21 (Root project container)")
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            // 其他所有模块 (common, core, spigot) 默认为 Java 8，保证最大兼容性
            else -> {
                println("  -> Target: Java 8 (Legacy/Universal)")
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:23.0.0")
    }
}

dependencies {
    api(project(":common"))
    api(project(":core"))
    api(project(":platform"))
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
            
            // 使用上面注册的任务作为 artifact
            artifact(sourcesJar)
            artifact(javadocJar)

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