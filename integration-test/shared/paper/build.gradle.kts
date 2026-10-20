plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":paper"))
    compileOnly(project(":integration-test:shared:core"))
    compileOnly(libs.paper.api)
    compileOnly(libs.brigadier)
}

// The fixture classes the Spigot and Paper suites share.
sourceSets.main {
    java.srcDir("../bukkit/src/main/java")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects.
tasks.register("prepareKotlinBuildScriptModel")
