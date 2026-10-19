# ponytail3-B-tests: test, fixture, build-script and docs changes in 02fbf5f9..HEAD (477e21fa)

Result: nothing here needs fixing.

## What was checked (with evidence)

1. **paper ReplaceChildTest#replacesTheMirroredRootsMapsInsteadOfChangingThem can fail.** I restored the pre-fix
   behavior with a throwaway mutation: `requireNonNull(findPluginCommandNode())` in `PaperInternals.find()`, so a
   missing PluginCommandNode drops all of PaperInternals again. The test then failed with
   `java.util.ConcurrentModificationException` (3 tests run, 1 failed). On HEAD it passes. The mutation is reverted
   and the worktree is clean.
2. **The stand-in `io.papermc.paper.command.brigadier.ApiMirrorRootNode` cannot leak.** It lives only in
   `paper/src/test/java`. Nothing publishes or consumes paper's test output: there are no testFixtures, and no
   `sourceSets.test.output` dependency in the root build, buildSrc, paper-testing or paper-kotlin. The integration
   fixtures add only `paper/src/main/java`. paper-testing has no access to the stand-in, so `PAPER` stays null there,
   as before. Side effect: in paper's own tests, `PAPER` is now non-null for the whole JVM. The only paper test that
   uses PaperDispatcher is ReplaceChildTest, and plain `RootCommandNode`s still take the old branch (`isInstance`
   is false, and `pluginCommandNode` is null).
3. **modded CommandLibRegistrationTest#colliding_top_level_names_throw_from_register_before_a_server_runs can fail.**
   Without `CommandNameValidator.validateTopLevel`, `register` runs only `defaults.applyTo`,
   `CommandTree.permissionConfigs` and `REGISTRATIONS.add`, and none of those throws on duplicate names. So before
   the fix the call returned normally, and the test would fail. I checked this by reading the code only; I did not
   run a modded build.
4. **The merged Bukkit fixtures still cover the Spigot 1.20.3 quirk.** The deleted paper copies of ScenarioTest and
   VanillaExecutionTest differed from the spigot copies only in these places: the 1.20.3 branches
   (`"0,0,0"` / `runs = 0`), the `BukkitUtil` import, and the LATE_COMMAND javadoc. The shared copy in
   `integration-test/shared/bukkit` keeps both 1.20.3 branches. On Paper they cannot trigger, because Paper has no
   1.20.3. `BukkitUtil.getMinecraftVersion()` exists in both the paper and spigot modules. The javadoc now describes
   both suites. Both TestMain classes construct the shared classes. Folia was already excluded and stays excluded.
   `:integration-test:shared:spigot-fixture:compileJava` and `:paper-fixture:compileJava` both pass.
5. **velocity-testing build script.** `velocity` declares `compileOnlyApi("com.velocitypowered:velocity-api:3.4.0")`,
   so dropping the duplicate compileOnly is safe. `:velocity-testing:compileJava` and `compileTestJava` pass.
6. **Docs and CHANGELOG.** No `.md` file still points at the deleted paper or spigot copies of ScenarioTest or
   VanillaExecutionTest (I checked compatibility.md, roadmap.md, the registration audit, vanilla-integration.md and
   the integration README). The CHANGELOG entry about leading `-` matches the validator. The audit-doc text on
   1.21.4 / CME matches the code.

## Optional nit (not worth a commit on its own)

- N1, simplify, trivial: `paper/src/main/java/dev/marulabs/commandlib/PaperDispatcher.java:162`. The comment
  "Not a Paper server, as in tests" is now only half true, because paper's own tests find the stand-in mirror root.
  If the file is touched again, change it to "Not a Paper server, or Paper changed these internals". It saves 0 lines.

Not checked: I did not run a modded build (the test's ability to fail comes from reading the code) and did not run
any real-server integration. Earlier passes' items in triage.md are not repeated here.
