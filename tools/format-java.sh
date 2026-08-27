#!/usr/bin/env bash
#
# Formats Java files with the exact google-java-format that `./mvnw spotless:apply`
# uses, so what the editor does on save and what `./mvnw verify` demands cannot drift.
#
# The version is read from pom.xml, so bumping the Spotless config is the only edit
# needed. The jar is fetched into the local Maven repository on first use.
#
# Usage: tools/format-java.sh <file.java> [more.java ...]

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

version="$(sed -n '/<googleJavaFormat>/,/<\/googleJavaFormat>/s#.*<version>\(.*\)</version>.*#\1#p' \
  "$repo_root/pom.xml")"
if [ -z "$version" ]; then
  echo "format-java: could not read the google-java-format version from pom.xml" >&2
  exit 1
fi

repo="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
jar="$repo/com/google/googlejavaformat/google-java-format/$version/google-java-format-$version-all-deps.jar"

if [ ! -f "$jar" ]; then
  echo "format-java: fetching google-java-format $version..." >&2
  "$repo_root/mvnw" -q dependency:get \
    -Dartifact="com.google.googlejavaformat:google-java-format:$version:jar:all-deps" >&2
fi

exec java -jar "$jar" --replace "$@"
