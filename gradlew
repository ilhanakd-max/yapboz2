#!/usr/bin/env sh

DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "Downloading Gradle wrapper..."
  mkdir -p "$DIR/gradle/wrapper"
  curl -L "https://services.gradle.org/distributions/gradle-8.7-bin.zip" -o /tmp/gradle.zip
  unzip -p /tmp/gradle.zip gradle-8.7/bin/gradle > /tmp/gradle
  chmod +x /tmp/gradle
  /tmp/gradle wrapper --gradle-version 8.7 --distribution-type bin
fi

java -classpath "$GRADLE_WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
