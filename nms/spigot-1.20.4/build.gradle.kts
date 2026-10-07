plugins {
    id("commandlib.spigot-nms")
}

// Compiled with Mojang names and remapped to the Spigot names that Spigot and Paper up to 1.20.4 run with.
spigotNms {
    minecraftVersion.set("1.20.4")
    remapped.set(true)
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly(project(":spigot"))
}
