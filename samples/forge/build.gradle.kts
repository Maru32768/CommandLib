import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.36"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "dev.marulabs"
version = "1.0.0"

base.archivesName.set("samplemod")

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

configure<net.minecraftforge.gradle.userdev.UserDevExtension> {
    mappings("snapshot", "20210309-1.16.5")

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("samplemod") {
                    source(sourceSets["main"])
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("samplemod") {
                    source(sourceSets["main"])
                }
            }
        }

        create("data") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args(
                "--mod",
                "samplemod",
                "--all",
                "--output",
                file("src/generated/resources/"),
                "--existing",
                file("src/main/resources/")
            )
            mods {
                create("samplemod") {
                    source(sourceSets["main"])
                }
            }
        }
    }
}

sourceSets["main"].resources.srcDir("src/generated/resources")

// -PcommandlibLocal=true builds against this repository's code instead of the jitpack release. Publish it first with
// ./gradlew -Pcommandlib.typedNms=false :common:publishToMavenLocal :modded:forge-1.16.5:publishToMavenLocal
// at the repository root. The repository build needs Gradle 9, which ForgeGradle 6 does not run on, so this sample
// cannot include it as a composite build like the Spigot and Paper samples do.
val commandLibLocal = providers.gradleProperty("commandlibLocal").orNull.toBoolean()
val commandLibGroup = if (commandLibLocal) "dev.marulabs" else "com.github.Maru32768.CommandLib"

repositories {
    if (commandLibLocal) {
        mavenLocal()
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    "minecraft"("net.minecraftforge:forge:1.16.5-36.2.20")
    implementation("$commandLibGroup:forge-1.16.5:latest.release")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to "samplemod",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .format(Date())
            )
        )
    }
    finalizedBy("reobfJar")
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    dependencies {
        include(dependency("$commandLibGroup:forge-1.16.5:.*"))
    }
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
    finalizedBy("reobfShadowJar")
}

@Suppress("UNCHECKED_CAST")
(extensions.getByName("reobf") as NamedDomainObjectContainer<Any>).create("shadowJar")
