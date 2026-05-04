@echo off
setlocal

where java >nul 2>nul
if errorlevel 1 (
  echo Java non trovato nel PATH.
  echo Installa JDK 17 o superiore, poi imposta JAVA_HOME e PATH.
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "$line=java -version 2>&1 | Select-Object -First 1; if ($line -match '\"1\.([0-9]+)') { $major=[int]$Matches[1] } elseif ($line -match '\"([0-9]+)') { $major=[int]$Matches[1] } else { $major=0 }; if ($major -lt 17) { exit 17 }"
if errorlevel 17 (
  echo Versione Java non supportata.
  echo Questo progetto richiede JDK 17 o superiore.
  echo Controlla con: java -version
  echo Poi avvia di nuovo: start.bat
  exit /b 1
)

.\mvnw.cmd javafx:run
