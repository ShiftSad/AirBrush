plugins {
    id("java-library")
}

dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.joml)
    compileOnly(libs.annotations)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    toolchain.vendor = JvmVendorSpec.JETBRAINS
}

val packResourcePack by tasks.registering(Zip::class) {
    archiveFileName = "witchcraft-resourcepack.zip"
    destinationDirectory = layout.buildDirectory.dir("generated/resourcepack")
    from(rootProject.layout.projectDirectory.dir("resourcepack"))
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.processResources {
    from(packResourcePack)
}
