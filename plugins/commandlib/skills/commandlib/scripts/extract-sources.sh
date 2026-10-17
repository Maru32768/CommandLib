#!/bin/bash
# Locate and extract CommandLib sources JARs to /tmp/commandlib-sources/.
# Usage: extract-sources.sh [artifact] [version]
#   artifact: spigot, paper, velocity, forge-1.20.1, ...; default spigot
#   version:  the CommandLib version the project depends on; default the newest one in the Gradle cache
# Prints the platform JAR path on success, or "NOT_FOUND" if no JAR is found.

ARTIFACT=${1:-spigot}
VERSION=${2:-*}
CACHE=~/.gradle/caches/modules-2/files-2.1/com.github.Maru32768.CommandLib

# The cache layout is <artifact>/<version>/<hash>/<artifact>-<version>-sources.jar, so a version sort of the paths
# orders by version.
newest() {
  find "$CACHE/$1" -name "$1-$2-sources.jar" 2>/dev/null | sort -V | tail -1
}

JAR=$(newest "$ARTIFACT" "$VERSION")

if [ -z "$JAR" ]; then
  echo "NOT_FOUND"
  exit 0
fi

rm -rf /tmp/commandlib-sources
mkdir -p /tmp/commandlib-sources
unzip -qo "$JAR" -d /tmp/commandlib-sources
# Platform modules build on the shared common module, published as its own artifact of the same version.
COMMON=$(newest common "$(basename "$(dirname "$(dirname "$JAR")")")")
[ -n "$COMMON" ] && unzip -qo "$COMMON" -d /tmp/commandlib-sources
echo "$JAR"
