param(
    [string]$ProjectRoot = $PSScriptRoot
)

$ErrorActionPreference = "Continue"

$Sdk = $env:ANDROID_SDK_ROOT
if (-not $Sdk) { $Sdk = $env:ANDROID_HOME }
if (-not $Sdk -and $env:LOCALAPPDATA) { $Sdk = Join-Path $env:LOCALAPPDATA "Android/Sdk" }
if (-not $Sdk -or -not (Test-Path $Sdk)) { throw "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME." }

$Bt       = Join-Path (Join-Path $Sdk "build-tools") "36.0.0"
$Platform = Join-Path (Join-Path (Join-Path $Sdk "platforms") "android-36") "android.jar"
$MinSdk   = 26
$TargetSdk= 36
$Jdk      = $env:JAVA_HOME
if (-not $Jdk) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) { $Jdk = Split-Path (Split-Path $javaCmd.Source) }
}
if (-not $Jdk) { throw "JAVA_HOME not set and java not on PATH." }

$src      = Join-Path $ProjectRoot "src"
$res      = Join-Path $ProjectRoot "res"
$manifest = Join-Path $ProjectRoot "AndroidManifest.xml"
$build    = Join-Path $ProjectRoot "build"
$gen      = Join-Path $build "gen"
$flat     = Join-Path $build "res-flat"
$classes  = Join-Path $build "classes"
$dex      = Join-Path $build "dex"
$out      = Join-Path $ProjectRoot "dist"

foreach ($d in @($build, $gen, $flat, $classes, $dex, $out)) {
    if (Test-Path $d) { Remove-Item $d -Recurse -Force }
    New-Item -ItemType Directory -Path $d -Force | Out-Null
}

function Step($name) { Write-Host "`n=== $name ===" -ForegroundColor Cyan }

# ---------------------------------------------------------------- resources
Step "aapt2 compile"
$aapt2 = Join-Path $Bt "aapt2.exe"
$resZip = Join-Path $build "res-flat.zip"
if (Test-Path $resZip) { Remove-Item $resZip -Force }
if (Test-Path $res) {
    & $aapt2 compile --dir $res -o $resZip
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
}

Step "aapt2 link"
$unsigned = Join-Path $out "app-unsigned.apk"
$linkArgs = @(
    "link",
    "-o", $unsigned,
    "-I", $Platform,
    "--manifest", $manifest,
    "--java", $gen,
    "--min-sdk-version", "$MinSdk",
    "--target-sdk-version", "$TargetSdk",
    "--version-code", "1",
    "--version-name", "1.0.0",
    "--auto-add-overlay"
)
if ((Test-Path $resZip) -and ((Get-Item $resZip).Length -gt 0)) {
    $linkArgs += @("-R", $resZip)
}
& $aapt2 @linkArgs
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

# ---------------------------------------------------------------- java
Step "javac"
$sources = @()
$sources += Get-ChildItem $src      -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$sources += Get-ChildItem $gen      -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$javac = Join-Path $Jdk "bin\javac.exe"
& $javac -encoding UTF-8 --release 8 -nowarn -classpath $Platform -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# ---------------------------------------------------------------- dex
Step "jar classes"
$classesJar = Join-Path $build "classes.jar"
if (Test-Path $classesJar) { Remove-Item $classesJar -Force }
& (Join-Path $Jdk "bin\jar.exe") cf $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Step "d8"
$d8 = Join-Path $Bt "d8.bat"
& $d8 --release --min-api $MinSdk --lib $Platform --output $dex $classesJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

# ---------------------------------------------------------------- package
Step "package dex into apk"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    foreach ($f in (Get-ChildItem $dex -Filter "*.dex")) {
        $existing = $zip.GetEntry($f.Name)
        if ($existing) { $existing.Delete() }
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $f.FullName, $f.Name, [System.IO.Compression.CompressionLevel]::Optimal)
    }
} finally { $zip.Dispose() }

# ---------------------------------------------------------------- align + sign
Step "zipalign"
$aligned = Join-Path $build "app-aligned.apk"
& (Join-Path $Bt "zipalign.exe") -f -p 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Step "keystore"
$ks = Join-Path $ProjectRoot "ironlog-debug.keystore"
if (-not (Test-Path $ks)) {
    & (Join-Path $Jdk "bin\keytool.exe") -genkeypair -v `
        -keystore $ks -storepass android -keypass android `
        -alias ironlog -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=IronLog Debug, OU=Debug, O=IronLog, L=NA, S=NA, C=CN" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

Step "apksigner"
$final = Join-Path $out "IronLog-1.0.0.apk"
if (Test-Path $final) { Remove-Item $final -Force }
& (Join-Path $Bt "apksigner.bat") sign `
    --ks $ks --ks-pass pass:android --key-pass pass:android `
    --ks-key-alias ironlog `
    --min-sdk-version $MinSdk `
    --out $final $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Step "verify"
& (Join-Path $Bt "apksigner.bat") verify --min-sdk-version $MinSdk --print-certs $final 2>&1 | Out-String | Write-Host
if ($LASTEXITCODE -ne 0) { throw "signature verify failed" }

Step "verify launcher class"
$badging = & (Join-Path $Bt "aapt2.exe") dump badging $final 2>&1
$launchMatch = $badging | Select-String "launchable-activity: name='([^']+)'"
if (-not $launchMatch) { throw "no launchable activity declared in manifest" }
$launchClass = $launchMatch.Matches[0].Groups[1].Value
$descriptor = "L" + ($launchClass -replace "\.", "/") + ";"
$dexDump = & (Join-Path $Bt "dexdump.exe") (Join-Path $dex "classes.dex") 2>&1
if (-not ($dexDump | Select-String ([regex]::Escape($descriptor)))) {
    throw "launcher class $launchClass is declared but missing from classes.dex"
}
Write-Host "launcher -> $launchClass (present in dex)" -ForegroundColor Green

Step "verify manifest classes"
$manifestText = Get-Content $manifest -Raw
$declared = [regex]::Matches($manifestText, 'android:name="(\.[^"]+|com\.ironlog[^"]*)"') |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$missing = @()
foreach ($cls in $declared) {
    $full = if ($cls.StartsWith(".")) { "com.ironlog.train" + $cls } else { $cls }
    $desc = "L" + ($full -replace "\.", "/") + ";"
    if (-not ($dexDump | Select-String ([regex]::Escape($desc)))) {
        $missing += $full
    }
}
if ($missing.Count -gt 0) {
    throw "classes declared in manifest but missing from dex: $($missing -join ', ')"
}
Write-Host ("manifest classes OK: " + ($declared -join ", ")) -ForegroundColor Green

$size = [math]::Round((Get-Item $final).Length / 1KB, 1)
Write-Host "`nBUILD OK -> $final ($size KB)" -ForegroundColor Green
