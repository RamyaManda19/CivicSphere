@echo off
title Build CivicSphere WAR
echo =====================================================================
echo   Compiling and Packaging CivicSphere.war
echo =====================================================================
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"

set "CP=C:\Program Files\Apache Software Foundation\Tomcat 11.0\lib\servlet-api.jar;C:\Users\manda\.m2\repository\com\google\code\gson\gson\2.9.0\gson-2.9.0.jar;C:\Users\manda\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar"

if not exist "target\classes" mkdir target\classes
if not exist "target\war-staging\WEB-INF\classes" mkdir target\war-staging\WEB-INF\classes
if not exist "target\war-staging\WEB-INF\lib" mkdir target\war-staging\WEB-INF\lib

echo [1/4] Compiling Java classes...
powershell -Command "$src = Get-ChildItem -Path 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }; & '%JAVAC%' -encoding UTF-8 -cp '%CP%' -d 'target\classes' $src"

echo [2/4] Copying resources and static webapp files...
xcopy /E /I /Y "src\main\resources\*" "target\classes\"
xcopy /E /I /Y "src\main\webapp\*" "target\war-staging\"
xcopy /E /I /Y "target\classes\*" "target\war-staging\WEB-INF\classes\"

echo [3/4] Bundling dependencies...
copy /Y "C:\Users\manda\.m2\repository\com\google\code\gson\gson\2.9.0\gson-2.9.0.jar" "target\war-staging\WEB-INF\lib\"
copy /Y "C:\Users\manda\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar" "target\war-staging\WEB-INF\lib\"

echo [4/4] Creating CivicSphere.war...
cd target\war-staging
"%JAR%" -cvf "..\CivicSphere.war" *
cd ..\..

echo.
echo =====================================================================
echo   SUCCESS: CivicSphere.war is ready in target\CivicSphere.war
echo =====================================================================
pause

