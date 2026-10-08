package commandlib.nms

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The local Maven repository, found the way Maven and Gradle's `mavenLocal()` find it: the `maven.repo.local` system
 * property, then `<localRepository>` in `~/.m2/settings.xml`, then in `$M2_HOME/conf/settings.xml`, then
 * `~/.m2/repository`. BuildTools installs Spigot there and the typed NMS modules read it back.
 */
fun mavenLocalRepository(): File {
    System.getProperty("maven.repo.local")?.takeIf { it.isNotBlank() }?.let { return File(it) }
    val userHome = System.getProperty("user.home")
    val settingsFiles = listOfNotNull(
        File(userHome, ".m2/settings.xml"),
        System.getenv("M2_HOME")?.let { File(it, "conf/settings.xml") },
    )
    for (settings in settingsFiles) {
        localRepository(settings)?.let { return File(it) }
    }
    return File(userHome, ".m2/repository")
}

private fun localRepository(settings: File): String? {
    if (!settings.isFile) {
        return null
    }
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    val document = factory.newDocumentBuilder().parse(settings)
    val value = document.documentElement
        .getElementsByTagName("localRepository")
        .takeIf { it.length > 0 }
        ?.item(0)
        ?.textContent
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return Regex("""\$\{(env\.)?([^}]+)}""").replace(value) { match ->
        val name = match.groupValues[2]
        val resolved = if (match.groupValues[1].isNotEmpty()) System.getenv(name) else System.getProperty(name)
        resolved ?: match.value
    }
}
