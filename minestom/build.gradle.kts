plugins {
    id("java")
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.minestom)
    implementation(libs.joml)
}

java {
    // Minestom 2026.x requires Java 25 — this module is an independent 26.x server,
    // unaffected by the Paper backport to 1.21.4 (which runs on Java 21).
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    toolchain.vendor = JvmVendorSpec.JETBRAINS
}

val mainClassName = "br.com.vrosa.airbrush.minestom.AirBrushServer"

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier = ""
        mergeServiceFiles()
        manifest {
            attributes["Main-Class"] = mainClassName
        }
    }

    register<JavaExec>("run") {
        group = "application"
        mainClass = mainClassName
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = layout.projectDirectory.dir("run").asFile
        doFirst { workingDir.mkdirs() }
    }
}
