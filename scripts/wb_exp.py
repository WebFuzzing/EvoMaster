#!/usr/bin/env python

### Python script used to generate Bash scripts to run locally.
### Given N jobs (ie EvoMaster runs), those will be divided equally among M different Bash scripts.
### Once such Bash scripts are generated, they can be run in parallel with schedule.py

import io
import os
import pathlib
import random
import shutil
import stat
import statistics

import math
import sys


EXP_ID = "evomaster"

### Named optional parameters
LABEL_seed = "seed"
LABEL_njobs = "njobs"
LABEL_configfilter = "configfilter"
LABEL_sutfilter = "sutfilter"
LABEL_jacoco = "jacoco"
LABELS = [LABEL_seed,LABEL_njobs,LABEL_configfilter,LABEL_sutfilter,LABEL_jacoco]


if len(sys.argv) < 5:
    print("Usage:\n<nameOfScript>.py <dir> <minSeed> <maxSeed> <budget> named_param=? ... named_param=?")
    print("Available named parameters: " + str(LABELS))
    exit(1)


### MAIN input parameters ###


# When creating a new set of experiments, all needed files will be saved in a folder
BASE_DIR = os.path.abspath(sys.argv[1])

# Experiments are repeated a certain number of times, with different seed for the
# random generator. This specifies the starting seed.
MIN_SEED = int(sys.argv[2])

# Max seed, included. For example, if running min=10 and max=39, each experiment is
# going to be repeated 30 times, starting from seed 10 to seed 39 (both included).
MAX_SEED = int(sys.argv[3])

# For how long to run the search. If it is a number, then it will use number of actions as stopping criterion.
# However, if it contains either "s", "m", or "h", then time will be used as stopping criterion (see --maxTime)
BUDGET = str(sys.argv[4])


### main input parameter validation
if MIN_SEED > MAX_SEED:
    print("ERROR: min seed is greater than max seed")
    exit(1)



### Named parameters ###

# base seeds used for EM runs. TCP port bindings will be based on such seed.
# If running new experiments while some previous are still running, to avoid TCP port
# conflict, can use an higher base seed. Each EM run reserves 10 ports. So, if you run
# 500 jobs with starting seed 10000, you will end up using ports up to 15000
BASE_SEED = 10000


# How many scripts M we want the N jobs to be divided into.
# Also note that in the same .sh script there can be experiments only for a single SUT.
# To run experiments, ideally a high number would be good, especially when running things with schedule.py
# However, creating too many jobs can end up with too many bash scripts created in the file system.
# As we usually run 15 jobs in parallel, a default like 100 is a good compromise.
# WARNING: if experiments rely on applying different kinds of instrumentations,
# then the value of nJobs MUST be greater than the number of experiment runs, to GUARANTEE that no more than
# one experiment is run per bash script job (once a SUT is instrumented, we cannot change its instrumentation again).
NJOBS = 1000

# String to filter CONFIGS to be included.
# A string could refer to multiple CONFIGS separated by a `,` like a,b
# None represents all CONFIGS should be included.
# Default is None.
CONFIGFILTER = None

# An optional string to filter SUTs to be included based on their names
# A string could refer to multiple SUTs separated by a `,` like a,b
# Note that
# None represents all SUTs should be included
# and only consider unique ones, eg, create one experiment setting for a,a
# Default is None
SUTFILTER = None

# Whether the generated tests have JaCoCo configured to collect code coverage.
# This requires some environment variables to be setup to point to whether Agent and CLI
# jar files of JaCoCo are located on local machine.
JACOCO = False

### Derived named variables ###
if len(sys.argv) > 5:
    # There might be better ways to build such map in Python...
    options = sys.argv[5:len(sys.argv)]
    keys   = [x.lower() for x in list(map(lambda z: z.split("=")[0], options))]
    values = list(map(lambda z: z.split("=")[1], options))
    kv = dict(zip(keys,values))


    if LABEL_seed in kv:
        BASE_SEED = int(kv[LABEL_seed])

    if LABEL_njobs in kv:
        NJOBS = int(kv[LABEL_njobs])

    if LABEL_configfilter in kv:
        CONFIGFILTER = kv[LABEL_configfilter]

    if LABEL_sutfilter in kv:
        SUTFILTER = kv[LABEL_sutfilter]

    if LABEL_jacoco in kv:
        JACOCO = kv[LABEL_jacoco].lower() in ("yes", "true", "t")

    for key in kv:
        if key not in LABELS:
            print("Undefined option: '" + key +"'. Available options: ")
            print(*LABELS)
            exit(1)


