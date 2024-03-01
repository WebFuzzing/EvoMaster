#!/usr/bin/env python
import math
import sys
import os
import shutil
import stat
import pathlib
from pathlib import PureWindowsPath, PurePosixPath
import platform
import io
## use to write configuration file for RestTestGenV2
import json

if len(sys.argv) < 6 or len(sys.argv) > 8:
    print("Usage:\n<nameOfScript>.py <basePort> <dir> <minSeed> <maxSeed> <maxTimeSeconds> <sutFilter?> <toolFilter?>")
    exit(1)


# TCP port bindings will be based on such port.
# If running new experiments while some previous are still running, to avoid TCP port
# conflict, can use an higher base port. Each run reserves 10 ports. So, if you run
# 500 jobs with starting port 10000, you will end up using ports up to 15000
BASE_PORT= int(sys.argv[1])

# When creating a new set of experiments, all needed files will be saved in a folder
BASE_DIR = os.path.abspath(sys.argv[2])

# Experiments are repeated a certain number of times, with different seed for the
# random generator. This specify the starting seed.
MIN_SEED = int(sys.argv[3])

# Max seed, included. For example, if running min=10 and max=39, each experiment is
# going to be repeated 30 times, starting from seed 10 to seed 39 (both included).
MAX_SEED = int(sys.argv[4])

# By default, experiments on BB are run with time as stopping criterion
MAX_TIME_SECONDS  = int(sys.argv[5])

#
# An optional string to filter SUTs to be included based on their names
# A string could refer to multiple SUTs separated by a `,` like a,b
# Note that
# None or `all` represents all SUTs should be included
# and only consider unique ones, eg, create one experiment setting for a,a
# Default is None
SUTFILTER = None
if len(sys.argv) > 6:
    SUTFILTER = str(sys.argv[6])

# An optional string to filter bb tools to be included based on their names
# A string could refer to multiple tools separated by a `,` like a,b
# Note that
# None or `all` represents all tools should be included
# and only consider unique ones, eg, create one experiment setting for a,a
# Default is None
TOOLFILTER = None
if len(sys.argv) > 7:
    TOOLFILTER = str(sys.argv[7])

## Default setting: Configure whether to enable auth configuration
ENABLE_AUTH_CONFIG = True

## Default setting: Configure whether to enable run cmd for globally handling timeout for each tool
ENABLE_TIMEOUT_RUM_CMD = True
ENABLE_TIMEOUT_RUM_CMD_VAR = "i"

## Default setting: Configure whether to fix basic issue in schema in order to apply the tool
FIX_BASIC_SCHEMA_ISSUE = True

## configure auth file
AUTH_DIR = os.path.abspath("authconfig")

class AuthInfo:
    def __init__(self, key, value):
        self.key = key
        self.value = value

class PlatformSetup:
    def __init__(self, platform, dir):
            self.platform = platform
            self.dir = dir

JVM = "JVM"
NODEJS = "NODEJS"

# a prefix for indicating local schema
LOCAL_SCHEMA_PREFIX="local:"

class Sut:
    def __init__(self, name, endpointPath, openapiName, baseURL, authInfo, runtime):
        self.name = name
        self.endpointPath = endpointPath
        ## baseline techniques need the schema which is on the local
        self.openapiName = openapiName
        ## base URL to process path in the schema
        self.baseURL = baseURL
        ## auth configuration in header
        self.authInfo = authInfo
        ## eg either JVM or NODEJS
        self.runtime = runtime


SUTS = [
    # REST JVM
    Sut("catwatch", "/v2/api-docs", "openapi.json", "", None, JVM),
    Sut("cwa-verification", LOCAL_SCHEMA_PREFIX+"$BASE/local-schema/cwa-verification/api-docs.json", "openapi.json", "", None, JVM), # not exporting schema. TODO
    Sut("features-service", "/swagger.json", "openapi.json","", None, JVM),
    Sut("languagetool", "/v2/swagger", "openapi.json", "/v2",None, JVM),
    Sut("ocvn-rest", "/v2/api-docs?group=1ocDashboardsApi", "openapi.json", "",None, JVM), # TODO auth
    Sut("proxyprint", "/v2/api-docs", "openapi.json", "",AuthInfo("Authorization","Basic bWFzdGVyOjEyMzQ="), JVM),
    Sut("rest-ncs", "/v2/api-docs", "openapi.json", "", None, JVM),
    Sut("rest-news", "/v2/api-docs", "openapi.json", "", None, JVM),
    Sut("rest-scs", "/v2/api-docs", "openapi.json", "", None, JVM),
    Sut("restcountries", "/openapi.yaml", "openapi.yaml", "/rest", None, JVM),
    Sut("scout-api", "/api/swagger.json", "openapi.json", "/api", AuthInfo("Authorization","ApiKey administrator"), JVM),
    Sut("gestaohospital-rest","/v2/api-docs", "openapi.json", "", None, JVM),
    # REST JVM ind0
    ## Note that this is an industrial case study, and it is not available in replication package
    Sut("ind0","/v2/api-docs", "openapi.json", "", None, JVM),
    # REST NodeJS
    Sut("js-rest-ncs","/swagger.json","swagger.json","", None, NODEJS),
    Sut("js-rest-scs","/swagger.json","swagger.json","", None, NODEJS),
    Sut("cyclotron","/swagger.json","swagger.json","", None, NODEJS),
    Sut("disease-sh-api","/apidocs/swagger_v3.json","swagger_v3.json","", None, NODEJS),
    Sut("realworld-app","/swagger.json","swagger.json","/api", None, NODEJS), # TODO auth
    Sut("spacex-api","/openapi.json","openapi.json", "/v4", AuthInfo("Authorization","spacex-key foo"), NODEJS),
]

