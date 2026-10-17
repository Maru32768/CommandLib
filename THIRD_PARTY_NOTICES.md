# Third-party dependencies and distribution

CommandLib's original code is licensed under the [MIT License](LICENSE). This document is an inventory and distribution policy, not a replacement for upstream licenses or a grant of rights to upstream code. Listing a dependency here does not mean that it is bundled. Versions below are the declared versions; dependency conflict resolution may select other versions in a consumer's build.

## Published library dependencies

| Dependency | Declared version | Upstream license | Use / packaging |
| --- | --- | --- | --- |
| [Brigadier](https://github.com/Mojang/brigadier/blob/master/LICENSE) | 1.0.18 | MIT | Provided by the platform for library modules; an external `api` dependency of common-testing. |
| [JetBrains annotations](https://github.com/JetBrains/java-annotations) | 16.0.2 | Apache-2.0 | compileOnlyApi; not bundled. |
| [Log4j Core](https://github.com/apache/logging-log4j2/blob/rel/2.17.1/LICENSE.txt) | 2.17.1 | Apache-2.0 | compileOnly and repository tests; provided by the server, not bundled. |
| [Bukkit](https://github.com/Bukkit/Bukkit/blob/master/LICENCE.txt) / [Spigot](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/LICENCE.txt) | API 1.16.5-R0.1-SNAPSHOT; typed NMS targets in nms/ | GPL-3.0 family; check the exact upstream revision and notices | Compile-time platform; server/API classes are not bundled. |
| [Paper](https://github.com/PaperMC/Paper/blob/main/LICENSE.md) | API 1.21-R0.1-SNAPSHOT; dev bundles 1.20.6 and 1.21.1 | Inherited GPL terms, with some contributions additionally MIT | Compile-time platform; API/server/dev bundles are not bundled. |
| [Velocity](https://github.com/PaperMC/Velocity) | API 3.4.0 (with Velocity's Brigadier fork) | GPL-3.0 for the proxy, MIT for the API; check the exact upstream revision | Compile-time platform for velocity and velocity-testing; not bundled. |
| [Minecraft Forge](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/LICENSE.txt) | 1.16.5-36.2.42; 1.18.2-40.3.0; 1.19.2-43.5.0; 1.20.1-47.4.10 | LGPL-2.1, with upstream exceptions and separate third-party terms | External platform for modded and modded-testing; not bundled. |
| [NeoForge](https://github.com/neoforged/NeoForge/blob/1.21.1/LICENSE.txt) | 21.1.256 for Minecraft 1.21.1; 21.4.161 for 1.21.4 | LGPL-2.1, plus upstream third-party terms | External platform; not bundled. |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader/blob/master/LICENSE) / [Fabric API](https://github.com/FabricMC/fabric-api/blob/1.21.1/LICENSE) | Loader 0.16.14; API 0.92.12+1.20.1, 0.116.17+1.21.1 and 0.119.4+1.21.4 | Apache-2.0 | External platform for the Fabric nodes of modded and modded-testing; not bundled. |
| [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api/blob/master/LICENSE.txt) | 0.3.1 | MIT | compileOnly for the Fabric nodes of modded; used at runtime only when a mod ships it; not bundled. |
| [Minecraft and official mappings](https://www.minecraft.net/en-us/eula) | 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.4 for modded; additional NMS targets | Minecraft EULA and the terms supplied with the exact mappings | Build/runtime platform; game classes, assets and mapping files are not distribution inputs. |
| [Kotlin standard library](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) | 2.1.21 (compiled with Kotlin 2.3.20) | Apache-2.0 | External dependency of the `*-kotlin` modules; not bundled. |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) | 1.8.0 | Apache-2.0 | compileOnly for the `*-kotlin` modules; supplied by the plugin that uses the coroutine functions. |
| [Mockito](https://github.com/mockito/mockito/blob/main/LICENSE) | inline/core 4.8.1 for spigot-testing; core 5.11.0 for paper-testing, velocity-testing and modded-testing | MIT | External `api` dependencies of published testing modules; not embedded. |
| [Byte Buddy](https://github.com/raphw/byte-buddy) and byte-buddy-agent | 1.12.16 via Mockito 4.8.1; 1.14.12 via Mockito 5.11.0 | Apache-2.0; bundled ASM has its own notices | Mockito transitive dependencies; retain upstream archive notices when redistributing. |
| [Objenesis](https://github.com/objenesis/objenesis) | 3.2 via Mockito 4.8.1; 3.3 via Mockito 5.11.0 | Apache-2.0 | Mockito transitive runtime dependency; not embedded. |

Paper and Velocity modules also reference Adventure (MIT), and NMS/modded code references platform-provided Gson (Apache-2.0). These arrive through platform classpaths rather than standalone embedded library dependencies. Their selected versions must be read from the resolved classpath for the target server.

The spigot artifact embeds only CommandLib's own typed NMS adapter classes from `dev/marulabs/commandlib/nms/**`. The typed NMS configuration is non-transitive. Compiling or remapping those adapters does not authorize redistribution of upstream server code or mappings.

## Repository-only tools and tests

These are not runtime dependencies of the published library solely because they are used to build or test it:

- JUnit Jupiter 5.8.1 / 5.10.2 and JUnit Platform: EPL-2.0.
- AssertJ 3.25.1 / 3.25.3, Awaitility 4.2.1, Error Prone 2.50.0 and NullAway 0.14.2: Apache-2.0.
- Testcontainers 1.21.4 and Adventure serializers 4.15.0: MIT; SLF4J Simple 2.0.17: MIT.
- JMH 1.37 (`benchmark`): GPL-2.0 with the Classpath Exception.
- MCProtocolLib (`com.github.steveice10:mcprotocollib` and `org.geysermc.mcprotocollib:protocol`, the integration-test
  bots; versions per target): MIT.
- Gradle, Kotlin, Spotless, the Eclipse formatter, japicmp, Stonecutter, Architectury Loom, paperweight, SpecialSource, BuildTools and the sample Shadow/ForgeGradle plugins are build tooling. This inventory does not certify all tooling transitives. Inspect each tool's own license before redistributing the tool, its caches or a development image.
- Sample and integration-test artifacts are separate applications; their resolved/shaded dependencies require a separate distribution review.

Using an unmodified test/build tool does not itself relicense CommandLib's output. Copying tool code, generated third-party code, runtime components or resources into an output requires a separate check.

## GPL platform integration

MIT is GPL-compatible, but `compileOnly` and the absence of bundled GPL classes do not establish that every linked distribution is outside the GPL. CommandLib does not promise an exemption from Bukkit, Spigot or Paper licensing. The MIT permission covers the authors' original CommandLib code; it does not relicense a combined work or override upstream obligations.

For a distribution treated as a GPL-covered combined work, comply with the applicable GPL version, including notices, the license text and corresponding-source requirements for that work. Retain MIT notices for CommandLib's original code. Do not describe such a combined distribution as exclusively MIT. A source jar containing only CommandLib sources must not be represented as necessarily satisfying all corresponding-source obligations of a larger work.

Whether a particular plugin/library distribution constitutes a combined work, and whether an upstream exception applies, depends on its code, linkage, exact upstream terms and applicable law. Obtain a qualified review or permission from the relevant rightsholders before making a claim that a proprietary or MIT-only combined distribution is exempt. Documentation here does not resolve that legal determination. See the [GNU linking FAQ](https://www.gnu.org/licenses/gpl-faq.en.html#GPLStaticVsDynamic).

Forge explicitly explains that ordinary Java references to its libraries do not bind mod code to Forge's license. Retain the separation: copying or bundling Forge/NeoForge code invokes additional obligations under their licenses.

## Redistributing or shading dependencies

The normal library jars contain CommandLib code, LICENSE and this inventory. External Maven dependencies remain separate; their original archives carry their own licenses. This file is not a substitute for those licenses.

If a downstream build embeds Brigadier or Mockito, preserve their actual copyright and MIT permission notices. If it embeds Apache-licensed components, include the Apache-2.0 license and applicable upstream NOTICE/attribution files, and identify modifications as required. Byte Buddy's embedded ASM notices must also survive shading. Preserve separate filenames under META-INF/licenses/<component>/ so that duplicate LICENSE or NOTICE entries are not silently dropped. Do not embed server/platform jars as a shortcut.

Do not redistribute Minecraft game/server classes, assets, development bundles, mapping files, patched server jars or build caches as part of CommandLib releases. Consult the exact upstream terms before any separate redistribution. Permission to use mappings for a build is not a blanket permission to redistribute the mapping dataset. Minecraft's EULA and usage guidelines also apply independently of the library's MIT license, including to monetization decisions.

## Resolved dependency inventory

Run `./gradlew -I tools/license-audit.init.gradle licenseDependencyReport` to write `build/reports/licenses/dependencies.json`. It records selected external module versions and unresolved dependencies for each published project's compile/runtime configuration. The task fails when any dependency is unresolved. Review licenses against these selected versions; this metadata report does not infer licenses from names or certify them. NMS build classpaths, tooling and separate sample builds are outside its scope. For a focused inventory without configuring typed NMS projects, add `-Pcommandlib.typedNms=false`; do not use that switch to produce a spigot release.

## Release verification

1. Resolve compileClasspath and runtimeClasspath for every published module and supported loader node. Record selected versions and configuration names, including transitives; review new or changed components against their actual versioned license/NOTICE files.
2. Build all publishable jars with every typed NMS module enabled. A build using `-Pcommandlib.typedNms=false` is a focused check only, not a releasable spigot artifact.
3. Inspect binary, sources, javadoc and remapped jars: LICENSE must match the root MIT text, and THIRD_PARTY_NOTICES.md must be included. Verify the contents, not only the POM license metadata.
4. Verify that binaries contain only CommandLib classes and intended resources, with no upstream server classes, mapping datasets, nested platform jars or game assets. Review adapters for copied upstream implementation code as well; a package-name check alone cannot establish provenance.
5. Review any fat jars separately and preserve all upstream notices. Publish source/build instructions matching the release and satisfy any applicable corresponding-source requirements.

The initial audit inspected build declarations, locally cached POMs and existing six Bukkit-side module jars. Some existing testing jars used the old CommandLib package, so they were not evidence of a complete current release. This inventory is not a complete SBOM of every platform, build-tool transitive or server dependency, and it does not claim that a legal determination about GPL linkage has been obtained.
