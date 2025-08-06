plugins {
    `maven-publish`
    java
}

group = "cn.yvmou"
version = "1.0.0-beta5"

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    java {
        when (project.name) {
            "folia", "paper" -> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            else -> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:23.0.0")
        // 测试依赖
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }

    // 测试平台
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

var buildAll = tasks.register<Jar>("buildAll") {
    //delete(files("$projectDir/target"))

    subprojects.forEach {
        dependsOn(it.tasks.named("build"))
    }

    subprojects.forEach { subproject ->
        from(zipTree(subproject.tasks.named("jar").get().outputs.files.singleFile))
    }

    // 设置输出目录
    destinationDirectory.set(file("$projectDir/target"))

    // 在任务执行时删除所有项目的 build 目录
    doLast {
        delete(layout.buildDirectory)
        subprojects.forEach {
            delete(it.layout.buildDirectory)
        }
        println("✅ 已清理所有子项目的 build 目录")
        println("📦 生成的JAR文件: ${archiveFile.get()}")
        println("📋 Group: ${project.group}")
        println("🏷️  ArtifactId: YLib")
        println("📌 Version: ${project.version}")
    }
}

tasks.named("build") {
    dependsOn(buildAll)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = project.group as String
            artifactId = "YLib"
            version = project.version as String

            artifact(buildAll.get())

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