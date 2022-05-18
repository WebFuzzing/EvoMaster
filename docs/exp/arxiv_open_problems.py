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

if len(sys.argv) != 6:
    print("Usage:\n<nameOfScript>.py <basePort> <dir> <minSeed> <maxSeed> <maxTimeSeconds>")
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

## Default setting: Configure whether to enable auth configuration
ENABLE_AUTH_CONFIG = True

## Default setting: Configure whether to fix basic issue in schema in order to apply the tool
FIX_BASIC_SCHEMA_ISSUE = True

## configure auth file
AUTH_DIR = os.path.abspath("authconfig")

class AuthInfo:
    def __init__(self, key, value):
        self.key = key
        self.value = value

class Sut:
    def __init__(self, name, endpointPath, openapiName, authInfo):
        self.name = name
        self.endpointPath = endpointPath
        ## baseline techniques need the schema which is on the local
        self.openapiName = openapiName
        ## auth configuration in header
        self.authInfo = authInfo


SUTS = [
    # REST JVM
    Sut("catwatch", "/v2/api-docs", "openapi.json", None),
    Sut("features-service", "/swagger.json", "openapi.json", None),
    Sut("languagetool", "/v2/swagger", "openapi.json", None),
    Sut("ocvn-rest", "/v2/api-docs?group=1ocDashboardsApi", "openapi.json", None),
    Sut("proxyprint", "/v2/api-docs", "openapi.json", AuthInfo("Authorization","Basic bWFzdGVyOjEyMzQ=")),
    Sut("rest-ncs", "/v2/api-docs", "openapi.json", None),
    Sut("rest-news", "/v2/api-docs", "openapi.json", None),
    Sut("rest-scs", "/v2/api-docs", "openapi.json", None),
    Sut("restcountries", "/openapi.yaml", "openapi.yaml", None),
    Sut("scout-api", "/api/swagger.json", "openapi.json", AuthInfo("Authorization","ApiKey administrator"))
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

############################################################################
### Restler
###     follow https://github.com/microsoft/restler-fuzzer to install it
###     then configure the bin folder where Restler is (`RESTLER_DIR`)
############################################################################
RESTLER_START_SCRIPT_DIR = "tools/baseline-restler-exp.py"
RESTLER_START_SCRIPT = "baseline-restler-exp.py"
RESTLER_DIR = os.environ.get("RESTLER_DIR", "")

############################################################################
### RestCT
###     follow https://github.com/GIST-NJU/RestCT to install it
###     the tool does not work on windows,
###     then need to employ wsl to start
############################################################################
WSL = "wsl -d Ubuntu-20.04"
WSL_ACCESS_DIR = os.environ.get("WSL_ACCESS_DIR", "")
if IS_WINDOWS and WSL_ACCESS_DIR == "":
    print("ERROR: cannot find WSL_ACCESS_DIR")
    exit(1)
WSL_ACCESS_DIR = WSL_ACCESS_DIR+"/results"
WSL_RESULT_DIR = "~/results"
RESTCT_DIR = "~/github/RestCT/src/restct.py"


############################################################################
### RestTestGen
###     nominal tester and error tester
############################################################################
RESTTESTGEN_NOMINAL_SWAGGER_CLIENT = "$BASE/tools/resttestgen/tools/swagger-codegen/swagger-codegen-cli.jar"
RESTTESTGEN_NOMINAL_TESTER = "$BASE/tools/resttestgen/resttestgen-1.0-SNAPSHOT.jar"
RESTTESTGEN_ERROR_TESTER = "$BASE/tools/resttestgen/error-tester.jar"

############################################################################
### RestTest
###     the tool is more than 1G, then
###     follow https://github.com/isa-group/RESTest to compile it with .zip
############################################################################
RESTEST_DIR = "$BASE/tools/RESTest-restest-1.2.0/RESTest-restest-1.2.0/target"
RESTEST_JAR = "restest-full.jar"
RESTTEST_CREATE_TESTCONF_CLASS = "es.us.isa.restest.main.CreateTestConf"

############################################################################
### bBOXRT
###     https://git.dei.uc.pt/cnl/bBOXRT/tree/master
###     follow the README to build the tool
###     note the jar could not be used directly due to some unresolved
###     dependency. in order to use it, configure BBOXRT_DIR to the `target`
############################################################################
BBOXRT_DIR = "$BASE/tools/bBOXRT/bBOXRT.git/target"
BBOXRT_JAR = "REST_API_Robustness_Tester-1.0.jar"

############################################################################
### in order to apply bb tools
###     there might need a further handling in inputs,
###     eg, set auth, modify the port, add schemes/server, modify the format
###     then we developed such utility which includes
###         - authForResTest <testConfig.yaml path> <key> <value>
###         - jsonToYaml <openapi path>
###         - updateURLAndPort <openapi path> <port>
############################################################################
BB_EXP_UTIL = "$BASE/tools/bb-exp-util.jar"



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
    s += "# JaCoCo does not like full paths or exec in Windows/GitBash format... but relative path seems working \n"
    # strange it does not want  ./"+sys.argv[2] before the exec
    s += "bash $BASE/suts/"+sut.name+".sh $PORT $BASE/tools/jacocoagent.jar  ./exec/"+label+"__jacoco.exec > \""+LOGS+"/sut__"+sut.name+"__"+tool+"__$PORT.txt\" 2>&1 & \n"
    s += "PID=$! \n"
    s += "# this works on GitBash/Windows... but very brittle \n"
    s += "sleep 120 \n"  # OCVN can be very sloooooow to start, eg 45s on my laptop
    # s += "PGID=$( ps  -p $PID | tail -n +2 | cut -c18-30 | xargs) \n" # this gives too many issues
    s += "CHILD=$( ps | cut -c1-20 | grep $PID | cut -c1-10 | grep -v $PID | xargs) \n"
    s += " \n"
    return s

def getScriptFooter():
    s = ""
    s += "\n"
    s += "# with default kill signal, JaCoCo not generated. we need a SIGINT (code 2)\n"
    #s += "kill -n 2 -- -$PGID\n" # this kills everything, even other scripts started with schedule.py
    # s += "echo Going to kill child process [$CHILD] of parent [$PID] \n" #logging
    s += "kill -n 2 $CHILD\n"
    #s += "kill -n 2 $PID\n" # might not be necessary
    s += "\n"
    return s

def createScriptForEvoMaster(sut, port, seed):
    tool = "evomaster"

    result_folder = createResultDir(sut.name, seed, port, tool)

    em = JAVA_8_COMMAND+" -Xms1G -Xmx4G -jar $BASE/tools/evomaster.jar"
    em += " --blackBox true"
    em += " --outputFormat JAVA_JUNIT_5"
    em += " --maxTime " + str(MAX_TIME_SECONDS) + "s"
    em += " --bbSwaggerUrl http://localhost:$PORT" + sut.endpointPath
    em += " --seed " + str(seed)
    em += " --showProgress=false"
    em += " --testSuiteSplitType=NONE"

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        em += " --header0 \"" + sut.authInfo.key + ": " + sut.authInfo.value+"\""

    em += " --outputFolder \""+result_folder+"\""
    em += " > \""+LOGS+"/tool__" + sut.name+"__"+tool+"__$PORT.txt\" 2>&1"
    em += "\n"

    code = getScriptHead(port, tool, sut) + em + getScriptFooter()
    writeScript(code, port, tool, sut)

######################## baseline tools start ########################################
def createScriptForRestler(sut, port, seed):
    tool = "Restler"

    # copy python script to the exp folder
    shutil.copy(pathlib.PurePath(RESTLER_START_SCRIPT_DIR).as_posix(), BASE_DIR)

    restler_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)
    # restler employ hour as the unit
    time_budget = math.ceil(MAX_TIME_SECONDS / 36) / 100
    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    script = io.StringIO()

    command = downloadOpenApi(sut.endpointPath, openapi_path, restler_log)

    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, restler_log)

    command = command + PYTHON_COMMAND + " " + RESTLER_START_SCRIPT
    command = command + " --api_spec_path " + openapi_path
    command = command + " --port " + str(port)
    command = command + " --restler_drop_dir " + pathlib.PurePath(RESTLER_DIR).as_posix()
    command = command + " --time_budget " + str(time_budget)
    command = command + " --result_dir " + result_folder

    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        command = command + " --token_refresh_cmd \""+PYTHON_COMMAND+" "+str(pathlib.PurePath(os.path.join(AUTH_DIR, tool+"_"+sut.name+".py")).as_posix()) +"\""

    command = command + " >> " + restler_log + " 2>&1"

    script.write("\n\necho \"Starting Restler: " + command + "\"\n")
    script.write("echo\n\n")
    script.write(command + "\n\n")

    code = getScriptHead(port, tool, sut) + script.getvalue() + getScriptFooter()
    writeScript(code, port, tool, sut)