# Avoid overriding an existing experiment folder
if not os.path.isdir(BASE_DIR):
    print("creating folder: " + BASE_DIR)
    os.makedirs(BASE_DIR)
else:
    print("ERROR: target folder already exists")
    exit(1)



#### Printing Summary of Options ####
print("*Configurations*")
print("BASE_DIR: " + str(BASE_DIR))
print("MIN_SEED: " + str(MIN_SEED))
print("MAX_SEED: " + str(MAX_SEED))
print("BUDGET: " + str(BUDGET))
print(LABEL_njobs + ": " + str(NJOBS))
print(LABEL_seed + ": " + str(BASE_SEED))
print(LABEL_configfilter + ": " + str(CONFIGFILTER))
print(LABEL_sutfilter + ": " + str(SUTFILTER))
print(LABEL_jacoco + ":" + str(JACOCO))


JDK_8 = "JDK_8"
JDK_11 = "JDK_11"
JDK_17 = "JDK_17"
JDK_21 = "JDK_21"

def isJava(sut):
    return (sut.platform == JDK_8
            or sut.platform == JDK_11
            or sut.platform == JDK_17
            or sut.platform == JDK_21
            )

class Sut:
    def __init__(self, name, platform):
        self.name = name
        # Java? JS? NodeJS
        self.platform = platform



# To ge the SUTs, you need in EMB to run the script "scripts/dist.py True" to
# generate a dist.zip file that you can upload on cluster.
# Note: the values after the SUT names is multiplicative factor for how long
# experiments should be run.
# Depending on what experiments you are running, might want to de-select some
# of the SUTs (eg, by commenting them out)

SUTS = [
    # REST
    Sut("bibliothek",  JDK_17),
    Sut("blogapi",JDK_8),
    Sut("catwatch",  JDK_8),
    Sut("cwa-verification",  JDK_11),
    Sut("familie-ba-sak",JDK_17),
    Sut("features-service",  JDK_8),
    Sut("genome-nexus",  JDK_8),
    Sut("gestaohospital-rest",  JDK_8),
    Sut("languagetool",  JDK_8),
    Sut("market",  JDK_11),
    Sut("ocvn",  JDK_8),
    Sut("pay-publicapi",JDK_11),
    Sut("person-controller",JDK_21),
    Sut("proxyprint",  JDK_8),
    Sut("reservations-api",  JDK_11),
    Sut("rest-ncs",  JDK_8),
    Sut("rest-news",  JDK_8),
    Sut("rest-scs",  JDK_8),
    Sut("restcountries",  JDK_8),
    Sut("scout-api",  JDK_8),
    Sut("tiltaksgjennomforing-api",JDK_17),
    Sut("tracking-system",JDK_11),
    Sut("session-service",JDK_8),
    Sut("user-management",JDK_8),
    Sut("youtube-mock",JDK_8),
    # GRAPHQL
    # Sut("graphql-ncs",  JDK_8),
    # Sut("graphql-scs",  JDK_8),
    # Sut("patio-api",  JDK_11),
    # Sut("petclinic-graphql",  JDK_8),
    # Sut("timbuctoo",  JDK_11),
    # RPC
    # Sut("signal-registration",JDK_17),
    # Sut("rpc-thrift-ncs",  JDK_8),
    # Sut("rpc-thrift-scs",  JDK_8),
]

if SUTFILTER is not None:
    filteredsut = []

    for s in list(set(SUTFILTER.split(","))):
        found = list(filter(lambda x: x.name.lower() == s.lower(), SUTS))
        if len(found) == 0:
            print("ERROR: cannot find the specified SUT "+s)
            exit(1)
        filteredsut.extend(found)

    SUTS = filteredsut


