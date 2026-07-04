plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.joml)
    compileOnly(libs.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.joml)
    testImplementation(libs.annotations)
    testImplementation(libs.adventure.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val packResourcePack by tasks.registering(Zip::class) {
    archiveFileName = "airbrush-resourcepack.zip"
    destinationDirectory = layout.buildDirectory.dir("generated/resourcepack")
    from(rootProject.layout.projectDirectory.dir("resourcepack"))
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.processResources {
    from(packResourcePack)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
