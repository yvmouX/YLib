plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
}

group = "com.github.yvmouX"
version = "1.0.0-beta4"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // 生成源码和文档 JAR
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    exclude("org/jetbrains/annotations/**")
    exclude("org/intellij/lang/annotations/**")
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

tasks.build {
    dependsOn(tasks.shadowJar)
}
tasks.publish {
    dependsOn(tasks.shadowJar)
}