# You will need to define environment variables on your OS
EVOMASTER_DIR = os.environ.get("EVOMASTER_DIR", "")
EMB_DIR = os.environ.get('EMB_DIR',"")

if EVOMASTER_DIR == "":
        raise Exception("You must specify a EVOMASTER_DIR env variable specifying where evomaster.jar can be found")

if EMB_DIR == "":
        raise Exception("You must specify a EMB_DIR env variable specifying the '/dist' folder from where EMB repository was cloned")

CASESTUDY_DIR = EMB_DIR

if not os.path.exists(CASESTUDY_DIR):
        raise Exception(CASESTUDY_DIR + " does not exist. Did you run script/dist.py?")


LOGS_DIR = BASE_DIR

JAVA_HOME_8 = os.environ.get("JAVA_HOME_8", "")
if JAVA_HOME_8 == "":
        raise Exception("You must specify a JAVA_HOME_8 env variable specifying where JDK 8 is installed")

JAVA_HOME_11 = os.environ.get("JAVA_HOME_11", "")
if JAVA_HOME_11 == "":
        raise Exception("You must specify a JAVA_HOME_11 env variable specifying where JDK 11 is installed")

JAVA_HOME_17 = os.environ.get("JAVA_HOME_17", "")
if JAVA_HOME_17 == "":
        raise Exception("You must specify a JAVA_HOME_17 env variable specifying where JDK 17 is installed")

JAVA_HOME_21 = os.environ.get("JAVA_HOME_21", "")
if JAVA_HOME_21 == "":
        raise Exception("You must specify a JAVA_HOME_21 env variable specifying where JDK 21 is installed")

JACOCO_AGENT = "not-defined"
JACOCO_CLI = "not-defined"

if JACOCO:

    JACOCO_LOCATION = os.environ.get("JACOCO_LOCATION", "")
    if JACOCO_LOCATION == "":
        raise Exception("If you want to use JaCoCo, must setup JACOCO_LOCATION env variable pointing to their folder")

    JACOCO_AGENT = os.path.join(JACOCO_LOCATION, "jacocoagent.jar")
    JACOCO_CLI   = os.path.join(JACOCO_LOCATION, "jacococli.jar")

    if not os.path.exists(JACOCO_AGENT):
        raise Exception("JaCoCo Agent JAR not existing at location: " + JACOCO_AGENT)
    if not os.path.exists(JACOCO_CLI):
        raise Exception("JaCoCo Agent CLI not existing at location: " + JACOCO_CLI)


# How to run EvoMaster
EM_PATH= pathlib.PurePath(os.path.join(BASE_DIR,"evomaster.jar")).as_posix()
EVOMASTER_JAVA_OPTIONS = " -Xms2G -Xmx8G  -jar " + EM_PATH + " "
AGENT = "evomaster-agent.jar"
EM_POSTFIX = "-evomaster-runner.jar"
EM_POSTFIX_DOTNET = "-evomaster-runner.dll"
SUT_POSTFIX = "-sut.jar"

if NJOBS < len(SUTS):
    print("ERROR: you need at least one job per SUT, and those are " + str(len(SUTS)))
    exit(1)

# Where to put stuff (default in subdirs of BASEDIR)
REPORT_DIR = BASE_DIR + "/reports"
os.makedirs(REPORT_DIR)

SCRIPT_DIR = BASE_DIR + "/scripts"
os.makedirs(SCRIPT_DIR)

TEST_DIR = BASE_DIR + "/tests"
os.makedirs(TEST_DIR)

ALL_LOGS = LOGS_DIR + "/logs"
#We might end up generating gigas of log files. So, at each new experiments, we delete previous logs
shutil.rmtree(ALL_LOGS, ignore_errors=True)
LOG_DIR = ALL_LOGS + "/" + EXP_ID
os.makedirs(LOG_DIR)

CONTROLLER_PID = "CONTROLLER_PID"


REPORT_DIR = str(pathlib.PurePath(REPORT_DIR).as_posix())
SCRIPT_DIR = str(pathlib.PurePath(SCRIPT_DIR).as_posix())
TEST_DIR = str(pathlib.PurePath(TEST_DIR).as_posix())
LOG_DIR = str(pathlib.PurePath(LOG_DIR).as_posix())

