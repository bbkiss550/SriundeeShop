@echo off
setlocal

cd /d "%~dp0"

echo Starting SriundeeShop...
echo.
echo Checking Java...
set "JAVA_HOME_FILE=%TEMP%\sriundee-java-home-%RANDOM%-%RANDOM%.txt"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0ensure-java.ps1" > "%JAVA_HOME_FILE%"
if errorlevel 1 (
    if exist "%JAVA_HOME_FILE%" del "%JAVA_HOME_FILE%"
    echo.
    echo Java setup failed. Check your internet connection and try again.
    exit /b 1
)

set /p JAVA_HOME=<"%JAVA_HOME_FILE%"
del "%JAVA_HOME_FILE%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java setup did not return a usable Java home.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java from: %JAVA_HOME%
echo.
echo Local profile uses database: sriundee_shop_test
echo URL: http://localhost:8080
echo.

echo Checking port 8080...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$pids = (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique; foreach ($pidValue in $pids) { if ($pidValue) { Write-Host ('Stopping process on port 8080: ' + $pidValue); Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue } }"
echo.
echo Starting on port 8080 only...
echo.

if not exist "%~dp0logs" mkdir "%~dp0logs"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$arguments = @('spring-boot:run', '-Dspring-boot.run.arguments=--server.port=8080', '-Dspring-boot.run.profiles=local');" ^
    "Start-Process -FilePath '%~dp0mvnw.cmd' -ArgumentList $arguments -WorkingDirectory '%~dp0' -WindowStyle Hidden -RedirectStandardOutput '%~dp0logs\run-output.log' -RedirectStandardError '%~dp0logs\run-error.log' | Out-Null"

if errorlevel 1 (
    echo Failed to start SriundeeShop.
    exit /b 1
)

start "" "http://localhost:8080"

endlocal
exit
