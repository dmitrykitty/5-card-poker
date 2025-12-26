# Konfiguracja kodowania .NET dla PowerShella
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# Ustawienie strony kodowej konsoli Windows
chcp 65001 | Out-Null

# Ścieżka do JAR (relatywna do lokalizacji skryptu)
$JAR_PATH = "$PSScriptRoot/../poker-client/target/poker-client-1.0-SNAPSHOT-with-dependencies.jar"

Write-Host "Running poker-client with UTF-8..." -ForegroundColor Green

java "-Dfile.encoding=UTF-8" -jar "$JAR_PATH"
pause