#Due to Windows limitations (ie crappy FS), we need to copy JARs over
for sut in SUTS:
        if isJava(sut):
            # copy jar files
            shutil.copy(os.path.join(CASESTUDY_DIR, sut.name + EM_POSTFIX), BASE_DIR)
            shutil.copy(os.path.join(CASESTUDY_DIR, sut.name + SUT_POSTFIX), BASE_DIR)
        elif sut.platform == JS or sut.platform == DOTNET_3:
            # copy folders, which include both SUT and EM Controller
            # Note: if this fails when running on Windows, you need to increase the max path for
            # files (default is 260 characters) by enabling "Enable Win32 long paths".
            # See https://helpdeskgeek.com/how-to/how-to-fix-filename-is-too-long-issue-in-windows/
            shutil.copytree(os.path.join(CASESTUDY_DIR, sut.name), os.path.join(BASE_DIR, sut.name))
        else:
            raise Exception("Unexpected platform" + sut.platform)

shutil.copy(os.path.join(CASESTUDY_DIR, AGENT), BASE_DIR)
shutil.copy(os.path.join(EVOMASTER_DIR, "evomaster.jar"), BASE_DIR)


def writeScript(code, port, sut):
    script_path = SCRIPT_DIR + "/evomaster_" + str(port) + "_" + sut.name + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

    return script


def getScriptHead():
    s = "#!/bin/bash \n"
    return s


def createJobHead(port, sut):
    script = io.StringIO()

    script.write(getScriptHead())

    sut_log = LOG_DIR + "/log_sut_" + sut.name + "_" + str(port) + ".txt"

    # Start SUT as background process on the given port
    controllerPort = str(port)
    sutPort = str(port + 1)

    script.write("\n")

    # some APIs are slow to startup... especially if many at same time
    timeoutStart = 5 * 60

    command = ""

    if isJava(sut):
        sutPath = pathlib.PurePath(os.path.join(BASE_DIR,sut.name + SUT_POSTFIX)).as_posix()
        params = " " + controllerPort + " " + sutPort + " " + sutPath + " " + str(timeoutStart) + " " + getJavaCommand(sut)

        # Note: this is for the process of the Driver. The Xmx settings of the SUTs will need to be specified directly
        #       in the Java/Kotlin code of the External Driver, under getJVMParameters(), if the default is not enough.
        agentPath = pathlib.PurePath(os.path.join(BASE_DIR,AGENT)).as_posix()
        jvm = " -Xms1G -Xmx2G -Dem.muteSUT=true -Devomaster.instrumentation.jar.path="+agentPath
        JAVA = getJavaCommand(sut)
        jarPath = pathlib.PurePath(os.path.join(BASE_DIR, sut.name + EM_POSTFIX)).as_posix()
        command = JAVA + jvm + " -jar " + jarPath + " " + params + " > " + sut_log + " 2>&1 &"

    else:
        raise Exception("ERROR: unrecognized / not supported " + sut.platform)

    script.write("\n\necho \"Starting EM Runner with: " + command + "\"\n")
    script.write("echo\n\n")

    script.write(command + "\n\n")
    # as the process running NPM dies immediately after spawning Node, to make this work we need to run Node
    # directly and not NPM
    script.write(CONTROLLER_PID + "=$! \n\n")  # store pid of process, so can kill it

    script.write("sleep 20 \n\n")  # wait a bit to be sure the SUT handler can respond

    return script.getvalue()


def closeJob(port, sut_name):
    return "kill $" + CONTROLLER_PID + "\n"


####################################

