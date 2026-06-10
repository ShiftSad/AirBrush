plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.joml)
    compileOnly(libs.annotations)
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