if SUTFILTER is not None and SUTFILTER.lower() != "all":
    filteredsut = []
    unfound = []

    for s in list(set(SUTFILTER.split(","))):
        found = list(filter(lambda x: x.name.lower() == s.lower(), SUTS))
        if len(found) == 0:
            print("ERROR: cannot find the specified sut "+s)
            exit(1)
        filteredsut.extend(found)
    SUTS = filteredsut

# where the script for sut setup could be found based on platforms
SUTS_SETUP = [
    PlatformSetup(JVM, "jvm-suts"),
    PlatformSetup(NODEJS, "js-suts")
]

# input parameter validation
if MIN_SEED > MAX_SEED:
    print("ERROR: min seed is greater than max seed")
    exit(1)

if not os.path.isdir(BASE_DIR):
    print("creating folder: " + BASE_DIR)
    os.makedirs(BASE_DIR)
else:
    print("ERROR: target folder already exists")
    exit(1)


TMP=BASE_DIR+"/tmp"
SCRIPT_DIR=BASE_DIR+"/scripts"
LOGS=BASE_DIR+"/logs"
TESTS=BASE_DIR+"/tests"

IS_WINDOWS = platform.system() == 'Windows'

### configure python
PYTHON_COMMAND = "python3"
if IS_WINDOWS:
    PYTHON_COMMAND = "python"

### Java
JAVA_HOME_8 = os.environ.get("JAVA_HOME_8", "")
if JAVA_HOME_8 == "":
    print("ERROR: cannot find JAVA_HOME_8")
    exit(1)
JAVA_8_COMMAND = "\"" + JAVA_HOME_8 + "\"/bin/java"

JAVA_HOME_11 = os.environ.get("JAVA_HOME_11", "")
if JAVA_HOME_11 == "":
    print("ERROR: cannot find JAVA_HOME_11, and it is needed for RestTestGen")
    exit(1)
JAVA_11_COMMAND = "\"" + JAVA_HOME_11 + "\"/bin/java"



############################################################################
### evomaster blackBox
###     see https://github.com/EMResearch/EvoMaster
############################################################################
BB_EVOMASTER = "evomaster_bb_v2"

############################################################################
### Restler
###     follow https://github.com/microsoft/restler-fuzzer to install it
###     then configure the bin folder where Restler is (`RESTLER_DIR`)
###     see https://github.com/microsoft/restler-fuzzer/blob/main/docs/user-guide/Telemetry.md
###     Set the RESTLER_TELEMETRY_OPTOUT environment variable to 1 or true.
############################################################################
BB_RESTLER = "Restler"
RESTLER_START_SCRIPT_DIR = "tools/baseline-restler-exp.py"
RESTLER_START_SCRIPT = "baseline-restler-exp.py"
RESTLER_DIR = os.environ.get("RESTLER_DIR", "")
# RESTLER_DIR = "$BASE/tools/restler_bin"

############################################################################
### RestCT
###     follow https://github.com/GIST-NJU/RestCT to build it
###     the tool does not work on windows,
###     then need to employ wsl to start
### if the experiment will be conducted in Windows, then WSL is needed
### `WSL_ACCESS_DIR` is to configure where the output could be accessed from the host machine
### `WSL_RESULT_DIR` is to configure where to save the output (on WSL or locally)
### `RESTCT_DIR` is to configure where the restct is located (ie, restct.py) on WSL or local machine
############################################################################
BB_RESTCT = "RestCT"
WSL = "wsl -d Ubuntu-20.04"
WSL_ACCESS_DIR = os.environ.get("WSL_ACCESS_DIR", "")
if IS_WINDOWS and WSL_ACCESS_DIR == "":
    print("ERROR: cannot find WSL_ACCESS_DIR")
    ## TODO FIXME: info on how to setup this variable
    exit(1)
WSL_ACCESS_DIR = WSL_ACCESS_DIR+"/results"
WSL_RESULT_DIR = "~/results"
RESTCT_DIR = "~/github/RestCT/src/restct.py"


############################################################################
### RestTestGen
###     nominal tester and error tester
############################################################################
BB_RESTTESTGEN = "RestTestGen"
RESTTESTGEN_NOMINAL_SWAGGER_CLIENT = "$BASE/tools/resttestgen/tools/swagger-codegen/swagger-codegen-cli.jar"
RESTTESTGEN_NOMINAL_TESTER = "$BASE/tools/resttestgen/resttestgen-1.0-SNAPSHOT.jar"
RESTTESTGEN_ERROR_TESTER = "$BASE/tools/resttestgen/error-tester.jar"

