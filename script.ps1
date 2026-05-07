$files = Get-ChildItem -Path app/src/main/res/layout -Recurse -File *.xml
foreach ($f in $files) {
    $c = Get-Content $f.FullName -Raw
    $mod = $false

    if ($c -match 'android:background="@color/(background|app_bg|details_background)"') {
        $c = $c -replace 'android:background="@color/background"', 'android:background="@drawable/bg_main_premium"'
        $c = $c -replace 'android:background="@color/app_bg"', 'android:background="@drawable/bg_main_premium"'
        $c = $c -replace 'android:background="@color/details_background"', 'android:background="@drawable/bg_main_premium"'
        $mod = $true
    }
    
    if ($c -match 'app:cardBackgroundColor="@color/(background|app_bg)"') {
        $c = $c -replace 'app:cardBackgroundColor="@color/(background|app_bg)"', 'app:cardBackgroundColor="@color/transparent" android:background="@drawable/bg_glass"'
        $mod = $true
    }
    
    if ($c -match 'android:background="#131313"') {
        $c = $c -replace 'android:background="#131313"', 'android:background="@drawable/bg_main_premium"'
        $mod = $true
    }

    if ($mod) { Set-Content $f.FullName -Value $c; Write-Host "Modified $($f.Name)" }
}
