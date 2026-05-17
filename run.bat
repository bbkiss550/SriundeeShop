@echo off
setlocal

cd /d "%~dp0"

echo Starting SriundeeShop...
echo.
echo Make sure MySQL is running and the database settings in application.properties are correct.
echo URL: http://localhost:8080
echo.

call "%~dp0mvnw.cmd" spring-boot:run %*

endlocal