class State:
    def __init__(self, budget):
        # total budget for the search which is left
        self.budget = budget

    # number of generated script files, so far
    generated = 0
    # each job will have a different time duration, and we keep track
    # of those durations for every single generated script
    waits = []
    # how many jobs/scripts we still need to create
    jobsLeft = NJOBS
    # how many SUTs we still need to create jobs/scripts for.
    # recall that in a script there can be only 1 SUT
    sutsLeft = len(SUTS)
    # how much budget we have used for the current opened job/script
    counter = 0
    # whether we are adding a new run in an existing script.
    # if not, need to make sure to create all the right header / init methods
    opened = False
    # budget left for each remaining job/script
    perJob = 0
    # to avoid TCP conflicts, each job uses a different port range
    port = BASE_SEED

    def updatePerJob(self):
        if self.jobsLeft == 0:
            self.perJob = 0
        else:
            self.perJob = self.budget / self.jobsLeft

    def updatePort(self):
        self.port += 10

    def updateBudget(self, weight):
        # the used budget for current script increases...
        self.counter += weight
        # ... whereas the total left budget decreases by the same amount
        self.budget -= weight


    def resetTmpForNewRun(self):
        self.counter = 0
        self.opened = False
        self.updatePerJob()
        self.updatePort()

    def hasSpareJobs(self):
        return self.jobsLeft > self.sutsLeft


def writeWithHeadAndFooter(code, port, sut):
    head = createJobHead(port, sut)
    footer = closeJob(port, sut)
    code = head + code + footer
    writeScript(code, port, sut)



def createOneJob(state, sut, seed, setting, configName):
    code = addJobBody(state.port, sut, seed, setting, configName)
    state.updateBudget(1)
    state.jobsLeft -= 1
    state.opened = True
    state.generated += 1
    return code


def getJavaExe(sut):

    if not isJava(sut):
        raise Exception("ERROR: not a recognized JVM SUT: " + sut.platform)

    if sut.platform == JDK_8:
            JAVA = JAVA_HOME_8 +"/bin/java"
    elif sut.platform == JDK_11:
            JAVA = JAVA_HOME_11 +"/bin/java"
    elif sut.platform == JDK_17:
            JAVA = JAVA_HOME_17 +"/bin/java"
    elif sut.platform == JDK_21:
            JAVA = JAVA_HOME_21 +"/bin/java"
    else:
            raise Exception("ERROR: unhandled JVM version: " + sut.platform)

    JAVA = str(pathlib.PurePath(JAVA).as_posix())

    return JAVA


def getJavaCommand(sut):

    JAVA = getJavaExe(sut)
    # due to possible spaces in Windows folder paths
    JAVA = "\"" + JAVA + "\" "
    if sut.platform == JDK_17 or sut.platform == JDK_21:
        JAVA = JAVA + " --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED "

    return JAVA

last_generated_ip = "127.0.0.2"
def generate_ip():
    global last_generated_ip
    tokens = last_generated_ip.split(".")

    if len(tokens) != 4:
        pass

    if int(tokens[0]) != 127:
        pass

    for n in range(len(tokens)-1, 0, -1):
        part = int(tokens[n])
        if n == 3:
            part += 20
            if part < 255:
                tokens[n] = str(part)
                for m in range(n + 1, len(tokens) -1):
                    tokens[m] = "0"
                break
            elif part >= 255:
                tokens[n] = "1"
        elif n < 3:
            part += 1
            if part < 255:
                tokens[n] = str(part)
                for m in range(n+1, len(tokens) - 1):
                    tokens[m] = "0"
                break
            elif part >= 255:
                tokens[n] = "1"

    ip = "{}.{}.{}.{}".format(tokens[0], tokens[1], tokens[2], tokens[3])
    last_generated_ip = ip
    return ip

