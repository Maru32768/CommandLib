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
$recordsRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$verifiedPaths = @{}
$totalBytes = 0L
foreach ($entry in $manifest.entries) {
    if ($manifest.schema_version -ne 2) { throw 'Unsupported manifest schema.' }
    $path = [IO.Path]::GetFullPath((Join-Path $recordsRoot $entry.record_path))
    $runsRoot = Join-Path $recordsRoot 'runs'
    if (-not $path.StartsWith($runsRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw ('Manifest path outside runs: ' + $entry.record_path)
    }
    if ($verifiedPaths.ContainsKey($path)) { throw ('Duplicate original: ' + $entry.record_path) }
    $verifiedPaths[$path] = $true
    $file = Get-Item -LiteralPath $path
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($file.Length -ne $entry.bytes -or $hash -ne $entry.sha256) {
        throw ('Original mismatch: ' + $entry.record_path)
    }
    $totalBytes += $file.Length
}
if ($verifiedPaths.Count -ne $manifest.file_count -or $manifest.entries.Count -ne $manifest.file_count -or $totalBytes -ne $manifest.total_bytes) {
    throw 'Archive counts or total bytes do not match the manifest.'
}
Write-Output ('Verified {0} original files, {1} bytes.' -f $verifiedPaths.Count, $totalBytes)
