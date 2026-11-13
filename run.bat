@echo off
title CivicSphere - Smart Citizen Complaint Management
echo =====================================================================
echo   Starting CivicSphere Server...
echo =====================================================================
cd /d "%~dp0"

set "JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

set "CP=target\classes;C:\Program Files\Apache Software Foundation\Tomcat 11.0\lib\servlet-api.jar;C:\Users\manda\.m2\repository\com\google\code\gson\gson\2.9.0\gson-2.9.0.jar;C:\Users\manda\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar"

start "" http://localhost:8080/

"%JAVA_EXE%" -cp "%CP%" com.civicsphere.CivicSphereServer 8080
pause