############################################################################
### RestTestGen v2
###     https://github.com/SeUniVr/RestTestGen
### note that for this tool, it requires to load a configuration file which
### should be under the same level with the jar of this tool, then
### RESTTESTGEN_V2_DIR is required to refer to its actual absolute path
############################################################################
BB_RESTTESTGENV2 = "RestTestGenV2"
RESTTESTGEN_V2_JAR = "resttestgen-framework-fat-1.0-SNAPSHOT.jar"
RESTTESTGEN_V2_DIR= os.path.abspath("tools")#"$BASE/tools/"

############################################################################
### RestTest
###     the tool is more than 1G, then
###     follow https://github.com/isa-group/RESTest to compile it with .zip
############################################################################
BB_RESTEST = "RestTest"
RESTEST_DIR = os.environ.get("RESTEST_DIR", "")
# RESTEST_DIR = "$BASE/tools/RESTest-restest-1.2.0/RESTest-restest-1.2.0/target"
RESTEST_JAR = "restest-full.jar"
RESTTEST_CREATE_TESTCONF_CLASS = "es.us.isa.restest.main.CreateTestConf"

############################################################################
### bBOXRT
###     https://git.dei.uc.pt/cnl/bBOXRT/tree/master
###     follow the README to build the tool
###     note the jar could not be used directly due to some unresolved
###     dependency. in order to use it, configure BBOXRT_DIR to the `target`
############################################################################
BB_BBOXRT = "bBOXRT"
BBOXRT_DIR = "$BASE/tools/bBOXRT/bBOXRT.git/target"
BBOXRT_JAR = "REST_API_Robustness_Tester-1.0.jar"

############################################################################
### Schemathesis
###     https://github.com/schemathesis/schemathesis
###     install with pip install schemathesis
############################################################################
BB_SCHEMATHESIS = "Schemathesis"
SCHEMATHESIS_CMD = "schemathesis run"

############################################################################
### in order to apply bb tools
###     there might need a further handling in inputs,
###     eg, set auth, modify the port, add schemes/server, modify the format
###     then we developed such utility which includes
###         - authForResTest <testConfig.yaml path> <key> <value>
###         - jsonToYaml <openapi path>
###         - updateURLAndPort <openapi path> <port>
############################################################################
BB_EXP_UTIL = "$BASE/util/bb-exp-util.jar"
TIMEOUT_RUN_CMD_SCRIPT="util/run_cmd.sh"

BB_TOOLS = [BB_EVOMASTER, BB_RESTLER, BB_RESTCT, #BB_RESTTESTGEN,
            BB_RESTTESTGENV2, BB_RESTEST, BB_BBOXRT, BB_SCHEMATHESIS]

## copy timeout run cmd script to script folder
# if ENABLE_TIMEOUT_RUM_CMD:
#     shutil.copy(pathlib.PurePath(TIMEOUT_RUN_CMD_SCRIPT).as_posix(), BASE_DIR)

if TOOLFILTER is not None and TOOLFILTER.lower() != "all":
    filteredtools = []
    unfound = []

    for s in list(set(TOOLFILTER.split(","))):
        found = list(filter(lambda x: x.lower() == s.lower(), BB_TOOLS))
        if len(found) == 0:
            print("ERROR: cannot find the specified sut "+s)
            exit(1)
        filteredtools.extend(found)
    BB_TOOLS = filteredtools


def writeScript(code, port, tool, sut):
    script_path = SCRIPT_DIR + "/" + tool  + "_" + sut.name + "_" + str(port) + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

    return script

def getScriptHead(port,tool,sut):
    s = ""
    s += "#!/bin/bash \n"

    s += 'SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd ) \n'
    s += "BASE=$SCRIPT_DIR/../.. \n"
    s += "PORT=" + str(port) + " \n"
    label = sut.name+"__"+tool+"__\"$PORT\""

    if ENABLE_TIMEOUT_RUM_CMD:
        s += "\nsource $BASE/util/run_cmd.sh\n\n"

    sut_dir = findSutScriptDir(sut.runtime)
    command = "bash $BASE/"+sut_dir+"/"+sut.name+".sh"
    redirection = "> \""+LOGS+"/sut__"+sut.name+"__"+tool+"__$PORT.txt\" 2>&1 & \n"

    if sut.runtime == JVM:
        s += "# JaCoCo does not like full paths or exec in Windows/GitBash format... but relative path seems working \n"
        # strange it does not want  ./"+sys.argv[2] before the exec
        inputs =  " $PORT $BASE/tools/jacocoagent.jar  ./exec/"+label+"__jacoco.exec "
    else :
        inputs = " $PORT $SCRIPT_DIR/../c8/"+label
    s += command + inputs + redirection

    if sut.runtime == JVM:
        s += "PID=$! \n"
        s += "# this works on GitBash/Windows... but very brittle \n"
        s += "sleep 120 \n"  # OCVN can be very sloooooow to start, eg 45s on my laptop
        # s += "PGID=$( ps  -p $PID | tail -n +2 | cut -c18-30 | xargs) \n" # this gives too many issues
        s += "CHILD=$( ps | cut -c1-20 | grep $PID | cut -c1-10 | grep -v $PID | xargs) \n"
        s += " \n"
    else:
        s += "sleep 60 \n"

    return s

