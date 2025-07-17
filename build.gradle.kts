plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
}

group = "com.github.yvmouX"
version = "1.0.0-beta1"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    implementation("com.tcoded:FoliaLib:0.5.1")
    compileOnly("dev.folia:folia-api:1.21.6-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    // 生成源码和文档 JAR
    withSourcesJar()
    withJavadocJar()
}

tasks.shadowJar {
    archiveFileName.set("YLib-${version}-all.jar")
    relocate("com.tcoded.folialib", "cn.yvmou.ylib.lib.folialib")
    exclude("org/jetbrains/annotations/**")
    exclude("org/intellij/lang/annotations/**")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = project.group as String
            artifactId = "YLib"
            version = project.version as String

            from(components["java"] as SoftwareComponent)

            artifact(tasks.shadowJar) {
                classifier = "all"
            }

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
