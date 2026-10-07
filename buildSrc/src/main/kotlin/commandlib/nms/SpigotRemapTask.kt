package commandlib.nms

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Remaps a jar with [SpecialSource](https://github.com/md-5/SpecialSource), as the `specialsource-maven-plugin`
 * does for Spigot plugins. The inheritance jars let SpecialSource remap members that the input jar inherits from
 * server classes.
 */
@CacheableTask
abstract class SpigotRemapTask : DefaultTask() {
    @get:Classpath
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mappingFile: RegularFileProperty

    @get:Classpath
    abstract val inheritanceJars: ConfigurableFileCollection

    @get:Input
    abstract val reverse: Property<Boolean>

    @TaskAction
    fun remap() {
        val mapping = JarMapping()
        mapping.loadMappings(mappingFile.get().asFile.absolutePath, reverse.get(), false, null, null)

        val provider = JointProvider()
        val inheritance = inheritanceJars.files
            .filter { it.isFile && it.name.endsWith(".jar") }
            .map { Jar.init(it) }
        try {
            Jar.init(inputJar.get().asFile).use { input ->
                provider.add(JarProvider(input))
                inheritance.forEach { provider.add(JarProvider(it)) }
                mapping.setFallbackInheritanceProvider(provider)
                JarRemapper(null, mapping, null).remapJar(input, outputJar.get().asFile)
            }
        } finally {
            inheritance.forEach(Jar::close)
        }
    }
}
