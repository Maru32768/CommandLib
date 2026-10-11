#!/bin/bash
# Locate and extract CommandLib sources JAR to /tmp/commandlib-sources/.
# Prints the JAR path on success, or "LOCAL_BUILD" if no JAR is found.

JAR=$(find ~/.gradle/caches/modules-2/files-2.1/com.github.Maru32768.CommandLib \
  -name "bukkit-*-sources.jar" 2>/dev/null | sort | tail -1)

if [ -z "$JAR" ]; then
  echo "LOCAL_BUILD"
  exit 0
fi

mkdir -p /tmp/commandlib-sources
unzip -qo "$JAR" -d /tmp/commandlib-sources
echo "$JAR"
