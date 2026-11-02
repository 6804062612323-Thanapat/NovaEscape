@echo off
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
echo Compiling Nova Escape...
javac -encoding UTF-8 -d out -sourcepath src src\novaescape\Main.java
if errorlevel 1 (
  echo.
  echo Compile failed. Please install a JDK ^(Java 8 or newer^) and make sure javac is in PATH.
  pause
  exit /b 1
)
echo Starting game...
java -cp out novaescape.Main
