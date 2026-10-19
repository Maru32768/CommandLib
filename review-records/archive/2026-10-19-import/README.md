# Imported review archive

`manifest.json` records original source paths, import snapshot paths, locations in review runs, byte sizes, and
SHA-256 hashes. `IMPORT.md` records import-specific provenance and entry points.

Treat original files as immutable evidence. Record corrections separately, interpret historical links using the
source paths in the manifest, and use working copies when reproducing findings.

Originals in review runs and their manifests belong in version control. See the [review recording conventions](../../README.md)
for integrity verification and evidence handling.
