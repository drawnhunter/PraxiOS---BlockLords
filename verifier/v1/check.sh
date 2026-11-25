#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
git diff --check
export JAVA_HOME=/tmp/jdk21
export PATH=/tmp/jdk21/bin:$PATH
/tmp/jdk21/bin/java -version
/tmp/gradle-8.10.2/bin/gradle clean build --no-daemon