## find a dir where save sut scripts based on the specified platform
def findSutScriptDir(platform):
    found = list(filter(lambda x: x.platform.lower() == platform.lower(), SUTS_SETUP))
    if len(found) == 0 or len(found) > 1:
        print("ERROR: 0 or multiple (>1) dir are found based on "+platform)
        exit(1)
    return found[0].dir

def getScriptFooter(port,tool,sut):
    s = ""
    s += "\n"
    if sut.runtime == JVM:
        s += "# with default kill signal, JaCoCo not generated. we need a SIGINT (code 2)\n"
        #s += "kill -n 2 -- -$PGID\n" # this kills everything, even other scripts started with schedule.py
        # s += "echo Going to kill child process [$CHILD] of parent [$PID] \n" #logging
        s += "kill -n 2 $CHILD\n"
        #s += "kill -n 2 $PID\n" # might not be necessary
    else :
        # see issue at: https://github.com/bcoe/c8/issues/166
        # added /shutdown endpoint to all SUTs
        s += "curl -X POST http://localhost:$PORT/shutdown\n"
    s += "\n"
    return s

def createScriptForEvoMaster(sut, port, seed):
    tool = BB_EVOMASTER

    evomaster_bb_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" + str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    command = downloadOpenApi(sut.endpointPath, openapi_path, evomaster_bb_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, evomaster_bb_log)

    ## start_tool
    start_tool_command = JAVA_8_COMMAND+" -Xms1G -Xmx4G -jar $BASE/tools/evomaster.jar"
    start_tool_command += " --blackBox true"
    start_tool_command += " --maxTime " + str(MAX_TIME_SECONDS) + "s"
    if sut.endpointPath.startswith(LOCAL_SCHEMA_PREFIX):
        prefix = "file://"
        if not openapi_path.startswith("/"):
            prefix += "/"
        start_tool_command += " --bbSwaggerUrl \"" + prefix + openapi_path + "\""
    else:
        start_tool_command += " --bbSwaggerUrl http://localhost:$PORT" + sut.endpointPath
    start_tool_command += " --bbTargetUrl http://localhost:$PORT"
    start_tool_command += " --seed " + str(seed)
    start_tool_command += " --showProgress=false"
    start_tool_command += " --testSuiteSplitType=NONE"

    if ENABLE_TIMEOUT_RUM_CMD:
        start_tool_command += " --outputFilePrefix=EM_BB_R$"+ ENABLE_TIMEOUT_RUM_CMD_VAR

    if sut.runtime == JVM :
        start_tool_command += " --outputFormat JAVA_JUNIT_5"
    else :
        start_tool_command += " --outputFormat JS_JEST"

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        start_tool_command += " --header0 \"" + sut.authInfo.key + ": " + sut.authInfo.value+"\""

    start_tool_command += " --outputFolder \""+result_folder+"\""
    start_tool_command += " >> \""+evomaster_bb_log+"\" 2>&1"
    start_tool_command += "\n"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

######################## baseline tools start ########################################
def createScriptForRestler(sut, port, seed):
    tool = BB_RESTLER

    # copy python script to the exp folder
    shutil.copy(pathlib.PurePath(RESTLER_START_SCRIPT_DIR).as_posix(), BASE_DIR)

    restler_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)
    # restler employ hour as the unit
    time_budget = math.ceil(MAX_TIME_SECONDS / 36) / 100
    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    command = downloadOpenApi(sut.endpointPath, openapi_path, restler_log)

    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, restler_log)

    start_tool_command = PYTHON_COMMAND + " " + RESTLER_START_SCRIPT
    start_tool_command +=  " --api_spec_path " + openapi_path
    start_tool_command += " --port " + str(port)
    start_tool_command +=  " --restler_drop_dir " + pathlib.PurePath(RESTLER_DIR).as_posix()
    start_tool_command +=  " --time_budget " + str(time_budget)
    start_tool_command +=  " --result_dir " + result_folder

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        start_tool_command +=  " --token_refresh_cmd \""+PYTHON_COMMAND+" "+str(pathlib.PurePath(os.path.join(AUTH_DIR, tool+"_"+sut.name+".py")).as_posix()) +"\""

    start_tool_command +=  " >> " + restler_log + " 2>&1"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

