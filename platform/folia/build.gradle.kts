dependencies {
    implementation(project(":api"))
    implementation(project(":common"))

    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT") // Platform-specific API
}