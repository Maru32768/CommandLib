"""
Generates the typed NMS classes of the nms/* modules and registers them in the spigot wrappers.

    uv run generate.py spigot-1.20.4
    uv run generate.py --all

See docs/agents/nms-build.md.
"""
import runpy
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODULES = [
    "spigot-1.16.5",
    "spigot-1.17.1",
    "spigot-1.18.2",
    "spigot-1.19.2",
    "spigot-1.19.4",
    "spigot-1.20.1",
    "spigot-1.20.4",
    "spigot-1.20.6",
    "paper-1.20.6",
]


def generate(module):
    script = "spigot_legacy.py" if module == "spigot-1.16.5" else "mojang.py"
    runpy.run_path(str(HERE / script), init_globals={"MODULE": module}, run_name="__main__")


def main(args):
    if args == ["--all"]:
        modules = MODULES
    elif args and all(x in MODULES for x in args):
        modules = args
    else:
        raise SystemExit(f"usage: generate.py --all | <module>...{chr(10)}modules: {', '.join(MODULES)}")
    sys.path.insert(0, str(HERE))
    for module in modules:
        generate(module)


if __name__ == "__main__":
    main(sys.argv[1:])
