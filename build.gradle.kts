plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.github.yvmouX"
version = "1.0.0-beta5"

subprojects {
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
        // 生成源码和文档 JAR
//        withSourcesJar()
//        withJavadocJar()
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:23.0.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}




publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = project.group as String
            artifactId = "YLib"
            version = project.version as String

            artifact(tasks.shadowJar)

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

tasks.shadowJar {
    val libs = listOf(
        project(":common"),
        project(":core")
    )

    dependsOn(libs.map { it.tasks.named("jar") })

    libs.forEach { p ->
        val ss = p.extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
        from(ss.getByName("main").output)
    }

    val runtimeConfs = libs.mapNotNull { it.configurations.findByName("runtimeClasspath") }
    if (runtimeConfs.isNotEmpty()) {
        configurations = runtimeConfs
    }

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
tasks.publish {
    dependsOn(tasks.shadowJar)
}
