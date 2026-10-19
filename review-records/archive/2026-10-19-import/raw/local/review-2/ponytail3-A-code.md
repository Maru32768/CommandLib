# ponytail3-A-code: main-code changes in 02fbf5f9..HEAD (HEAD = 477e21fa)

Commits reviewed: 83e54f34 (Paper map copy on 1.21.4+), bb0913f7 (PlatformAdapter U removed),
6f72c22e (shared usableOptions), cb265420 (modded validateTopLevel at register), plus the code they touch
(PaperDispatcher, CommandNodeChildren, Paper CommandLib Live.owns/unregister, spigot addToPaperDispatcher,
HelpMessageAction, CommandRunner, CommandNameValidator, CommandNodeCreator callers).

## Result

No findings worth fixing.

## Paper version check (83e54f34)

Patched the real Paper server jars from integration-test/targets/paper-*/build/docker-server/server.jar
(paperclip -Dpaperclip.patchonly=true) for 1.21.0, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8,
1.21.9, 1.21.10, 1.21.11, 26.1.1, 26.1.2, 26.2, 26.3, and checked with javap (plus Vineflower on 1.21.0, 1.21.4, 26.3):

| Member PaperDispatcher / CommandNodeChildren reflect on | 1.21.0-1.21.3 | 1.21.4-26.3 |
|---|---|---|
| `ApiMirrorRootNode` with public `getDispatcher()`, private `unwrapNode(CommandNode)` | yes | yes |
| `PluginCommandNode(String, PluginMeta, LiteralCommandNode, String)` | yes | absent (as the commit says) |
| `PaperCommands.INSTANCE`, `getDispatcherInternal()` | yes | yes |
| `CommandNode` final maps `children`, `literals`, `arguments` | yes | yes |

- In 1.21.4 and 26.3 the mirror's `addChild` is exactly `getDispatcher().getRoot().addChild(unwrapNode(node))`, and
  `removeCommand` is exactly `getDispatcher().getRoot().removeCommand(name)`. Paper's brigadier `removeCommand` (26.3
  bytecode) only removes from the three maps. So the mirror path (unwrapNode + `CommandNodeChildren.replace` on the
  vanilla root) does what the mirror's methods do, apart from the intended replace-instead-of-merge. The fix holds on
  every version from 1.21.0 to 26.3.
- `child()`/`Live.owns()` now read the vanilla node on 1.21.4+ too, as they already did on 1.21.0-1.21.3. Before the
  fix they got the mirror's wrapped node, which `wrapNode` caches in `wrappedCached`, so identity worked either way.
  No change in behavior.
- Spigot artifact: `spigot/.../CommandLib#addToPaperDispatcher` still needs `PluginCommandNode` and wasn't touched.
  The spigot artifact is limited to Paper <= 1.21.1 (`NMSClassRegistry.LAST_PAPER_VERSION`, docs/packaging.md), and
  `PluginCommandNode` is there on 1.20.6 and 1.21.1 (paperweight dev-bundle jars) and on 1.21.0 (patched jar).
  Newer Paper is refused before this code runs (paper-26.3-spigot-jar target). The spigot path is still correct.

## Other commits

- bb0913f7: PlatformAdapter/Holder/HelpMessageAction now compile without U. Nothing is left that uses it.
- 6f72c22e: `usableOptions` is now the one filter for the help and the argument-error usage. The error path builds a
  list only to call isEmpty, which is fine since that path runs only on a parse error.
- cb265420: validateTopLevel runs before REGISTRATIONS.add, so a rejected register leaves no state behind. The
  build-time check in CommandNodeCreator.build stays because the *-testing CommandTesters rely on it.

## Not reported (already in triage)

- Plain label nodes on Paper 1.21.4+ (no pluginCommandMeta/apiCommandMeta) are triage Pending (line 342).
