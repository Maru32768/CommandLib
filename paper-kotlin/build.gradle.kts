repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The command factories and their tests are the same on every platform.
sourceSets.main {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platform/kotlin"))
}
sourceSets.test {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platformTest/kotlin"))
}

dependencies {
    api(project(":common-kotlin"))
    api(project(":paper"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    testImplementation(project(":paper-testing"))
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
