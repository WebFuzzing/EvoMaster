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

VERSION=3.0.1

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
  --java-options "--add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
# needs to be kept in sync with JdkIssue