def createScriptForRestCT(sut, port, seed):
    tool = BB_RESTCT

    restct_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    default_folder = createResultDir(sut.name, seed, port, tool)
    result_folder = default_folder
    result_actual_folder = default_folder
    if IS_WINDOWS:
        result_actual_folder = createResultDir(sut.name, seed, port, tool, WSL_ACCESS_DIR)
        result_folder = WSL_RESULT_DIR + "/" + resultDir(sut.name, seed, port, tool)

    # restCT use seconds as well
    time_budget = MAX_TIME_SECONDS
    default_openapi_path = pathlib.PurePath(os.path.join(default_folder, sut.openapiName)).as_posix()
    openapi_actual_path = default_openapi_path
    openapi_path = default_openapi_path
    if IS_WINDOWS:
        openapi_actual_path = result_actual_folder + "/" + sut.openapiName
        openapi_path = result_folder + "/" +sut.openapiName

    command = downloadOpenApi(sut.endpointPath, openapi_actual_path, restct_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_actual_path, port, restct_log)

    output_label = ""
    if ENABLE_TIMEOUT_RUM_CMD:
        output_label = "_R\"$" + ENABLE_TIMEOUT_RUM_CMD_VAR + "\""

    if IS_WINDOWS:
        start_tool_command =  WSL + " python3 \"" + RESTCT_DIR +"\""
    else:
        start_tool_command =  PYTHON_COMMAND + " " + RESTCT_DIR

    start_tool_command +=   " --swagger \"" + openapi_path +"\""
    start_tool_command +=   " --budget " + str(time_budget)
    start_tool_command +=   " --dir \"" + result_folder +"\""
    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        start_tool_command +=   " --header "+ "\"{\\\"" + sut.authInfo.key + "\\\": \\\"" + sut.authInfo.value + "\\\"}\""
    start_tool_command +=  " >> " + restct_log + " 2>&1 "
    if IS_WINDOWS:
        start_tool_command +=  "\n\nsleep 5\n"
        start_tool_command +=  "cp -r " + result_actual_folder + " " + pathlib.PurePath(TESTS).as_posix()

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)


# RestTestGen
def createScriptForRestTestGen(sut, port, seed):

    tool = BB_RESTTESTGEN

    swagger_client_log = pathlib.PurePath(
        LOGS + "/tool__" + sut.name + "__" + tool + "__swagger_client_generator__" + str(port) + ".txt").as_posix()
    nominal_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool  +"__nominal_tester__"+str(port) + ".txt").as_posix()
    error_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool +"__error_tester__"+str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)
    client_code_path = str(pathlib.PurePath(result_folder+"/output/codegen").as_posix())
    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()


    command = downloadOpenApi(sut.endpointPath, openapi_path, swagger_client_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, swagger_client_log)

    output_label = ""
    if ENABLE_TIMEOUT_RUM_CMD:
        output_label = "_R\"$" + ENABLE_TIMEOUT_RUM_CMD_VAR + "\""

    # Use swagger codegen to generate client-side code
    start_tool_command = JAVA_8_COMMAND + " -jar "+RESTTESTGEN_NOMINAL_SWAGGER_CLIENT+" generate -i " + openapi_path + " -l java -o " + client_code_path
    start_tool_command +=" >> " + swagger_client_log + " 2>&1 \n"

    # wait 2s
    start_tool_command += "\nsleep 2\n"

    # Compile generated classes
    start_tool_command += "(cd "+client_code_path+" && mvn clean package)"
    start_tool_command += " >> " + swagger_client_log + " 2>&1 \n"
    swagger_client_path = str(pathlib.PurePath(result_folder+"/output/codegen/target/swagger-java-client-1.0.0.jar").as_posix())
    # wait 2s
    start_tool_command += "\nsleep 2\n"

    start_tool_command += "\n\n"
    # run resttestgen
    start_tool_command += JAVA_8_COMMAND + " -jar " + RESTTESTGEN_NOMINAL_TESTER
    start_tool_command += " --output " + result_folder
    start_tool_command += " --classes " + swagger_client_path
    start_tool_command += " --swagger " + openapi_path
    start_tool_command += " >> " + nominal_log + " 2>&1 \n"

    start_tool_command +=  JAVA_8_COMMAND + " -jar " + RESTTESTGEN_ERROR_TESTER
    start_tool_command +=  " --service " + result_folder
    start_tool_command +=  " --reports " + result_folder + "/reports" + output_label
    start_tool_command += " --swagger " + openapi_path
    start_tool_command += " >> " + error_log + " 2>&1 \n"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

# RestTestGen v2 from https://github.com/SeUniVr/RestTestGen
def createScriptForRestTestGenV2(sut, port, seed):
    # RestTestGen-v2, but keep the name without the version info
    tool = BB_RESTTESTGENV2

    resttestgen_log = pathlib.PurePath(
        LOGS + "/tool__" + sut.name + "__" + tool + "__resttestgen_log__" + str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    # copy tool to result dir
    shutil.copy(pathlib.PurePath(os.path.join(RESTTESTGEN_V2_DIR, RESTTESTGEN_V2_JAR)), result_folder)
    # a path where the schema will be saved
    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    command = downloadOpenApi(sut.endpointPath, openapi_path, resttestgen_log)

    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, resttestgen_log, True, "json")
        is_yaml = sut.openapiName.endswith('.yaml')
        if is_yaml:
            openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName.replace(".yaml",".json"))).as_posix()

    # update the schema
    config_file = createConfigFileForRestTestGenV2(result_folder, openapi_path)

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        authForRestTestGenV2(sut, config_file)

    start_tool_command = "pushd \"" + result_folder + "\"\n"
    start_tool_command += JAVA_11_COMMAND + " -jar " + RESTTESTGEN_V2_JAR
    start_tool_command += " >> " + resttestgen_log + " 2>&1 "

    start_tool_command += "\npopd"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)


