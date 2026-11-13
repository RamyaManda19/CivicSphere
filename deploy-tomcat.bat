@echo off
title Deploy CivicSphere to Apache Tomcat
echo =====================================================================
echo   Deploying CivicSphere to Apache Tomcat 11.0 / 10.1
echo =====================================================================
cd /d "%~dp0"

set "TOMCAT_HOME=C:\Program Files\Apache Software Foundation\Tomcat 11.0"
if not exist "%TOMCAT_HOME%" (
    echo Tomcat not found at %TOMCAT_HOME%. Please update TOMCAT_HOME path.
    pause
    exit /b 1
)

echo Deploying target\CivicSphere.war to %TOMCAT_HOME%\webapps\...
copy /Y "target\CivicSphere.war" "%TOMCAT_HOME%\webapps\CivicSphere.war"

if %ERRORLEVEL% EQU 0 (
    echo Deployment successful!
    echo Starting Tomcat...
    start "" "%TOMCAT_HOME%\bin\startup.bat"
    start "" "http://localhost:8080/CivicSphere/"
) else (
    echo Note: Writing to Program Files requires Administrator privileges.
    echo Please right-click deploy-tomcat.bat and select "Run as administrator",
    echo OR run the standalone server by double-clicking "run.bat".
)
pause

