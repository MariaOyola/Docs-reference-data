@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "WRAPPER_PROPERTIES=%SCRIPT_DIR%.mvn\wrapper\maven-wrapper.properties"

for /f "tokens=1,* delims==" %%A in ('findstr /i "distributionUrl" "%WRAPPER_PROPERTIES%"') do set "DISTRIBUTION_URL=%%B"
for %%F in (%DISTRIBUTION_URL%) do set "DISTRIBUTION_FILENAME=%%~nxF"
set "DISTRIBUTION_NAME=%DISTRIBUTION_FILENAME:.zip=%"
rem apache-maven-X.Y.Z-bin.zip unpacks into apache-maven-X.Y.Z (no -bin suffix)
set "DISTRIBUTION_DIR=%DISTRIBUTION_NAME:-bin=%"

if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "DISTS_DIR=%MAVEN_USER_HOME%\wrapper\dists"
set "CACHE_DIR=%DISTS_DIR%\%DISTRIBUTION_DIR%"

if not exist "%CACHE_DIR%\bin\mvn.cmd" (
  echo [mvnw] Downloading %DISTRIBUTION_URL%
  md "%DISTS_DIR%" 2>nul
  powershell -NoProfile -Command "Invoke-WebRequest '%DISTRIBUTION_URL%' -OutFile '%DISTS_DIR%\%DISTRIBUTION_FILENAME%'"
  powershell -NoProfile -Command "Expand-Archive '%DISTS_DIR%\%DISTRIBUTION_FILENAME%' -DestinationPath '%DISTS_DIR%' -Force"
  del "%DISTS_DIR%\%DISTRIBUTION_FILENAME%"
)

"%CACHE_DIR%\bin\mvn.cmd" %*