def createScriptForRestTest(sut, port, seed):
    tool = BB_RESTEST

    resttest_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    command = downloadOpenApi(sut.endpointPath, openapi_path, resttest_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, resttest_log)

    ## the file and path cannot be configured with command line, unless modify the source code
    testconfig_path = pathlib.PurePath(os.path.join(result_folder, "testConf.yaml")).as_posix()

    properties = createConfigFileForRestTest(result_folder, sut.name, openapi_path, testconfig_path)

    ## create testconfig
    start_tool_command = "pushd \"" + RESTEST_DIR +"\"\n"
    start_tool_command +=  JAVA_8_COMMAND + " -cp " + RESTEST_JAR + " " + RESTTEST_CREATE_TESTCONF_CLASS + " " + openapi_path
    start_tool_command +=  " >> " + resttest_log + " 2>&1 "
    start_tool_command +=  "\n\nsleep 2\n"

    ## modify test config file
    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        start_tool_command += authForResTest(testconfig_path, sut.authInfo.key, sut.authInfo.value, resttest_log)

    ## run the tool
    # command = command + "\necho \"start ResTest $(date)\"" + " >> " + resttest_log + "\n\n"
    start_tool_command += JAVA_8_COMMAND + " -jar " + RESTEST_JAR + " " + properties
    # start_tool_command += " >> " + resttest_log + " 2>&1"
    start_tool_command += " >> " + resttest_log + " 2>&1 &"
    start_tool_command +="\nRPID=$!"

    start_tool_command += "\nsleep " + str(MAX_TIME_SECONDS)
    start_tool_command += "\nkill $RPID"
    start_tool_command +=  "\npopd"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS+3)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

def createScriptForbBOXRT(sut, port, seed):
    tool = BB_BBOXRT

    bboxrt_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    time_budget = MAX_TIME_SECONDS

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    ## bBOXRT only supports schema with json, then we need to convert json to yaml in order to use it
    command = downloadOpenApi(sut.endpointPath, openapi_path, bboxrt_log)

    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, bboxrt_log)
        is_json = sut.openapiName.endswith('.json')
        if is_json:
            command = command + jsonToYaml(openapi_path, bboxrt_log)
            openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName.replace(".json",".yaml"))).as_posix()

    java_api_config = createbBOXRTApiConfigJavaFile(result_folder, sut, openapi_path)

    output_label = ""
    if ENABLE_TIMEOUT_RUM_CMD:
        output_label = "_R\"$" + ENABLE_TIMEOUT_RUM_CMD_VAR + "\""

    start_tool_command = "pushd \"" + BBOXRT_DIR + "\"\n"

    start_tool_command += JAVA_8_COMMAND + " -jar " + BBOXRT_JAR
    start_tool_command += " --api-file \""+java_api_config+"\""
    start_tool_command += " --api-yaml-file \"" + openapi_path+"\""
    start_tool_command += " --wl-results \"" + pathlib.PurePath(os.path.join(result_folder,"workloadResultsFile" + output_label + ".xlsx")).as_posix()+"\""
    start_tool_command += " --fl-results \"" + pathlib.PurePath(os.path.join(result_folder,"faultloadResultsFile" + output_label + ".xlsx")).as_posix()+"\""
    # Maximum duration of workload execution or 0 for exhausting all requests (default is 0)
    start_tool_command += " --wl-max-time " + str(int(time_budget/2) + 1)
    # Maximum duration of faultload execution or 0 for exhausting all requests (default is 0)
    start_tool_command += " --fl-max-time " + str(int(time_budget/2) + 1)
    #command = command + " --out " + bboxrt_log
    start_tool_command += " >> " + bboxrt_log + " 2>&1 "

    start_tool_command += "\npopd"

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

## based on doc https://schemathesis.readthedocs.io/en/stable/
def createScriptForSchemathesis(sut, port, seed):
    tool = BB_SCHEMATHESIS

    schemathesis_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()
    result_folder = createResultDir(sut.name, seed, port, tool)

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()


    command = downloadOpenApi(sut.endpointPath, openapi_path, schemathesis_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, schemathesis_log)

    start_tool_command = ""
    output_label=""
    if ENABLE_TIMEOUT_RUM_CMD:
        start_tool_command = schemathesisOption() +"\n"
        output_label = "_R\"$"+ENABLE_TIMEOUT_RUM_CMD_VAR+"\""

    start_tool_command += SCHEMATHESIS_CMD
    # Utilize stateful testing capabilities.
    if ENABLE_TIMEOUT_RUM_CMD:
        start_tool_command += " $OPTION"
    else:
        start_tool_command += " --stateful=links"

    # Enable or disable validation of input schema. default is true
    start_tool_command += " --validate-schema=false"
    # Timeout in milliseconds for network requests during the test run. 2s
    start_tool_command += " --request-timeout=2000"

    # Save test results as a VCR-compatible cassette.
    start_tool_command += " --cassette-path=\""+ result_folder + "/cassette" + output_label + ".yaml\""
    # Create junit-xml style report file at given path.
    start_tool_command += " --junit-xml=\"" + result_folder+"/junit" + output_label + ".xml\""
    # command += " --base-url="+openapi_path

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        start_tool_command += " --header \"" + sut.authInfo.key + ": " + sut.authInfo.value+"\""

    start_tool_command += " --base-url=http://localhost:$PORT" + str(sut.baseURL)
    start_tool_command += " \""+openapi_path +"\""

    start_tool_command += " >> " + schemathesis_log + " 2>&1 "

    if ENABLE_TIMEOUT_RUM_CMD:
        command += runCMDTimeout(tool, port, seed, start_tool_command, MAX_TIME_SECONDS)
    else:
        command += start_tool_command

    code = getScriptHead(port, tool, sut) + command + getScriptFooter(port, tool, sut)
    writeScript(code, port, tool, sut)

