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
