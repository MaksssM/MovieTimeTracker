# Check format specifiers consistency
$enFile = 'app/src/main/res/values/strings.xml'
$ukFile = 'app/src/main/res/values-uk/strings.xml'
$ruFile = 'app/src/main/res/values-ru/strings.xml'

function Get-StringMap($path) {
    [xml]$xml = Get-Content $path
    $map = @{}
    foreach ($node in $xml.resources.string) {
        if ($node.name) {
            $map[$node.name] = $node.InnerText
        }
    }
    return $map
}

function Get-FormatSpecifiers($str) {
    # Regex to match format specifiers like %d, %s, %.1f, %1$s, %2$d, etc.
    $pattern = '%(?:[0-9]+\$)?(?:\.[0-9]+)?[a-zA-Z]'
    $matches = [regex]::Matches($str, $pattern)
    $specs = @()
    foreach ($m in $matches) {
        $specs += $m.Value
    }
    return $specs -join ','
}

$enMap = Get-StringMap -path $enFile
$ukMap = Get-StringMap -path $ukFile
$ruMap = Get-StringMap -path $ruFile

Write-Host "=== Comparing UK format specifiers ==="
foreach ($key in $enMap.Keys) {
    if ($ukMap.ContainsKey($key)) {
        $enSpecs = Get-FormatSpecifiers $enMap[$key]
        $ukSpecs = Get-FormatSpecifiers $ukMap[$key]
        if ($enSpecs -ne $ukSpecs) {
            Write-Host "Mismatch for '$key':"
            Write-Host "  EN: '$($enMap[$key])' -> ($enSpecs)"
            Write-Host "  UK: '$($ukMap[$key])' -> ($ukSpecs)"
        }
    }
}

Write-Host ""
Write-Host "=== Comparing RU format specifiers ==="
foreach ($key in $enMap.Keys) {
    if ($ruMap.ContainsKey($key)) {
        $enSpecs = Get-FormatSpecifiers $enMap[$key]
        $ruSpecs = Get-FormatSpecifiers $ruMap[$key]
        if ($enSpecs -ne $ruSpecs) {
            Write-Host "Mismatch for '$key':"
            Write-Host "  EN: '$($enMap[$key])' -> ($enSpecs)"
            Write-Host "  RU: '$($ruMap[$key])' -> ($ruSpecs)"
        }
    }
}
