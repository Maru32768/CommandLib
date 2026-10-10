"""
Generates the typed NMS classes of the nms/* modules and registers them in the spigot wrappers.

    uv run generate.py spigot-1.20.4
    uv run generate.py --all

See docs/agents/nms-build.md.
"""
import runpy
import sys
from dataclasses import dataclass, field
from pathlib import Path

HERE = Path(__file__).resolve().parent


@dataclass(frozen=True)
class Module:
    """
    A typed NMS module: the Minecraft versions its classes run on, the CraftBukkit package of a Spigot module, and
    narrower versions for wrappers whose class needs a later release than the module's lower version.
    """
    lower: str
    upper: str
    craftbukkit: str | None = None
    wrapper_versions: dict = field(default_factory=dict)


# The typed NMS modules, one per directory under nms/. A Spigot module compiled with Mojang names covers only releases
# whose obfuscated names match its server jar (see docs/agents/nms-build.md). Generating any module also writes this
# table to TypedNmsModules in the spigot module, which NMSClassRegistry and settings.gradle.kts read.
MODULES = {
    "spigot-1.16.5": Module("1.16.4", "1.16.5", "v1_16_R3"),
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
    "spigot-1.20.4": Module("1.20.4", "1.20.4", "v1_20_R3"),
    "spigot-1.20.6": Module("1.20.5", "1.20.6", "v1_20_R4"),
    "spigot-1.21.1": Module("1.21", "1.21.1", "v1_21_R1"),
    "paper-1.20.6": Module("1.20.5", "1.20.6",
                           # PaperCommands, which the class uses, is new in Paper 1.20.6.
                           wrapper_versions={"NMSDataPackResources": ("1.20.6", "1.20.6")}),
}


def generate(module):
    script = "spigot_legacy.py" if module == "spigot-1.16.5" else "mojang.py"
    runpy.run_path(str(HERE / script),
                   init_globals={"MODULE": module, "CRAFTBUKKIT": MODULES[module].craftbukkit},
                   run_name="__main__")


def main(args):
    if args == ["--all"]:
        modules = MODULES
    elif args and all(x in MODULES for x in args):
        modules = args
    else:
        raise SystemExit(f"usage: generate.py --all | <module>...{chr(10)}modules: {', '.join(MODULES)}")
    sys.path.insert(0, str(HERE))
    from codegen import write_module_table
    for module in modules:
        generate(module)
    write_module_table(MODULES)


if __name__ == "__main__":
    main(sys.argv[1:])
