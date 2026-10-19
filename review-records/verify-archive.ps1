param(
    [string]$Archive = '2026-10-19-import'
)

$ErrorActionPreference = 'Stop'
$archiveParent = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'archive'))
$archiveRoot = [IO.Path]::GetFullPath((Join-Path $archiveParent $Archive))
if (-not $archiveRoot.StartsWith($archiveParent + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Archive must be inside review-records/archive.'
}
$manifest = Get-Content -LiteralPath (Join-Path $archiveRoot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$totalBytes = 0L
foreach ($entry in $manifest.entries) {
    $path = [IO.Path]::GetFullPath((Join-Path $archiveRoot $entry.archive_path))
    $rawRoot = Join-Path $archiveRoot 'raw'
    if (-not $path.StartsWith($rawRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw ('Manifest path outside raw: ' + $entry.archive_path)
    }
    $file = Get-Item -LiteralPath $path
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($file.Length -ne $entry.bytes -or $hash -ne $entry.sha256) {
        throw ('Archive mismatch: ' + $entry.archive_path)
    }
    $totalBytes += $file.Length
}
$files = @(Get-ChildItem -LiteralPath (Join-Path $archiveRoot 'raw') -Recurse -Force -File)
if ($files.Count -ne $manifest.file_count -or $manifest.entries.Count -ne $manifest.file_count -or $totalBytes -ne $manifest.total_bytes) {
    throw 'Archive counts or total bytes do not match the manifest.'
}
Write-Output ('Verified {0} files, {1} bytes.' -f $files.Count, $totalBytes)
