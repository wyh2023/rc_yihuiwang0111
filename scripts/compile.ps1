$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "out"
$localJavac = Join-Path $env:USERPROFILE ".local\devtools\jdk\jdk-21.0.11+10\bin\javac.exe"
$javac = if (Get-Command javac -ErrorAction SilentlyContinue) {
    "javac"
} elseif (Test-Path $localJavac) {
    $localJavac
} else {
    throw "javac was not found. Install JDK 17+ or add it to PATH."
}

if (Test-Path $out) {
    Remove-Item $out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $out | Out-Null

$sources = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d $out $sources

Write-Host "Compiled to $out"
