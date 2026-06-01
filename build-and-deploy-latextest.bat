@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java 17 was not found at "%JAVA_HOME%".
    echo Edit JAVA_HOME in this file if your JDK 17 is installed somewhere else.
    pause
    exit /b 1
)

call "%~dp0gradlew.bat" --no-daemon deployLatexTest
if errorlevel 1 (
    echo Build or deploy failed.
    pause
    exit /b 1
)

echo Built and deployed ChangedSurviveProtocol to the LatexTest mods folder.
pause
