plugins {
    id("commandlib.spigot-nms")
}

// Compiled with Mojang names and remapped to the Spigot names that Spigot 1.21.1 runs with. Paper runs with Mojang
// names from 1.20.5 and uses the paper module.
spigotNms {
    minecraftVersion.set("1.21.1")
    remapped.set(true)
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly(project(":spigot"))
}
