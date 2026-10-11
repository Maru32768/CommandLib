#!/bin/bash
# Locate and extract CommandLib sources JARs to /tmp/commandlib-sources/.
# Usage: extract-sources.sh [artifact]  (spigot, paper, forge-1.20.1, ...; default spigot)
# Prints the platform JAR path on success, or "LOCAL_BUILD" if no JAR is found.

ARTIFACT=${1:-spigot}
CACHE=~/.gradle/caches/modules-2/files-2.1/com.github.Maru32768.CommandLib

latest() {
  find "$CACHE" -name "$1-*-sources.jar" ! -name "*-testing-*" 2>/dev/null | sort | tail -1
}

JAR=$(latest "$ARTIFACT")

if [ -z "$JAR" ]; then
  echo "LOCAL_BUILD"
  exit 0
fi

rm -rf /tmp/commandlib-sources
mkdir -p /tmp/commandlib-sources
unzip -qo "$JAR" -d /tmp/commandlib-sources
# Platform modules build on the shared common module, published as its own artifact.
COMMON=$(latest common)
[ -n "$COMMON" ] && unzip -qo "$COMMON" -d /tmp/commandlib-sources
echo "$JAR"
