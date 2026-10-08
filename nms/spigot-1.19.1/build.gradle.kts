plugins {
    id("commandlib.spigot-nms")
}

// Compiled with Mojang names and remapped to the Spigot names that Spigot and Paper 1.19.1 run with.
spigotNms {
    minecraftVersion.set("1.19.1")
    remapped.set(true)
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly(project(":spigot"))
}
