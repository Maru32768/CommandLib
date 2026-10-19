# Source evidence

Reviewed commit: 2236d1e721118e69b592ccb500fe1a752baa7720. No tracked diff at start; unrelated untracked .claude/ excluded.

- integration-test/src/test/java/dev/marulabs/commandlib/integration/IntegrationTest.java:71-91: negative branch; :356-366: XML assertion.
- integration-test/shared/core/src/main/java/dev/marulabs/integration/core/JUnitXmlReport.java: build uses results.size() and counts FAILED entries; no independent inventory.
- integration-test/shared/bukkit/src/main/java/dev/marulabs/testplugin/TestBase.java: putCommandNotExecutedResult initializes FAILED; results returns map values.
- integration-test/shared/paper/src/main/java/dev/marulabs/testplugin/ArgumentTest.java: build explicitly calls builders with version exclusions; attributeArgument creates a FAILED placeholder before registration.
- integration-test/shared/paper/src/main/java/dev/marulabs/testplugin/TestMain.java:108-162: suite list, build, dispatch and flattening results. shared/spigot TestMain has the same flow.
- integration-test/shared/modded/src/main/java/dev/marulabs/testmod/TestBase.java: FAILED placeholder pattern.
- integration-test/src/test/java/dev/marulabs/commandlib/integration/ArgumentIntegrationCoverageTest.java: assertIntegrationCases checks source construction, not runtime inventory.
- integration-test/targets/paper-26.3-spigot-jar/build.gradle.kts:7 and integration-test/README.md: intentional enable failure.

Commands: git rev-parse HEAD; git status --short; git diff --stat; rg --files; rg -n and Get-Content on these paths. Static source tracing only; no runtime reproduction, Java/dependency execution, or Gradle/Docker tests.
