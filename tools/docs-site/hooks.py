"""MkDocs hooks that let the site use the Markdown as GitHub shows it.

README.md becomes the home page, and every relative link keeps working: links into the docs are resolved for the
site, and links to anything that is not a site page (sources, samples, the excluded agent and roadmap docs) go to
GitHub.
"""

import os
import re

from mkdocs.structure.files import File

GITHUB = "https://github.com/Maru32768/CommandLib"
BRANCH = "master"
LINK = re.compile(r"\]\(([^)\s]+)\)")
SCHEME = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*:")
FENCE = re.compile(r"^\s*(```|~~~)")


def _root(config):
    return os.path.dirname(os.path.abspath(config.config_file_path))


def on_files(files, config):
    files.append(File.generated(config, "index.md", abs_src_path=os.path.join(_root(config), "README.md")))
    return files


def on_page_markdown(markdown, page, config, files):
    root = _root(config)
    docs = os.path.abspath(config.docs_dir)
    readme = os.path.join(root, "README.md")
    source = os.path.abspath(page.file.abs_src_path)
    source_dir = os.path.dirname(source)
    # The page's own directory on the site, which differs from the source's for the README.
    page_dir = os.path.dirname(os.path.join(docs, page.file.src_path))

    def rewrite(match):
        target = match.group(1)
        if target.startswith("#") or SCHEME.match(target):
            return match.group(0)
        path, _, anchor = target.partition("#")
        anchor = "#" + anchor if anchor else ""
        resolved = os.path.normpath(os.path.join(source_dir, path))
        if resolved == readme:
            resolved = os.path.join(docs, "index.md")
        relative_to_docs = os.path.relpath(resolved, docs).replace(os.sep, "/")
        built = files.get_file_from_path(relative_to_docs)
        # Excluded docs (exclude_docs) stay in Files but are not built, so they go to GitHub like other files.
        if built is not None and built.inclusion.is_included():
            return "](" + os.path.relpath(resolved, page_dir).replace(os.sep, "/") + anchor + ")"
        if not os.path.exists(resolved):
            # Left as it is, so that MkDocs reports the broken link.
            return match.group(0)
        relative_to_root = os.path.relpath(resolved, root).replace(os.sep, "/")
        kind = "tree" if os.path.isdir(resolved) else "blob"
        return "](" + f"{GITHUB}/{kind}/{BRANCH}/{relative_to_root}{anchor})"

    lines = []
    in_fence = False
    for line in markdown.split("\n"):
        if FENCE.match(line):
            in_fence = not in_fence
        elif not in_fence:
            line = LINK.sub(rewrite, line)
            # md_in_html renders the Markdown inside the README's collapsible blocks.
            line = line.replace("<details>", '<details markdown="1">')
        lines.append(line)
    return "\n".join(lines)