###################### utility #############################

def schemathesisOption():
    return """
OPTION="--data-generation-method=negative"
if [ $(( $%s %% 3 )) -eq 1 ]; then
\tOPTION="--stateful=links"
elif [ $(( $%s %% 3 )) -eq 2 ]; then
\tOPTION="--checks=all"
fi

            """% (ENABLE_TIMEOUT_RUM_CMD_VAR, ENABLE_TIMEOUT_RUM_CMD_VAR)

def runCMDTimeout(tool, port, seed, commandsInOneLine, time_budget):
    fun_name = tool + "_" + str(port) + "_" + str(seed)

    commands = ''.join(list(map(lambda x: "\t\t\t"+x, commandsInOneLine.splitlines(True))))

    template = """
function %s {
\ti=0

\twhile true
\t\tdo
\t\t\tlet %s++
\t\t\techo "%s $%s"
%s
\t\t\tsleep 5
\t\tdone
}
run_cmd "%s" %s

    """% (str(fun_name), ENABLE_TIMEOUT_RUM_CMD_VAR, str(fun_name), ENABLE_TIMEOUT_RUM_CMD_VAR, str(commands), str(fun_name), str(int(time_budget) + 5))
    return template


def jsonToYaml(openapi_path, log):
    command = "\necho \"convert json to yaml\"" + " >> " + log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + BB_EXP_UTIL + " jsonToYaml \"" + openapi_path+"\""
    command = command + " >> " + log + " 2>&1 "
    command = command + "\nsleep 2"
    return command + "\n\n"

def updateURLAndPort(openapi_path, port, log, convertToV3=False, format=None):
    command = "\necho \"update url and port\"" + " >> " + log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + BB_EXP_UTIL + " updateURLAndPort \"" + openapi_path + "\" " + str(port)
    if convertToV3:
        command = command + " " + str("true")
    if format is not None:
        command = command + " " + str(format)
    command = command + " >> " + log + " 2>&1 "
    command = command + "\nsleep 2"
    return command + "\n\n"

def authForResTest(testconfig_path, key, value, log):
    command = "\necho \"configure auth for ResTest\"" + " >> " + log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + BB_EXP_UTIL + " authForResTest \"" + testconfig_path + "\" \"" + str(key) + "\" \"" + str(value) +"\""
    command = command + " >> " + log + " 2>&1 "
    command = command + "\nsleep 2"
    return command + "\n\n"

## download openapi
def downloadOpenApi(endpointPath, openapiName, log):
    command = "\n# save open api to local\n"
    if endpointPath.startswith(LOCAL_SCHEMA_PREFIX):
        local_path = endpointPath.split(LOCAL_SCHEMA_PREFIX)[1]
        command = command + "cp "+str(pathlib.PurePath(local_path).as_posix())+" "+openapiName
    else:
        command = command + "curl http://localhost:$PORT"+endpointPath + " --output "+openapiName
        command = command + " >> " + log + " 2>&1 "

    command = command + "\nsleep 5"
    return command + "\n\n"


## create folder to save the results
def createResultDir(sut_name, seed, port, tool_name, folder=TESTS):
    dir = folder + "/"+resultDir(sut_name, seed, port, tool_name)
    os.makedirs(dir)
    return str(pathlib.PurePath(dir).as_posix())

def resultDir(sut_name, seed, port, tool_name):
    return sut_name + "_"+ tool_name + "_" + "_S" + str(seed) + "_" + str(port)

## create properties file for RestTest
def createConfigFileForRestTest(result_folder, sut_name, openapi_path, testconfig_path):
    config_properties = str(pathlib.PurePath(result_folder + "/" + sut_name + ".properties").as_posix())

    properties = [
        "oas.path="+openapi_path,
        "conf.path="+testconfig_path,

        # with such configuration, the RESTest never stops generating test cases
        # then could stop it with timeout
        "numtotaltestcases=-1",
        "allure.report=false",
        "coverage.input=false",
        "coverage.output=false",
        "test.target.dir="+result_folder
    ]
    with open(config_properties, 'w') as f:
        for line in properties:
            f.write(line)
            f.write('\n')

    return config_properties

