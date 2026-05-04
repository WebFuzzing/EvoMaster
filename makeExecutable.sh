#!/bin/bash

WINDOWS="--type msi --win-dir-chooser  --win-console"
OSX="--type dmg"
DEBIAN="--type deb"
JPACKAGE=jpackage

TAG=$1

if [ "$TAG" == "WINDOWS" ]; then
    OS=$WINDOWS
    JPACKAGE="jpackage.exe"
elif [ "$TAG" == "OSX" ]; then
    OS=$OSX
elif [ "$TAG" == "DEBIAN" ]; then
    OS=$DEBIAN
else
    echo Unrecognized tag "$TAG"
    exit 1
fi

VERSION=5.2.1

RELEASE=release
BUILD=build
JAR=evomaster.jar


rm -fr $RELEASE
mkdir -p $RELEASE
rm -fr $BUILD
mkdir -p $BUILD

cp core/target/$JAR $BUILD/$JAR

YEAR=`date +'%Y'`
COPYRIGHT="Copyright 2016-$YEAR EvoMaster Team"
VENDOR="EvoMaster Team"

$JPACKAGE --main-jar $JAR --input $BUILD --dest $RELEASE --name evomaster \
  --copyright "$COPYRIGHT" --license-file ./LICENSE --vendor "$VENDOR" --app-version $VERSION $OS \
  --java-options "--add-opens java.base/java.net=ALL-UNNAMED" \
  --java-options "--add-opens java.base/java.util=ALL-UNNAMED"
# In theory, should not need --add-opens here, as in theory handled inside manifest of uber jar.
# Unfortunately, it does not work when unpacking a DMG distribution without installing it.
# After pulling enough hair understanding WTF was going on, an easy workaround was just to force the
# --add-opens here as well.
# IMPORTANT: must be kept in sync with maven-shade-plugin in pom.xml and JdkIssue

if [ "$TAG" == "WINDOWS" ]; then
  echo TODO WINDOWS
elif [ "$TAG" == "OSX" ]; then
  # Need to convert, as no way in f*****g Mac you can install programmatically :(
  hdiutil convert release/evomaster-*.dmg -format UDTO -o evomaster.cdr
  hdiutil attach evomaster.cdr -nobrowse -noverify -noautoopen
  rm -fr pypi-distribution/src/evomaster/dmg
  cp -r /Volumes/evomaster pypi-distribution/src/evomaster/dmg
  hdiutil detach /Volumes/evomaster
  rm evomaster.cdr
elif [ "$TAG" == "DEBIAN" ]; then
  echo TODO DEBIAN
else
    echo Unrecognized tag "$TAG"
    exit 1
fi