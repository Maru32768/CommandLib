# Imported review archive

`raw/` preserves original files without editing their contents. `manifest.json` records their source paths,
archive paths, byte sizes, and SHA-256 hashes. `IMPORT.md` records import-specific provenance and path mappings.

Treat original files as immutable evidence. Record corrections separately, interpret historical links using the
source paths in the manifest, and use working copies when reproducing findings.

Archive originals and manifests belong in version control. See the [review recording conventions](../../README.md)
for integrity verification and evidence handling.
