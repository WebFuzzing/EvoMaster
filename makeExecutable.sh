#!/bin/bash

WINDOWS="--license-file ./LICENSE --type msi --win-dir-chooser  --win-console"
OSX="--license-file ./LICENSE --type dmg"
DEBIAN="--license-file ./LICENSE --type deb"
APP="--type  app-image"
APPWIN="--type  app-image --win-console"
JPACKAGE=jpackage

TAG=$1

if [ "$TAG" == "WINDOWS" ]; then
    OS=$WINDOWS
    JPACKAGE="jpackage.exe"
elif [ "$TAG" == "OSX" ]; then
    OS=$OSX
elif [ "$TAG" == "DEBIAN" ]; then
    OS=$DEBIAN
elif [ "$TAG" == "APP" ]; then
    OS=$APP
elif [ "$TAG" == "APPWIN" ]; then
    OS=$APPWIN
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
  --copyright "$COPYRIGHT"  --vendor "$VENDOR" --app-version $VERSION $OS \
  --java-options "--add-opens java.base/java.net=ALL-UNNAMED" \
  --java-options "--add-opens java.base/java.util=ALL-UNNAMED"
# In theory, should not need --add-opens here, as in theory handled inside manifest of uber jar.
# Unfortunately, it does not work when unpacking an app-image distribution without installing it.
# After pulling enough hair understanding WTF was going on, an easy workaround was just to force the
# --add-opens here as well.
# IMPORTANT: must be kept in sync with maven-shade-plugin in pom.xml and JdkIssue

if [[ "$TAG" == "APP" || "$TAG" == "APPWIN" ]]; then
  rm -fr pypi-distribution/src/evomaster/release
  cp -r release pypi-distribution/src/evomaster/release
fi

