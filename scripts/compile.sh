#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' -print > build/main-sources.txt
javac -encoding UTF-8 -d build/classes @build/main-sources.txt

if find src/test/java -name '*.java' -print | grep -q .; then
  find src/test/java -name '*.java' -print > build/test-sources.txt
  javac -encoding UTF-8 -cp build/classes -d build/test-classes @build/test-sources.txt
fi