def addJobBody(port, sut, seed, setting, configName):
    script = io.StringIO()

    em_log = LOG_DIR + "/log_em_" + sut.name + "_" + str(port) + ".txt"

    params = ""
    label = ""

    ### default, no parameter configuration
    if len(setting) == 0:
        label = "default"
    else :
        ### set parameter based on the setting
        for ps in setting:
            params += " --" + str(ps[0]) + "=" + str(ps[1])
            ### set label based on each value of the parameter
            if is_float(str(ps[1])) and float(ps[1]) <= 1.0:
                label += "_" + str(int(float(ps[1]) * 100))
            else:
                label += "_" + str(ps[1])

    params += " --testSuiteFileName=EM_" + sut.name.replace("-","_") + label + "_" + str(seed) + "_Test"
    params += " --labelForExperiments=" + label
    params += " --labelForExperimentConfigs=" + configName

    identifier = "_" + sut.name  + "_" + label + "_" + str(seed)

    ### standard
    if ("s" in BUDGET) or ("m" in BUDGET) or ("h" in BUDGET):
        params += " --stoppingCriterion=TIME"
        params += " --maxTime=" + BUDGET
    else:
        params += " --stoppingCriterion=ACTION_EVALUATIONS"
        params += " --maxEvaluations=" + BUDGET

    params += " --statisticsColumnId=" + sut.name
    params += " --seed=" + str(seed)
    params += " --sutControllerPort=" + str(port)
    params += " --outputFolder=" + TEST_DIR + "/" + sut.name
    params += " --statisticsFile=" + REPORT_DIR + "/statistics" + identifier + ".csv"
    params += " --snapshotInterval=5"
    params += " --snapshotStatisticsFile=" + REPORT_DIR + "/snapshot" + identifier + ".csv"
    params += " --appendToStatisticsFile=true"
    params += " --writeStatistics=true"
    params += " --showProgress=false"
    params += " --testSuiteSplitType=NONE"
    params += " --exportCoveredTarget=true"
    params += " --coveredTargetFile="+REPORT_DIR+"/covered_target_file" + identifier + ".txt"
    params += " --externalServiceIP=" + generate_ip()
    params += " --probOfHarvestingResponsesFromActualExternalServices=0"  # this adds way too much noise to results
    params += " --createConfigPathIfMissing=false"
    params += " --javaCommand=\""+str(pathlib.PurePath(getJavaExe(sut)).as_posix())+"\""


    if JACOCO:
        params += " --jaCoCoAgentLocation="+str(pathlib.PurePath(os.path.abspath(JACOCO_AGENT)).as_posix())
        params += " --jaCoCoCliLocation="+str(pathlib.PurePath(os.path.abspath(JACOCO_CLI)).as_posix())
        params += " --jaCoCoOutputFile="+str(pathlib.PurePath(os.path.abspath("./exec/"+sut.name+"__wb"+configName+"__"+str(port)+"__jacoco.exec")).as_posix())
        # params += " --enableBasicAssertions=false" # TODO need to deal with flakiness


    JAVA = getJavaCommand(sut)
    command = JAVA + EVOMASTER_JAVA_OPTIONS + params + " >> " + em_log + " 2>&1"

    script.write("\n\necho \"Starting EvoMaster with: " + command + "\"\n")
    script.write("echo\n\n")


    script.write(command + " \n\n")

    return script.getvalue()


def createJobs():

    CONFIGS = getConfigs()

    ## filter configs if specified
    if CONFIGFILTER is not None:
        filteredconfigs = []
        for c in list(set(CONFIGFILTER.split(","))):
            found = list(filter(lambda x: x.name.lower() == c.lower(), CONFIGS))
            if len(found) == 0:
                raise Exception("ERROR: cannot find the specified config: "+c)
            filteredconfigs.extend(found)

        CONFIGS = filteredconfigs

    NRUNS_PER_SUT = (1 + MAX_SEED - MIN_SEED) * sum(map(lambda o: o.numOfSettings, CONFIGS))
    SUT_WEIGHTS = sum(map(lambda x: 1, SUTS))  # FIXME no longer using timeweights
    # For example, if we have 30 runs and 5 SUTs, the total budget
    # to distribute among the different jobs/scripts is 150.
    # However, some SUTs might have weights greater than 1 (ie, they run slower, so
    # need more budget)
    TOTAL_BUDGET = NRUNS_PER_SUT * SUT_WEIGHTS
    TOTAL_NRUNS = NRUNS_PER_SUT * len(SUTS)

    state = State(TOTAL_BUDGET)

    for sut in SUTS:

        state.sutsLeft -= 1
        state.resetTmpForNewRun()

        code = ""
        completedForSut = 0

        for seed in range(MIN_SEED, MAX_SEED + 1):

            random.shuffle(CONFIGS)

            for config in CONFIGS:

                for setting in config.generateAllSettings():

                    # first run in current script: we need to create all the initializing preambles
                    if state.counter == 0:
                        code = createOneJob(state, sut, seed, setting, config.name)

                    # can we add this new run to the current opened script?
                    elif(
                            # we need to check if we would not exceed the budget limit per job
                            (state.counter + 1) <= state.perJob
                            # however, that check must be ignored if we cannot open/create any new script file
                            # for the current SUT
                            or not state.hasSpareJobs() or
                            # this case is bit more tricky... let's say only few runs are left that
                            # we need to allocate in a script, but they are so few that they would need
                            # only a small percentage of a new script capacity (eg, less than 30%).
                            # In such a case, to avoid getting very imbalanced execution times,
                            # we could just add those few runs to the current script.
                            (NRUNS_PER_SUT - completedForSut < 0.3 * state.perJob / 1)
                    ):
                        code += addJobBody(state.port, sut, seed, setting, config.name)
                        state.updateBudget(1)

                    else:
                        writeWithHeadAndFooter(code, state.port, sut)
                        state.resetTmpForNewRun()
                        code = createOneJob(state, sut, seed, setting, config.name)

                    # keep track that a new run has been handled
                    completedForSut += 1

        if state.opened:
            writeWithHeadAndFooter(code, state.port, sut)

    print("Number of used SUTs: " + str(len(SUTS)))
    print("Total number of experiments: " + str(TOTAL_NRUNS))
    print("Generated scripts: " + str(state.generated))



