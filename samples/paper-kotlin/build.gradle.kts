import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "dev.marulabs"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    // Brings the paper artifact and common-kotlin along.
    implementation("com.github.Maru32768.CommandLib:paper-kotlin:latest.release")
}

kotlin {
    jvmToolchain(21)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    // One relocation covers the platform artifact, common and common-kotlin, which must stay together. The Kotlin
    // standard library is shaded as is, since every Kotlin plugin has to ship it.
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}
tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