def createScriptForRestCT(sut, port, seed):
    tool = "RestCT"

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

    script = io.StringIO()

    command = downloadOpenApi(sut.endpointPath, openapi_actual_path, restct_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, restct_log)

    if IS_WINDOWS:
        command = command + WSL + " python3 \"" + RESTCT_DIR +"\""
    else:
        command = command + PYTHON_COMMAND + " " + RESTCT_DIR

    command = command + " --swagger \"" + openapi_path +"\""
    command = command + " --budget " + str(time_budget)
    command = command + " --dir \"" + result_folder +"\""
    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        command = command + " --header "+ "\"{\\\"" + sut.authInfo.key + "\\\": \\\"" + sut.authInfo.value + "\\\"}\""
    command = command + " >> " + restct_log + " 2>&1 "
    if IS_WINDOWS:
        # command = command + "\nNPID=$!"
        # command = command + "\n\nwait $NPID\n"
        command = command + "\n\nsleep 5\n"
        command = command + "cp -r " + result_actual_folder + " " + pathlib.PurePath(TESTS).as_posix()

    script.write("\n\necho \"Starting RestCT: " + command + "\"\n")
    script.write("echo\n\n")
    script.write(command + "\n\n")

    code = getScriptHead(port, tool, sut) + script.getvalue() + getScriptFooter()
    writeScript(code, port, tool, sut)


