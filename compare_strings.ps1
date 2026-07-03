$enFile = 'app\src\main\res\values\strings.xml'
$ukFile = 'app\src\main\res\values-uk\strings.xml'
$ruFile = 'app\src\main\res\values-ru\strings.xml'

function Get-StringNames {
    param([string]$path)
    $content = Get-Content $path -Raw
    $pattern = 'name="([^"]+)"'
    $results = [regex]::Matches($content, $pattern)
    $names = @()
    foreach ($m in $results) {
        $names += $m.Groups[1].Value
    }
    return $names | Sort-Object -Unique
}

$enStrings = Get-StringNames -path $enFile
$ukStrings = Get-StringNames -path $ukFile
$ruStrings = Get-StringNames -path $ruFile

Write-Host "=== MISSING IN UK (not in EN default) ==="
$missingUk = $enStrings | Where-Object { $_ -notin $ukStrings }
if ($missingUk) { $missingUk | ForEach-Object { Write-Host "  $_" } } else { Write-Host "  (none)" }

Write-Host ""
Write-Host "=== MISSING IN RU (not in EN default) ==="
$missingRu = $enStrings | Where-Object { $_ -notin $ruStrings }
if ($missingRu) { $missingRu | ForEach-Object { Write-Host "  $_" } } else { Write-Host "  (none)" }

Write-Host ""
Write-Host "=== EN count: $($enStrings.Count), UK count: $($ukStrings.Count), RU count: $($ruStrings.Count)"
