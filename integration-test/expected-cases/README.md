# Expected integration cases

These inventories are independent of fixture registration and of the XML being checked. The shared runner requires
exactly these `classname.name` IDs, with no duplicates, failed cases or skipped cases. A missing suite or builder call
therefore cannot silently reduce coverage. Summary counters must also agree with the expected count.

`targets.properties` assigns every positive shared-runner target to a fixture profile. Unknown targets fail until they
receive an explicit assignment. `exclusions.properties` lists supported version exceptions: MCProtocolLib 1.16.5 cannot
read `brigadier:long`, and the Paper fixture needs Paper 1.21.8 for `blockInWorldPredicate()`.
The Spigot fixture on Paper 1.21.1 intentionally uses the Spigot inventory. Folia has its own smoke inventory.
The expected-enable-failure target checks logs instead of XML; Velocity uses a separate runner without this report.

When adding, removing or renaming a fixture case, review and update its profile explicitly. When adding a target,
assign its profile and document any version exclusion. Do not regenerate these files from the report under test or
from the actual registrations: that would accept the missing registration this check is intended to catch.

Initial inventories were cross-checked against fixture sources at `2236d1e721118e69b592ccb500fe1a752baa7720`, using local
reports as a starting point for IDs, then adding the newer Bukkit cases `ArgumentTest.chatColorArgumentReset` and
`ScenarioTest.{lateRegisteredCommandSurvivesReload,unregisteringKeepsALabelTakenSince,enableRegisteredCommandIsRemovedByUnregister}`.
The profiles contain 98 Spigot, 97 Paper, 72 modded and 4 Folia cases before exclusions. The regression tests check
missing cases/suites, replacements, duplicates, inconsistent counters, failures/skips, version exclusions and target coverage.
