plugins {
    id("commandlib.spigot-nms")
}

// Compiled with Mojang names and remapped to the Spigot names that Spigot 1.20.3 runs with.
spigotNms {
    minecraftVersion.set("1.20.3")
    // BuildTools now resolves 1.20.3 to the 1.20.4 build, so build the last 1.20.3 build.
    buildToolsRevision.set("3961")
    remapped.set(true)
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly(project(":spigot"))
}