# RestTestGen
def createScriptForRestTestGen(sut, port, seed):

    tool = "RestTestGen"

    swagger_client_log = pathlib.PurePath(
        LOGS + "/tool__" + sut.name + "__" + tool + "__swagger_client_generator__" + str(port) + ".txt").as_posix()
    nominal_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool  +"__nominal_tester__"+str(port) + ".txt").as_posix()
    error_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool +"__error_tester__"+str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)
    client_code_path = str(pathlib.PurePath(result_folder+"/output/codegen").as_posix())
    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    script = io.StringIO()

    command = downloadOpenApi(sut.endpointPath, openapi_path, swagger_client_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, swagger_client_log)

    # Use swagger codegen to generate client-side code
    command = command + JAVA_8_COMMAND + " -jar "+RESTTESTGEN_NOMINAL_SWAGGER_CLIENT+" generate -i " + openapi_path + " -l java -o " + client_code_path
    command = command + " >> " + swagger_client_log + " 2>&1 \n"

    # wait 2s
    command = command + "\nsleep 2\n"

    # Compile generated classes
    command = command + "(cd "+client_code_path+" && mvn clean package)"
    command = command + " >> " + swagger_client_log + " 2>&1 \n"
    swagger_client_path = str(pathlib.PurePath(result_folder+"/output/codegen/target/swagger-java-client-1.0.0.jar").as_posix())
    # wait 2s
    command = command + "\nsleep 2\n"

    command = command + "\n\n"
    # run resttestgen
    command = command + JAVA_8_COMMAND + " -jar " + RESTTESTGEN_NOMINAL_TESTER
    command = command + " --output " + result_folder
    command = command + " --classes " + swagger_client_path
    command = command + " --swagger " + openapi_path
    command = command + " >> " + nominal_log + " 2>&1 & \n"
    command = command + "NPID=$!\n\n"
    command = command + "wait $NPID\n\n"

    script.write("\n\necho \"Starting nominal tester of RestTestGen\" \n")
    script.write("echo\n\n")
    script.write(command + "\n")

    command = JAVA_8_COMMAND + " -jar " + RESTTESTGEN_ERROR_TESTER
    command = command + " --service " + result_folder
    command = command + " --reports " + result_folder + "/reports"
    command = command + " --swagger " + openapi_path
    command = command + " >> " + error_log + " 2>&1 \n"

    script.write("\n\necho \"Starting error tester of RestTestGen\" \n")
    script.write("echo\n\n")
    script.write(command + "\n\n")

    code = getScriptHead(port, tool, sut) + script.getvalue() + getScriptFooter()
    writeScript(code, port, tool, sut)

def createScriptForRestTest(sut, port, seed):
    tool = "RestTest"

    resttest_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    time_budget = MAX_TIME_SECONDS

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    script = io.StringIO()

    command = downloadOpenApi(sut.endpointPath, openapi_path, resttest_log)
    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, resttest_log)

    ## the file and path cannot be configured with command line, unless modify the source code
    testconfig_path = pathlib.PurePath(os.path.join(result_folder, "testConf.yaml")).as_posix()

    properties = createConfigFileForRestTest(result_folder, sut.name, openapi_path, testconfig_path)

    ## create testconfig
    command = command + "pushd " + RESTEST_DIR +"\n"
    command = command + JAVA_8_COMMAND + " -cp " + RESTEST_JAR + " " + RESTTEST_CREATE_TESTCONF_CLASS + " " + openapi_path
    command = command + " >> " + resttest_log + " 2>&1 "
    command = command + "\n\nsleep 2\n"

    ## modify test config file
    if ENABLE_AUTH_CONFIG and sut.authInfo is not None:
        command = command + authForResTest(testconfig_path, sut.authInfo.key, sut.authInfo.value, resttest_log)

    ## run the tool
    command = command + "\necho \"start ResTest $(date)\"" + " >> " + resttest_log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + RESTEST_JAR + " " + properties
    command = command + " >> " + resttest_log + " 2>&1 &"
    command = command + "\nRPID=$!"

    command = command + "\nsleep " + str(time_budget)
    command = command + "\nkill $RPID"
    command = command + "\necho \"kill ResTest $(date)\"" + " >> " + resttest_log + "\n\n"
    command = command + "\npopd"

    script.write("\n\necho \"Starting RestTest: " + command + "\"\n")
    script.write("echo\n\n")
    script.write(command + "\n\n")

    code = getScriptHead(port, tool, sut) + script.getvalue() + getScriptFooter()
    writeScript(code, port, tool, sut)

