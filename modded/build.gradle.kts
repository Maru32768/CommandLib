plugins {
    id("dev.architectury.loom") version "1.17.493"
}

val minecraftVersion = sc.current.version
val javaVersion = if (sc.current.parsed >= "1.17") 17 else 11

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

dependencies {
    api(project(":common"))
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")
}
