Write-Host "=== Перевірка проекту MovieTimeTracker ===" -ForegroundColor Cyan
Write-Host ""

# Перехід до директорії проекту
Set-Location "C:\Users\Gandon\Desktop\Projects\MovieTimeTracker"

# Очищення попередніх збірок
Write-Host "Крок 1: Очищення попередніх збірок..." -ForegroundColor Yellow
.\gradlew clean | Out-Null
Write-Host "✓ Очищення завершено" -ForegroundColor Green
Write-Host ""

# Збірка проекту
Write-Host "Крок 2: Збірка проекту..." -ForegroundColor Yellow
$buildOutput = .\gradlew assembleDebug --stacktrace 2>&1
$buildSuccess = $LASTEXITCODE -eq 0

if ($buildSuccess) {
    Write-Host "✓ Збірка успішна!" -ForegroundColor Green

    # Перевірка APK
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "✓ APK створено: $apkPath" -ForegroundColor Green
        Write-Host "  Розмір: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Gray
    } else {
        Write-Host "✗ APK не знайдено!" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Помилка збірки!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Деталі помилки:" -ForegroundColor Yellow
    $buildOutput | Select-String -Pattern "error|ERROR|FAILURE|Exception" | ForEach-Object {
        Write-Host $_.Line -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Завершено ===" -ForegroundColor Cyan

