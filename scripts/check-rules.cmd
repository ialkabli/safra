@echo off
setlocal
cd /d "%~dp0.."
if not defined JAVA_HOME (echo Set JAVA_HOME to your JDK directory first. & exit /b 1)
if not exist build\rule-checks mkdir build\rule-checks
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 --release 17 -d build\rule-checks app\src\main\java\com\iprigner\safra\Rules.java tests\RuleChecks.java
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\rule-checks com.iprigner.safra.RuleChecks
