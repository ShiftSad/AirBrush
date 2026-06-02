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
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName = "AirBrush"
        archiveClassifier = ""
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        downloadPlugins {
            // Lets clients on newer protocol versions join the 1.21.4 dev server.
            modrinth("viaversion", "5.9.1")
        }
        jvmArgs(
            "-Xms2G", "-Xmx2G",
            "-Dcom.mojang.eula.agree=true",
        )
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
