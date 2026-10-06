plugins {
    id("dev.architectury.loom") version "1.17.493"
}

val minecraftVersion = sc.current.version
val loader = property("loom.platform").toString()
val javaVersion = when {
    sc.current.parsed >= "1.20.5" -> 21
    sc.current.parsed >= "1.17" -> 17
    else -> 11
}

stonecutter {
    constants.match(loader, "forge", "neoforge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    api(project(":common"))
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${property("deps.neoforge")}")
    } else {
        "forge"("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")
    }
}
