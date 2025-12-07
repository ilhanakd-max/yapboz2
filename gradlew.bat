@echo off
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
if not exist %WRAPPER_JAR% (
  echo Downloading Gradle wrapper...
  powershell -Command "Invoke-WebRequest -OutFile %TEMP%\gradle.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip"
  powershell -Command "Expand-Archive %TEMP%\gradle.zip %TEMP%\gradle"
  %TEMP%\gradle\gradle-8.7\bin\gradle.bat wrapper --gradle-version 8.7 --distribution-type bin
)
java -classpath %WRAPPER_JAR% org.gradle.wrapper.GradleWrapperMain %*
