#!/bin/sh
#
# Maven wrapper for this repository.
#
# A host that already provides a JDK and Maven is used unchanged. Bare CI
# sandboxes that provide neither get a pinned Temurin 17 JDK and Maven 3.9.9
# downloaded once into ${MVNW_HOME:-$HOME/.m2/evomaster-toolchain} and reused by
# later invocations. Nothing is written to the working tree.
#
set -eu

MAVEN_VERSION="3.9.9"
JDK_MAJOR="17"
TOOLCHAIN_HOME="${MVNW_HOME:-${HOME:-/tmp}/.m2/evomaster-toolchain}"

download() {
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL --retry 3 --retry-delay 2 --connect-timeout 20 -o "$2" "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$2" "$1"
    else
        python3 -c 'import sys, urllib.request; urllib.request.urlretrieve(sys.argv[1], sys.argv[2])' "$1" "$2"
    fi
}

extract() {
    # $1 = archive, $2 = destination; the archive holds a single top-level dir.
    if command -v tar >/dev/null 2>&1; then
        tar -xzf "$1" -C "$2" --strip-components=1
    else
        python3 - "$1" "$2" <<'PY'
import sys
import tarfile

with tarfile.open(sys.argv[1]) as archive:
    try:
        archive.extractall(sys.argv[2], filter="data")
    except TypeError:  # Python < 3.11.4 has no extraction filter argument.
        archive.extractall(sys.argv[2])
PY
    fi
}

resolve_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        printf '%s\n' "$JAVA_HOME"
        return 0
    fi
    if command -v java >/dev/null 2>&1; then
        java_bin=$(command -v java)
        dirname "$(dirname "$java_bin")"
        return 0
    fi
    return 1
}

resolve_maven_home() {
    if command -v mvn >/dev/null 2>&1; then
        mvn_bin=$(command -v mvn)
        dirname "$(dirname "$mvn_bin")"
        return 0
    fi
    return 1
}

provision_java_home() {
    jdk_root="$TOOLCHAIN_HOME/jdk-$JDK_MAJOR"
    if [ -x "$jdk_root/bin/java" ]; then
        printf '%s\n' "$jdk_root"
        return 0
    fi
    echo "mvnw: no JDK on this host, fetching Temurin $JDK_MAJOR" >&2
    mkdir -p "$TOOLCHAIN_HOME"
    archive="$TOOLCHAIN_HOME/jdk-$JDK_MAJOR.tar.gz"
    download "https://api.adoptium.net/v3/binary/latest/$JDK_MAJOR/ga/linux/x64/jdk/hotspot/normal/eclipse" "$archive"
    rm -rf "$jdk_root"
    mkdir -p "$jdk_root"
    extract "$archive" "$jdk_root"
    rm -f "$archive"
    printf '%s\n' "$jdk_root"
}

provision_maven_home() {
    maven_root="$TOOLCHAIN_HOME/apache-maven-$MAVEN_VERSION"
    if [ -x "$maven_root/bin/mvn" ]; then
        printf '%s\n' "$maven_root"
        return 0
    fi
    echo "mvnw: no Maven on this host, fetching Maven $MAVEN_VERSION" >&2
    mkdir -p "$TOOLCHAIN_HOME"
    archive="$TOOLCHAIN_HOME/apache-maven-$MAVEN_VERSION.tar.gz"
    download "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" "$archive"
    rm -rf "$maven_root"
    mkdir -p "$maven_root"
    extract "$archive" "$maven_root"
    rm -f "$archive"
    printf '%s\n' "$maven_root"
}

JAVA_HOME=$(resolve_java_home || provision_java_home)
export JAVA_HOME
MAVEN_HOME=$(resolve_maven_home || provision_maven_home)

exec "$MAVEN_HOME/bin/mvn" "$@"
