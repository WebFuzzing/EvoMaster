package org.evomaster.resource.rest.generator

import java.nio.file.Files
import java.nio.file.Paths

/**
 * created by manzh on 2020-01-03
 */
object Util {

    fun generateDeployScript(config: List<GenConfig>, toolVersion : String, rootfolder : String){
        val scriptTemplate = """
#!/usr/bin/env python3

EVOMASTER_VERSION = "$toolVersion"

import os
import shutil
import platform
from shutil import copy
from subprocess import run
from os.path import expanduser


HOME = expanduser("~")
SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))
PROJ_LOCATION = os.path.abspath(os.path.join(SCRIPT_LOCATION, os.pardir))

if  platform.system() == 'Windows':
    mvnres = run(["mvn", "clean", "install", "-DskipTests"], cwd=PROJ_LOCATION, shell=True)
else:
    mvnres = run(["mvn", "clean", "install", "-DskipTests"], cwd=PROJ_LOCATION)
    mvnres = mvnres.returncode

if mvnres != 0:
    print("\nERROR: Maven command failed")
    exit(1)

dist = os.path.join(PROJ_LOCATION, "dist")

if os.path.exists(dist):
    shutil.rmtree(dist)

os.mkdir(dist)

${config.map {
            listOf(
                    "copy(\"${it.projectName}/${it.csName}/target/${it.getCSJarName()}.jar\", dist)",
                    "copy(\"${it.projectName}/${it.exName}/target/${it.getEXJarFinalName()}.jar\", dist)",
                    ""
            ).joinToString(System.lineSeparator())
        }.joinToString(System.lineSeparator())}

copy(HOME + "/.m2/repository/org/evomaster/evomaster-client-java-instrumentation/"
   + EVOMASTER_VERSION + "/evomaster-client-java-instrumentation-"
   + EVOMASTER_VERSION + ".jar",
   os.path.join(dist, "evomaster-agent.jar"))

zipName = "dist.zip"
if os.path.exists(zipName):
    os.remove(zipName)

print("Creating " + zipName)
shutil.make_archive(base_name=dist, format='zip', root_dir=dist+"/..", base_dir='dist')
            """
        val folder ="$rootfolder/pyscript"
        Files.createDirectories(Paths.get(folder))
        Files.write(Paths.get("$folder/dist.py"), scriptTemplate.toByteArray())
    }
}