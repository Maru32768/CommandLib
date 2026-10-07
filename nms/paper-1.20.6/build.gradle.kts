plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

// Paper runs with Mojang names from 1.20.5, so the paperweight dev bundle compiles against them and the jar is
// bundled into :spigot unchanged.
repositories {
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly(project(":spigot"))
}

configurations.create("nmsElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("nmsElements", tasks.jar)
}
