plugins {
    id("commandlib.spigot-nms")
}

// Spigot runs with Mojang names from 26.1, so the module compiles against the server jar as it is.
spigotNms {
    minecraftVersion.set("26.1.2")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly(project(":spigot"))
}
