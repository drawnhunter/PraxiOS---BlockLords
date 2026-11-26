#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
git diff --check
if [ -z "${JAVA_HOME:-}" ]; then
    export JAVA_HOME=/tmp/jdk21
fi
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build --no-daemon
