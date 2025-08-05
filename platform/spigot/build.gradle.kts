dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":core"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT") // Platform-specific API
}

tasks.jar {
    enabled = true
}