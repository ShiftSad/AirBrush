plugins {
    id("java-library")
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.api.get())
    implementation(project(":common"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    toolchain.vendor = JvmVendorSpec.JETBRAINS
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs(
            "-Xms2G", "-Xmx2G",
            "-Dcom.mojang.eula.agree=true",
            "-XX:+AllowEnhancedClassRedefinition"
        )
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
