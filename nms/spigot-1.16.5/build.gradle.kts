plugins {
    id("commandlib.spigot-nms")
}

// 1.16.5 is the last version whose Spigot-mapped server jar names members readably, so it compiles against the
// plain server jar and needs no remapping.
spigotNms {
    minecraftVersion.set("1.16.5")
}

dependencies {
    compileOnly(project(":spigot"))
}
