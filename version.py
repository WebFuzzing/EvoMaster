#!/usr/bin/env python

import sys
import re
import platform
import os
from subprocess import run

if len(sys.argv) != 2:
    print("Usage:\n<nameOfScript>.py <version-number>")
    exit(1)

version = sys.argv[1].strip()

versionRegex = re.compile(r"^(\d)+\.(\d)+\.(\d)+(-SNAPSHOT)?$")

if versionRegex.match(version) == None:
    print("Invalid version format")
    exit(1)


# release versions for msi/deb/dmg do not like SNAPSHOT
reducedVersion = version
if reducedVersion.endswith("-SNAPSHOT"):
    reducedVersion = version[0:(len(version)-len("-SNAPSHOT"))]


def replace(file, regex, replacement):

    found=0

    with open(file, "r") as sources:
        lines = sources.readlines()
    with open(file, "w") as sources:
        for line in lines:

            if regex.match(line):
                found = found + 1
                sources.write(replacement)
            else:
                sources.write(line)
    if found != 1:
        print("Regex " + str(regex) + " has been matched " + str(found) + " times")
        exit(1)

def replaceInMakeExecutable():
    regex = re.compile(r'.*VERSION.*=.*')
    replacement = 'VERSION='+reducedVersion+'\n'
    replace("makeExecutable.sh", regex, replacement)

def replaceInCI():
    regex = re.compile(r'  evomaster-version:.*')
    replacement = '  evomaster-version: '+reducedVersion+'\n'
    replace(".github/workflows/ci.yml", regex, replacement)
    replace(".github/workflows/release.yml", regex, replacement)

def replaceInBBE2E():
    regex = re.compile(r'\s*<version>.*</version><!--MARKER-->\s*')
    replacement = '        <version>'+version+'</version><!--MARKER-->\n'
    replace("e2e-tests/spring-rest-bb/maven/pom.xml", regex, replacement)

### JS and C# no longer supported
# def replaceInJS():
#     regex = re.compile(r'\s*"version"\s*:.*')
#     replacement = '  "version": "'+version+'",\n'
#     replace("client-js/evomaster-client-js/package.json", regex, replacement)
#
# def replaceInCS():
#     regex = re.compile(r'\s*<Version>.*</Version>\s*')
#     replacement = '         <Version>'+version+'</Version>\n'
#     replace("client-dotnet/common.props", regex, replacement)
#
#     regex = re.compile(r'.*VERSION.*=.*')
#     replacement = 'VERSION='+reducedVersion+'\n'
#     replace("client-dotnet/publish.sh", regex, replacement)


SHELL = platform.system() == 'Windows'

def replaceInMvn():
    mvnres = run(["mvn", "versions:set", "-DnewVersion="+version], shell=SHELL)
    mvnres = mvnres.returncode

    if mvnres != 0:
        print("\nERROR: Maven command failed")
        exit(1)


replaceInMakeExecutable()
replaceInCI()
replaceInMvn()
replaceInBBE2E()
### No longer supported
# replaceInCS()
# replaceInJS()