class ParameterSetting:
    # name is the same name used in the EM parameters
    # values is an array of configured values regarding the parameter
    def __init__(self, name, values):
        self.name = name
        self.values = values
        self.count = len(self.values)

    # get a parameter name with its value at index
    def pvalue(self, index):
        if index >= len(self.values):
            exit("a value at the index "+ str(index) + " does not exist")
        return (self.name, self.values[index])

# Each Config object has a list of ParameterSetting objects
class Config:
    # settings is an array of ParameterSetting objects
    def __init__(self, settings, name="exp"):
        if " " in name:
            raise Exception("Config name must have no space. Wrong value: " + name)
        self.name = name
        self.settings = settings
        self.numOfSettings = 1
        for s in self.settings:
            self.numOfSettings *= s.count

    # generate all settings for configured parameters
    def generateAllSettings(self):
        if len(self.settings) == 0:
            return [[]]
        all = []
        lst = [0] * len(self.settings)
        while lst is not None:
            e = []
            for i in range(len(lst)):
                e.append(self.settings[i].pvalue(lst[i]))
            all.append(e)
            lst = self.plus1(lst)
        return all

    # next setting
    def plus1(self, lst):
        if lst[0] < self.settings[0].count - 1:
            lst[0] = lst[0] + 1
            return lst
        for i in range(len(lst)):
            if lst[i] < self.settings[i].count-1:
                lst[i] = lst[i] + 1
                return lst
            else:
                lst[i] = 0
        return None



def is_float(input):
    try:
        float(input)
    except ValueError:
        return False
    return True


############################################################################
### Custom
### Following will need to be changed based on what kind of experiments
### we want to run.
############################################################################

def getConfigs():

    # array of configuration objects. We will run experiments for each of
    # these configurations
    # each Config object has a list of ParameterSetting objects
    CONFIGS = []

    ### Example (step1) on how to specify ParameterSetting objects

    # ALGO_MIO = ParameterSetting("algorithm",["MIO"])
    # ALGO_RANDOM = ParameterSetting("algorithm",["RANDOM"])

    # PR5 = ParameterSetting("probOfRandomSampling",[0.5])
    # PR = ParameterSetting("probOfRandomSampling",[0.1, 0.2])

    ### Example (step2) on how to define Config objects with specified ParameterSetting objects
    # foo = Config([ALGO_MIO, PR5], "foo")
    # bar = Config([ALGO_RANDOM, PR], "bar")

    ### Example (step3) on employing Config objects for the experiments
    # CONFIGS.append(foo)
    # CONFIGS.append(bar)

    ### Alternatively, an empty config will just use the default configurations in EM
    CONFIGS.append(Config([], "EXP"))

    return CONFIGS


############################################################################
#### END of custom configurations
############################################################################



# Create the actual job scripts
createJobs()

