$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$localJava = Join-Path $env:USERPROFILE ".local\devtools\jdk\jdk-21.0.11+10\bin\java.exe"
$java = if (Get-Command java -ErrorAction SilentlyContinue) {
    "java"
} elseif (Test-Path $localJava) {
    $localJava
} else {
    throw "java was not found. Install JDK 17+ or add it to PATH."
}

& (Join-Path $root "scripts\compile.ps1")
& $java -cp (Join-Path $root "out") com.example.notification.Main
