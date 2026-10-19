# Implementation verification

Date: 2026-10-20 Asia/Tokyo (JUnit timestamps and server logs use 2026-10-19 UTC).
Execution base: `2236d1e721118e69b592ccb500fe1a752baa7720` plus `implementation.patch` in this directory.
`git apply --reverse --check` verified that the patch matches the implementation working tree.
Artifact paths, byte sizes and SHA-256 hashes are in `hashes.json`.

From the repository root in PowerShell:

```powershell
.\gradlew.bat --configure-on-demand '-Pcommandlib.typedNms=false' :spotlessJavaApply :integration-test:test :integration-test:targets:paper-26.3:minecraftIntegrationTest
```

Result: BUILD SUCCESSFUL (2m 46s). Eight new regression tests passed; five existing argument-coverage tests passed.
The existing opt-in Velocity server test was skipped by the normal lightweight test task, as intended.
The Paper runner's two tests passed, including the real-server test. Its fixture XML contains exactly 97 expected
case IDs, with zero failures, errors or skips. The runner also completed its bot, reload/log and disable checks.

Environment: root Gradle 9.5.0, nested fixture Gradle 9.4.1; Docker Desktop server 29.8.2;
Paper 26.3 build 180 (`a2d844f`), Java 25.0.4.1+1-LTS (Eclipse Adoptium) inside the server container.
The runner target uses Java 21 for the bot and the lightweight integration module targets Java 17.

After formatting and declaring the inventories/target configurations as Gradle test inputs:

```powershell
.\gradlew.bat --configure-on-demand '-Pcommandlib.typedNms=false' :integration-test:test :spotlessJavaCheck
git diff --check
git apply --reverse --check review-records/runs/2026-10-20-integration-test-completeness/evidence/implementation.patch
```

All passed. The final normal-user Gradle invocation was BUILD SUCCESSFUL (4m 1s); the test task was up-to-date after
the successful post-formatting lightweight run. `final-regression-tests.xml` preserves those eight passing tests;
`argument-coverage-tests.xml` preserves the five existing coverage checks. An earlier sandbox invocation passed the
lightweight tests but was canceled while waiting on Spotless; the final check above completed in the normal user
environment. `regression-tests.xml` preserves the earlier successful regression run, and `paper-26.3-runner.xml` /
`paper-26.3-fixture.xml` preserve the live-server verification.

Limits: only Paper 26.3 was rerun on a real server. Target inventory coverage was checked for all 76 positive shared
runner targets, with the current Spigot/Paper/modded/Folia source inventories and documented version exclusions.
The other server targets and the unchanged expected-enable-failure/Velocity paths were not rerun live.
