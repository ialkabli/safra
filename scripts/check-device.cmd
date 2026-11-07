@echo off
setlocal
cd /d "%~dp0.."
call gradlew.bat assembleDebug assembleDebugAndroidTest
if errorlevel 1 exit /b 1
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
"%ANDROID_HOME%\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 exit /b 1
"%ANDROID_HOME%\platform-tools\adb.exe" install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
if errorlevel 1 exit /b 1
"%ANDROID_HOME%\platform-tools\adb.exe" shell am instrument -w com.iprigner.safra.test/com.iprigner.safra.AppTests
