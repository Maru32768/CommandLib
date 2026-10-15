# Javadoc

Every published module ships a javadoc jar and a sources jar next to its main jar, so an IDE that downloads sources
or documentation for dependencies shows the Javadoc without anything else. The Kotlin DSL modules (`*-kotlin`) have
an empty javadoc jar; their KDoc is in the sources jar.

## Browsing on JitPack

JitPack serves the Javadoc of each module and version at

```text
https://jitpack.io/com/github/Maru32768/CommandLib/<module>/<version>/javadoc/
```

JitPack generates the page the first time it is requested, so the first load can take a while. Replace `<version>`
with a release such as `0.17.2`:

| Module | Javadoc |
|---|---|
| `common` | [common](https://jitpack.io/com/github/Maru32768/CommandLib/common/0.17.2/javadoc/) |
| `spigot` | [spigot](https://jitpack.io/com/github/Maru32768/CommandLib/spigot/0.17.2/javadoc/) |
| `paper` | [paper](https://jitpack.io/com/github/Maru32768/CommandLib/paper/0.17.2/javadoc/) |
| `velocity` | [velocity](https://jitpack.io/com/github/Maru32768/CommandLib/velocity/0.17.2/javadoc/) |
| `common-testing` | [common-testing](https://jitpack.io/com/github/Maru32768/CommandLib/common-testing/0.17.2/javadoc/) |
| `spigot-testing` | [spigot-testing](https://jitpack.io/com/github/Maru32768/CommandLib/spigot-testing/0.17.2/javadoc/) |
| `paper-testing` | [paper-testing](https://jitpack.io/com/github/Maru32768/CommandLib/paper-testing/0.17.2/javadoc/) |
| `velocity-testing` | [velocity-testing](https://jitpack.io/com/github/Maru32768/CommandLib/velocity-testing/0.17.2/javadoc/) |

The Forge, NeoForge and Fabric modules use the artifact names of the [packaging](packaging.md) page, such as
`forge-1.20.1` and `fabric-testing-1.21.1`.

## Building locally

```bash
./gradlew :spigot:javadoc
```

writes the Javadoc of a module to `<module>/build/docs/javadoc`. It runs doclint, so broken Javadoc fails the build.
