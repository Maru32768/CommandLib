# nms-codegen

Generates the typed NMS classes in `nms/*`, adds the `registerTyped` call to each spigot wrapper that lacks one, and
writes the module table that `NMSClassRegistry` reads.
The generated sources are committed; edit the scripts and regenerate instead of editing the classes by hand.

The scripts need only the Python standard library. [uv](https://docs.astral.sh/uv/) runs them with the Python
version pinned in `.python-version`, downloading it when missing:

```bash
cd tools/nms-codegen
uv run generate.py --all
uv run generate.py spigot-1.20.4
```

Without uv, any Python 3.10 or later works: `python generate.py --all`.

- `codegen.py`: writes classes and wrapper registrations.
- `spigot_legacy.py`: Spigot 1.16.5, which compiles against the Spigot-mapped server jar.
- `mojang.py`: modules compiled with Mojang names. Version flags such as `HAS_BUILD_CONTEXT` switch the code where
  the Minecraft API changed.
- `generate.py`: the module table (versions, CraftBukkit package) and entry point. Each run also writes the table to
  `TypedNmsModules` in the spigot module.

See `docs/agents/nms-build.md` for the module layout.