def createScriptForbBOXRT(sut, port, seed):
    tool = "bBOXRT"

    bboxrt_log = pathlib.PurePath(LOGS + "/tool__" + sut.name + "__" + tool + "__" +str(port) + ".txt").as_posix()

    result_folder = createResultDir(sut.name, seed, port, tool)

    time_budget = MAX_TIME_SECONDS

    openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName)).as_posix()

    script = io.StringIO()

    ## bBOXRT only supports schema with json, then we need to convert json to yaml in order to use it
    command = downloadOpenApi(sut.endpointPath, openapi_path, bboxrt_log)

    if FIX_BASIC_SCHEMA_ISSUE:
        command = command + updateURLAndPort(openapi_path, port, bboxrt_log)
        is_json = sut.openapiName.endswith('.json')
        if is_json:
            command = command + jsonToYaml(openapi_path, bboxrt_log)
            openapi_path = pathlib.PurePath(os.path.join(result_folder, sut.openapiName.replace(".json",".yaml"))).as_posix()

    java_api_config = createbBOXRTApiConfigJavaFile(result_folder, sut, openapi_path)

    command = command + "pushd " + BBOXRT_DIR + "\n"

    command = command + JAVA_8_COMMAND + " -jar " + BBOXRT_JAR
    command = command + " --api-file \""+java_api_config+"\""
    command = command + " --api-yaml-file \"" + openapi_path+"\""
    command = command + " --wl-results \"" + pathlib.PurePath(os.path.join(result_folder,"workloadResultsFile.xlsx")).as_posix()+"\""
    command = command + " --fl-results \"" + pathlib.PurePath(os.path.join(result_folder,"faultloadResultsFile.xlsx")).as_posix()+"\""
    # Maximum duration of workload execution or 0 for exhausting all requests (default is 0)
    command = command + " --wl-max-time " + str(int(time_budget/2) + 1)
    # Maximum duration of faultload execution or 0 for exhausting all requests (default is 0)
    command = command + " --fl-max-time " + str(int(time_budget/2) + 1)
    #command = command + " --out " + bboxrt_log
    command = command + " >> " + bboxrt_log + " 2>&1 "

    command = command + "\npopd"

    script.write("\n\necho \"Starting bBOXRT: " + command + "\"\n")
    script.write("echo\n\n")
    script.write(command + "\n\n")

    code = getScriptHead(port, tool, sut) + script.getvalue() + getScriptFooter()
    writeScript(code, port, tool, sut)

###################### utility #############################

def jsonToYaml(openapi_path, log):
    command = "\necho \"convert json to yaml\"" + " >> " + log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + BB_EXP_UTIL + " jsonToYaml \"" + openapi_path+"\""
    command = command + " >> " + log + " 2>&1 "
    command = command + "\nsleep 2"
    return command + "\n\n"

def updateURLAndPort(openapi_path, port, log):
    command = "\necho \"update url and port\"" + " >> " + log + "\n\n"
    command = command + JAVA_8_COMMAND + " -jar " + BB_EXP_UTIL + " updateURLAndPort \"" + openapi_path + "\" " + str(port)
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
    return tool_name+"_" + sut_name + "_S" + str(seed) + "_" + str(port)

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

######################## baseline tools ends ########################################

def createJobs():

    port = BASE_PORT

    for sut in SUTS:
        for seed in range(MIN_SEED, MAX_SEED + 1):

            createScriptForEvoMaster(sut, port, seed)
            port = port + 10

            # Restler
            createScriptForRestler(sut, port, seed)
            port = port + 10

            # RestCT
            createScriptForRestCT(sut, port, seed)
            port = port + 10

            # RestTestGen
            createScriptForRestTestGen(sut, port, seed)
            port = port + 10

            # RestTest
            createScriptForRestTest(sut, port, seed)
            port = port + 10

            # bBOXRT
            createScriptForbBOXRT(sut, port, seed)
            port = port + 10



shutil.rmtree(TMP, ignore_errors=True)
os.makedirs(TMP)
os.makedirs(LOGS)
os.makedirs(TESTS)
os.makedirs(SCRIPT_DIR)
createJobs()