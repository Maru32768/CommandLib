"""
Generates the typed NMS classes of the nms/* modules and the module table of the spigot module.

    uv run generate.py spigot-1.20.4
    uv run generate.py --all
    uv run generate.py --check

--check regenerates every module into a temporary directory and exits with status 1 when the committed sources differ.

See docs/agents/nms-build.md.
"""
import runpy
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent


@dataclass(frozen=True)
class Module:
    """
    A typed NMS module: the Minecraft versions its classes run on and the CraftBukkit package of a Spigot module.
    """
    lower: str
    upper: str
    craftbukkit: str | None = None


# The typed NMS modules, one per directory under nms/. A Spigot module compiled with Mojang names covers only releases
# whose obfuscated names match its server jar (see docs/agents/nms-build.md). Generating any module also writes this
# table to TypedNmsModules in the spigot module, which NMSClassRegistry and settings.gradle.kts read.
MODULES = {
    "spigot-1.16.5": Module("1.16.4", "1.16.5", "v1_16_R3"),
    "spigot-1.17": Module("1.17", "1.17", "v1_17_R1"),
    "spigot-1.17.1": Module("1.17.1", "1.17.1", "v1_17_R1"),
    "spigot-1.18": Module("1.18", "1.18", "v1_18_R1"),
    "spigot-1.18.1": Module("1.18.1", "1.18.1", "v1_18_R1"),
    "spigot-1.18.2": Module("1.18.2", "1.18.2", "v1_18_R2"),
    "spigot-1.19": Module("1.19", "1.19", "v1_19_R1"),
    "spigot-1.19.1": Module("1.19.1", "1.19.1", "v1_19_R1"),
    "spigot-1.19.2": Module("1.19.2", "1.19.2", "v1_19_R1"),
    "spigot-1.19.3": Module("1.19.3", "1.19.3", "v1_19_R2"),
    "spigot-1.19.4": Module("1.19.4", "1.19.4", "v1_19_R3"),
    "spigot-1.20.1": Module("1.20", "1.20.1", "v1_20_R1"),
    "spigot-1.20.2": Module("1.20.2", "1.20.2", "v1_20_R2"),
    "spigot-1.20.3": Module("1.20.3", "1.20.3", "v1_20_R3"),
    "spigot-1.20.4": Module("1.20.4", "1.20.4", "v1_20_R3"),
    "spigot-1.20.6": Module("1.20.5", "1.20.6", "v1_20_R4"),
    "spigot-1.21.1": Module("1.21", "1.21.1", "v1_21_R1"),
    "spigot-1.21.3": Module("1.21.2", "1.21.3", "v1_21_R2"),
    "spigot-1.21.4": Module("1.21.4", "1.21.4", "v1_21_R3"),
    "spigot-1.21.5": Module("1.21.5", "1.21.5", "v1_21_R4"),
    "spigot-1.21.6": Module("1.21.6", "1.21.6", "v1_21_R5"),
    "spigot-1.21.8": Module("1.21.7", "1.21.8", "v1_21_R5"),
    "spigot-1.21.10": Module("1.21.9", "1.21.10", "v1_21_R6"),
    "spigot-1.21.11": Module("1.21.11", "1.21.11", "v1_21_R7"),
    # Spigot runs with Mojang names and an unversioned CraftBukkit package from 26.1.
    "spigot-26.1.2": Module("26.1", "26.3"),
    "paper-1.20.6": Module("1.20.5", "1.20.6"),
    "paper-1.21.1": Module("1.21", "1.21.1"),
}


def generate(module):
    script = "spigot_legacy.py" if module == "spigot-1.16.5" else "mojang.py"
    runpy.run_path(str(HERE / script),
                   init_globals={"MODULE": module, "CRAFTBUKKIT": MODULES[module].craftbukkit},
                   run_name="__main__")


def check(codegen):
    """Regenerates every module into a temporary directory and lists how the committed sources differ from it."""
    with tempfile.TemporaryDirectory() as temporary:
        codegen.OUT = Path(temporary)
        for module in MODULES:
            generate(module)
        codegen.write_module_table(MODULES)
        directories = [codegen.source_directory(module, codegen.ROOT) for module in MODULES]
        committed = {x.relative_to(codegen.ROOT).as_posix() for d in directories if d.is_dir()
                     for x in d.rglob("*") if x.is_file()}
        committed.add(codegen.MODULE_TABLE.as_posix())
        generated = {x.relative_to(codegen.OUT).as_posix() for x in codegen.OUT.rglob("*") if x.is_file()}
        differences = []
        for path in sorted(committed | generated):
            if path not in generated:
                differences.append(f"extra (no longer generated): {path}")
            elif not (codegen.ROOT / path).is_file():
                differences.append(f"missing: {path}")
            elif (codegen.ROOT / path).read_bytes() != (codegen.OUT / path).read_bytes():
                differences.append(f"changed: {path}")
        return differences


def main(args):
    sys.path.insert(0, str(HERE))
    import codegen
    if args == ["--check"]:
        differences = check(codegen)
        for difference in differences:
            print(difference)
        if differences:
            raise SystemExit(f"{len(differences)} generated files differ; run generate.py --all")
        print("generated sources are up to date")
        return
    if args == ["--all"]:
        modules = MODULES
    elif args and all(x in MODULES for x in args):
        modules = args
    else:
        raise SystemExit(f"usage: generate.py --all | --check | <module>...{chr(10)}modules: {', '.join(MODULES)}")
    for module in modules:
        codegen.clear(module)
        generate(module)
    codegen.write_module_table(MODULES)


if __name__ == "__main__":
    main(sys.argv[1:])
