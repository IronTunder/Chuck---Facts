@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "BASE_DIR=%~dp0"
set "MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if exist "%MAVEN_CMD%" goto run_maven

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven non trovato. Scarico Apache Maven %MAVEN_VERSION% nella cartella .mvn...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$base='%BASE_DIR%';" ^
  "$version='%MAVEN_VERSION%';" ^
  "$zip=Join-Path $base '.mvn\apache-maven.zip';" ^
  "$dest=Join-Path $base '.mvn';" ^
  "New-Item -ItemType Directory -Force -Path $dest | Out-Null;" ^
  "Invoke-WebRequest -Uri ('https://archive.apache.org/dist/maven/maven-3/{0}/binaries/apache-maven-{0}-bin.zip' -f $version) -OutFile $zip;" ^
  "Expand-Archive -Path $zip -DestinationPath $dest -Force;" ^
  "Remove-Item $zip -Force;"

if not exist "%MAVEN_CMD%" (
  echo Impossibile preparare Maven Wrapper.
  exit /b 1
)

:run_maven
"%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
