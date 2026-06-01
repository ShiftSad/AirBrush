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