## create java api config file for bBOXRT
def createbBOXRTApiConfigJavaFile(result_folder, sut, openapi_path):
     format_sut_name = str(sut.name).replace("-","_").upper()
     class_name = format_sut_name+"_RestAPI"

     authTemplate = ""
     if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        authTemplate = """
        config.AddAdditionalParameter(new AdditionalParameter("%s", Parameter.Location.Header, "%s").ApplyToAll());
        """ % (str(sut.authInfo.key), str(sut.authInfo.value))

     template = """
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.OpenAPIV3Parser;
import pt.uc.dei.rest_api_robustness_tester.specification.OpenApiToRestApi;
import pt.uc.dei.rest_api_robustness_tester.specification.RestApi;
import pt.uc.dei.rest_api_robustness_tester.specification.RestApiConverter;
import pt.uc.dei.rest_api_robustness_tester.specification.RestApiSpecification;
import pt.uc.dei.rest_api_robustness_tester.specification.RestApiSetup;
import pt.uc.dei.rest_api_robustness_tester.specification.RestApiConfig;
import pt.uc.dei.rest_api_robustness_tester.request.AdditionalParameter;
import pt.uc.dei.rest_api_robustness_tester.request.Parameter;

import java.io.File;

public class %s implements RestApiSetup {

    	@Override
        public RestApi Load(String apiYamlPath) {

         File file = new File("%s");
         OpenAPI openAPI = new OpenAPIV3Parser().read(file.getAbsolutePath());
         OpenAPIResolver res = new OpenAPIResolver(openAPI);
         openAPI = res.resolve();

         RestApiConverter<OpenAPI> converter = new OpenApiToRestApi();
         RestApiSpecification restAPISpecification = converter.Convert(openAPI);

         RestApiConfig config = new RestApiConfig();
         %s
         return new RestApi("%s", restAPISpecification, config);
    }
}
     """% (class_name, openapi_path, authTemplate, format_sut_name +" REST API")

     java_api_config = str(pathlib.PurePath(result_folder + "/" + class_name + ".java").as_posix())
     with open(java_api_config, 'w') as f:
        f.write(template)

     return java_api_config


def createConfigFileForRestTestGenV2(result_folder, openapi_path):

    rtg_config_path = str(pathlib.PurePath(result_folder + "/rtg_config.json").as_posix())
    output_folder = str(pathlib.PurePath(os.path.join(result_folder,"output")).as_posix())

    rtg_config = {
      "specificationFileName":  openapi_path ,
      "strategyName": "NominalAndErrorStrategy",
      "testingSessionName": "customNameForTestingSession",
      "outputPath":  output_folder
    }

    json_object = json.dumps(rtg_config, indent=4)

    with open(rtg_config_path, "w") as outfile:
        outfile.write(json_object)

    return rtg_config_path

def authForRestTestGenV2(sut, rtg_config_path):

    auth_config = "[{\\\"name\\\":\\\""+str(sut.authInfo.key)+"\\\",\\\"value\\\":\\\""+str(sut.authInfo.value)+"\\\",\\\"in\\\":\\\"header\\\",\\\"timeout\\\":30}]"

    with open(rtg_config_path, 'r') as openfile:
        config_obj = json.load(openfile)

    config_obj["authCommand"] = "echo \""+ auth_config + "\""

    json_object = json.dumps(config_obj, indent=4)

    with open(rtg_config_path, "w") as outfile:
        outfile.write(json_object)

######################## baseline tools ends ########################################

def createJobs():

    port = BASE_PORT

    for sut in SUTS:
        for seed in range(MIN_SEED, MAX_SEED + 1):

            if BB_EVOMASTER in BB_TOOLS:
                createScriptForEvoMaster(sut, port, seed)
                port = port + 10

            if BB_RESTLER in BB_TOOLS:
                # Restler
                createScriptForRestler(sut, port, seed)
                port = port + 10

            if BB_RESTCT in BB_TOOLS:
                # RestCT
                createScriptForRestCT(sut, port, seed)
                port = port + 10

            if BB_RESTTESTGEN in BB_TOOLS:
                # RestTestGen
                createScriptForRestTestGen(sut, port, seed)
                port = port + 10

            if BB_RESTTESTGENV2 in BB_TOOLS:
                # RestTestGen V2
                createScriptForRestTestGenV2(sut, port, seed)
                port = port + 10

            if BB_RESTEST in BB_TOOLS:
                # RestTest
                createScriptForRestTest(sut, port, seed)
                port = port + 10

            if BB_BBOXRT in BB_TOOLS:
                # bBOXRT
                createScriptForbBOXRT(sut, port, seed)
                port = port + 10

            if BB_SCHEMATHESIS in BB_TOOLS:
                # Schemathesis
                createScriptForSchemathesis(sut, port, seed)
                port = port + 10



shutil.rmtree(TMP, ignore_errors=True)
os.makedirs(TMP)
os.makedirs(LOGS)
os.makedirs(TESTS)
os.makedirs(SCRIPT_DIR)
createJobs()