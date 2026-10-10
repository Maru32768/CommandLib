"""Shared helpers that write typed NMS classes into nms/<module> and register them in the spigot wrappers."""
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NMS_SOURCES = ROOT / "spigot/src/main/java/net/kunmc/lab/commandlib/util/nms"
BASE_PACKAGE = "net.kunmc.lab.commandlib.util.nms"
NL = "\n"


def _wrapper_packages():
    """Maps each abstract wrapper's simple name to its package, skipping the version implementation packages."""
    packages = {}
    for directory, _, files in os.walk(NMS_SOURCES):
        relative = Path(directory).relative_to(NMS_SOURCES).as_posix()
        if re.match(r"v\d", relative.split("/")[-1]):
            continue
        package = BASE_PACKAGE if relative == "." else BASE_PACKAGE + "." + relative.replace("/", ".")
        for file in files:
            packages[file[:-len(".java")]] = package
    return packages


WRAPPER_PACKAGES = _wrapper_packages()
IMPLICIT_IMPORTS = [
    ("ArgumentType<", "com.mojang.brigadier.arguments.ArgumentType"),
    ("CommandContext<", "com.mojang.brigadier.context.CommandContext"),
    ("CommandSyntaxException e", "com.mojang.brigadier.exceptions.CommandSyntaxException"),
    ("UncheckedCommandSyntaxException", BASE_PACKAGE + ".exception.UncheckedCommandSyntaxException"),
    ("List<", "java.util.List"),
    ("Collectors.", "java.util.stream.Collectors"),
]


def module_package(module):
    return "net.kunmc.lab.commandlib.nms." + module.replace("-", "_").replace(".", "_")


def write_source(module, package, name, source):
    path = ROOT / "nms" / module / "src/main/java" / package.replace(".", "/") / f"{name}.java"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(source, encoding="utf-8", newline=NL)


def write(module, wrapper, body, imports=()):
    """
    Writes the typed implementation of a wrapper such as NMSArgumentDimension or NMSHolder.NMSReference and returns
    its fully qualified name. Wrappers that the body mentions are imported automatically.
    """
    outer = wrapper.split(".")[0]
    name = f"{wrapper.split('.')[-1]}_{module_package(module).split('.')[-1]}"
    package = module_package(module)

    all_imports = set(imports)
    all_imports.add(f"{WRAPPER_PACKAGES[outer]}.{outer}")
    for other, other_package in WRAPPER_PACKAGES.items():
        if other != outer and re.search(r"\b" + other + r"\b", body):
            all_imports.add(f"{other_package}.{other}")
    for marker, implicit in IMPLICIT_IMPORTS:
        if marker in body:
            all_imports.add(implicit)
    import_lines = NL.join(f"import {x};" for x in sorted(all_imports, key=lambda x: (not x.startswith("com."), x)))

    write_source(module, package, name,
                 f"package {package};{NL}{NL}{import_lines}{NL}{NL}"
                 f"public class {name} extends {wrapper} {{{NL}{body.rstrip()}{NL}}}{NL}")
    return f"{package}.{name}"


def register(wrappers):
    """
    Makes sure each wrapper, such as NMSArgumentDimension or NMSHolder.NMSReference, registers its typed classes.
    NMSClassRegistry derives the class names from its module table, so one registerTyped call per wrapper covers every
    module. A wrapper that already calls registerTyped is left alone.
    """
    for wrapper in dict.fromkeys(wrappers):
        outer = wrapper.split(".")[0]
        path = ROOT / "spigot/src/main/java" / WRAPPER_PACKAGES[outer].replace(".", "/") / f"{outer}.java"
        source = path.read_text(encoding="utf-8")

        # Nested wrappers register under their simple name inside the outer class.
        last = None
        for reference in dict.fromkeys([wrapper, wrapper.split(".")[-1]]):
            if re.search(r"NMSClassRegistry\.registerTyped\(\s*" + re.escape(reference) + r"\.class\b", source):
                last = False
                break
            pattern = re.compile(r"NMSClassRegistry\.register\(\s*" + re.escape(reference) + r"\.class[^;]*;" + NL)
            matches = list(pattern.finditer(source))
            if matches:
                last = matches[-1]
                break
        if last is False:
            continue
        if last is None:
            raise SystemExit(f"No reflection registration found for {wrapper}")

        line_start = source.rfind(NL, 0, last.start()) + 1
        indent = re.match(r"\s*", source[line_start:]).group(0)
        call = f"{indent}NMSClassRegistry.registerTyped({reference}.class);{NL}"
        path.write_text(source[:last.end()] + call + source[last.end():], encoding="utf-8", newline=NL)


def write_module_table(modules):
    """
    Writes TypedNmsModules, the table of typed NMS modules that NMSClassRegistry derives class names and version
    ranges from. A Paper module also ships its reobfuscated classes in the <package>_spigot package.
    """
    entries = []
    for module, spec in modules.items():
        package = module_package(module).split(".")[-1]
        narrowed = "".join(f'{NL}{" " * 20}.withWrapperVersions("{wrapper}", "{lower}", "{upper}")'
                           for wrapper, (lower, upper) in spec.wrapper_versions.items())
        entries.append(f'new NMSClassRegistry.TypedModule("{package}", "{spec.lower}", "{spec.upper}"){narrowed}')
        if module.startswith("paper-"):
            entries.append(f'new NMSClassRegistry.TypedModule("{package}_spigot", "{package}", "{spec.lower}", '
                           f'"{spec.upper}"){narrowed}')
    separator = f",{NL}{' ' * 12}"
    path = NMS_SOURCES / "TypedNmsModules.java"
    path.write_text(f"""package {BASE_PACKAGE};

import java.util.List;

/**
 * The typed NMS modules bundled into the spigot jar (see {{@code docs/agents/nms-build.md}}). Generated by
 * {{@code tools/nms-codegen/generate.py}} from its module table; edit the table and regenerate instead of this file.
 */
final class TypedNmsModules {{
    static final List<NMSClassRegistry.TypedModule> MODULES = List.of(
            {separator.join(entries)});

    private TypedNmsModules() {{
    }}
}}
""", encoding="utf-8", newline=NL)
