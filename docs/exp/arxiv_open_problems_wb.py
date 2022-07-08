#!/usr/bin/env python

####
# WARNING: by default, on saga.sigma2.no, might be that python refers to the ancient 2.7 version.
# You might need to add something like:
#
# module load  Python/3.7.4-GCCcore-8.3.0
#
# at the end of your .bash_profile in your home folder.
#
# Note: this script was first designed for Abel, but now Saga replaced it.
# Some of the comments/fixes might be no longer required.
###


### Python script used to generate Bash scripts to run on a cluster or locally.
#   Given N jobs (ie EvoMaster runs), those will be divided equally among M different
#   Bash scripts.
#   Once such Bash scripts are generated, on the cluster all of those can be submitted with:
#
#   ./runall.sh
#
#  they can also be submitted manually with:
#
#  for s in `ls *.sh`; do sbatch $s; done
#
#  For local experiments, better to use schedule.py
#
#  Currently, for 100k budget, use 300 minutes as timeout on cluster


### Other useful commands on the cluster:
# scancel --user=<your_username>      ->  to cancel all of your jobs (in case you realized there were problems)
# squeue -u <your_username>           ->  check your running jobs. to count them, you can pipe it to "| wc -l"
# cost -u <your_username>             ->  check how much resources (ie CPU time) we can still use

### For interactive session:
# qlogin --account=nn9476k

### More info:
# https://documentation.sigma2.no/quick/saga.html
#
# Max 400 submitted jobs per user at any time.


# Some useful commands as a user on the Abel system are:
#
# sbatch <job-script-file>   Submit a job script to the queue system
# squeue                     List of all jobs
# pending                    List of all pending jobs
# qsumm                      Summary information about queue usage
# cost -u                    User CPU Usage information
# projects                   Lists the projects you are member of
# module avail               Lists available software Modules
# module list                Lists your currently used software Modules
# dusage                     List home directory disk usage
# dusage -p <project>        List project disk usage
# scontrol show job <id>     Details of a job


###   If a job failed due to
#       perl: warning: Setting locale failed.
#       perl: warning: Please check that your locale settings:
#   	    LANGUAGE = (unset),
#   	    LC_ALL = (unset),
#   	    LC_CTYPE = "UTF-8",
#   	    LANG = "en_US.iso885915"
#       are supported and installed on your system.
#   you may fix it by executing
#       export LANG=en_US.UTF-8
#       export LC_ALL=en_US.UTF-8

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



if len(sys.argv) < 9 or len(sys.argv) > 11:
    print(
        "Usage:\n<nameOfScript>.py <cluster> <baseSeed> <dir> <minSeed> <maxSeed> <budget> <timeoutMinutes> <nJobs> <configFilter?> <sutFilter?>")
    exit(1)


### input parameters

# Whether .sh are meant to run on cluster or locally
CLUSTER = sys.argv[1].lower() in ("yes", "true", "t")

# base seeds used for EM runs. TCP port bindings will be based on such seed.
# If running new experiments while some previous are still running, to avoid TCP port
# conflict, can use an higher base seed. Each EM run reserves 10 ports. So, if you run
# 500 jobs with starting seed 10000, you will end up using ports up to 15000
BASE_SEED = int(sys.argv[2])

# When creating a new set of experiments, all needed files will be saved in a folder
BASE_DIR = os.path.abspath(sys.argv[3])

# Experiments are repeated a certain number of times, with different seed for the
# random generator. This specify the starting seed.
MIN_SEED = int(sys.argv[4])

# Max seed, included. For example, if running min=10 and max=39, each experiment is
# going to be repeated 30 times, starting from seed 10 to seed 39 (both included).
MAX_SEED = int(sys.argv[5])

# For how long to run the search. If it is a number, then it will use number of actions as stopping criterion.
# However, if it contains either "s", "m", or "h", then time will be used as stopping criterion (see --maxTime)
BUDGET = str(sys.argv[6])

# How many minutes we expect each EM run to last AT MOST.
# Warning: if this value is under-estimated, it will happen the cluster will kill jobs
# that are not finished withing the specified time.
# At the moment this is used only on cluster
TIMEOUT_MINUTES = int(sys.argv[7])

# How many scripts M we want the N jobs to be divided into.
# Note: on cluster we can at most submit 400 scripts.
# Also note that in the same .sh script there can be experiments only for a single SUT.
NJOBS = int(sys.argv[8])

# An optional string to filter CONFIGS to be included
# None or `all` represents all CONFIGS should be included
# Default is None
CONFIGFILTER = None
if len(sys.argv) > 9:
    CONFIGFILTER = str(sys.argv[9])

#
# An optional string to filter SUTs to be included based on their names
# A string could refer to multiple SUTs separated by a `,` like a,b
# Note that
# None or `all` represents all SUTs should be included
# and only consider unique ones, eg, create one experiment setting for a,a
# Default is None
SUTFILTER = None
if len(sys.argv) > 10:
    SUTFILTER = str(sys.argv[10])

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


JDK_8 = "JDK_8"
JDK_11 = "JDK_11"
JS = "JS"
DOTNET_3 = "DOTNET_3"

class Sut:
    def __init__(self, name, timeWeight, platform):
        self.name = name
        # the higher value, the more time it will need compared to the other SUTS
        self.timeWeight = timeWeight
        # Java? JS? NodeJS
        self.platform = platform



# To ge the SUTs, you need in EMB to run the script "scripts/dist.py True" to
# generate a dist.zip file that you can upload on cluster.
# Note: the values after the SUT names is multiplicative factor for how long
# experiments should be run.
# Depending on what experiments you are running, might want to de-select some
# of the SUTs (eg, by commenting them out)

SUTS = [
    Sut("js-rest-ncs", 1, JS),
    Sut("js-rest-scs", 1, JS),
    Sut("cyclotron", 1, JS),
    Sut("disease-sh-api", 1, JS),
    Sut("realworld-app", 1, JS),
    Sut("spacex-api", 1, JS),
    Sut("features-service", 1, JDK_8),
    Sut("scout-api", 2, JDK_8),
    Sut("proxyprint", 2, JDK_8),
    Sut("rest-ncs", 2, JDK_8),
    Sut("rest-scs", 1, JDK_8),
    Sut("rest-news", 1, JDK_8),
    Sut("catwatch", 1, JDK_8),
    Sut("restcountries", 2, JDK_8),
    Sut("languagetool", 3, JDK_8),
    Sut("ocvn-rest", 1, JDK_8),
    Sut("gestaohospital-rest", 1, JDK_8),
    Sut("cwa-verification", 1, JDK_11),
    Sut("ind0", 1, JDK_8)
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


# Specify if using any industrial case study
USING_IND = False


### We need different settings based on whether we are running the
### scripts on cluster or locally.
if CLUSTER:

    HOME = os.environ['HOME']
    EVOMASTER_DIR = HOME
    CASESTUDY_DIR = HOME + "/dist"
    LOGS_DIR = HOME + "/nobackup"


## Local configurations
else:

    # You will need to define environment variables on your OS
    #EVOMASTER_DIR = os.environ.get("EVOMASTER_DIR", "")
    EVOMASTER_DIR = "./tools"
    EMB_DIR = os.environ.get('EMB_DIR',"")

    if EVOMASTER_DIR == "":
        raise Exception("You must specify a EVOMASTER_DIR env variable specifying where evomaster.jar can be found")

    if EMB_DIR == "":
        raise Exception("You must specify a EMB_DIR env variable specifying the '/dist' folder from where EMB repository was cloned")

    CASESTUDY_DIR = EMB_DIR

    if not os.path.exists(CASESTUDY_DIR):
        raise Exception(CASESTUDY_DIR + " does not exist. Did you run script/dist.py?")

    if USING_IND:
        ind0_package = os.environ.get("SUT_PACKAGE_IND0", "")
        if ind0_package == "":
            raise Exception("You cannot run experiments on IND0 without specify target package to cover with SUT_PACKAGE_IND0 env variable")

    LOGS_DIR = BASE_DIR

    JAVA_HOME_8 = os.environ.get("JAVA_HOME_8", "")
    if JAVA_HOME_8 == "":
        raise Exception("You must specify a JAVA_HOME_8 env variable specifying where JDK 8 is installed")

    JAVA_HOME_11 = os.environ.get("JAVA_HOME_11", "")
    if JAVA_HOME_11 == "":
        raise Exception("You must specify a JAVA_HOME_11 env variable specifying where JDK 11 is installed")


# How to run EvoMaster
EVOMASTER_JAVA_OPTIONS = " -Xms2G -Xmx4G  -jar evomaster.jar "
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

### By default, we allocate 3 CPUs per run.
### Recall that we are running 3 processes, and they are multithreaded.
CPUS = 3

TIMEOUT_SUT_START_MINUTES = 20


if not CLUSTER:
    REPORT_DIR = str(pathlib.PurePath(REPORT_DIR).as_posix())
    SCRIPT_DIR = str(pathlib.PurePath(SCRIPT_DIR).as_posix())
    TEST_DIR = str(pathlib.PurePath(TEST_DIR).as_posix())
    LOG_DIR = str(pathlib.PurePath(LOG_DIR).as_posix())

    #Due to Windows limitations (ie crappy FS), we need to copy JARs over
    for sut in SUTS:
        if sut.platform == JDK_8 or sut.platform == JDK_11:
            # copy jar files
            shutil.copy(os.path.join(CASESTUDY_DIR, sut.name + EM_POSTFIX), BASE_DIR)
            shutil.copy(os.path.join(CASESTUDY_DIR, sut.name + SUT_POSTFIX), BASE_DIR)
        elif sut.platform == JS or sut.platform == DOTNET_3:
            # copy folders, which include both SUT and EM Controller
            shutil.copytree(os.path.join(CASESTUDY_DIR, sut.name), os.path.join(BASE_DIR, sut.name))


    shutil.copy(os.path.join(CASESTUDY_DIR, AGENT), BASE_DIR)
    shutil.copy(os.path.join(EVOMASTER_DIR, "evomaster.jar"), BASE_DIR)



# We could end up with many scripts, up to the max number of jobs we can run in parallel, eg. 400.
# But those scripts still need to be submitted. So, we create a script to do that.
def createRunallScript():
    script_path = BASE_DIR + "/runall.sh"
    script = open(script_path, "w")

    script.write("#!/bin/bash \n\n")

    script.write("cd \"$(dirname \"$0\")\"\n\n")

    script.write("for s in `ls scripts/*.sh`; do\n")
    script.write("   echo Going to start $s\n")
    if CLUSTER:
        script.write("   sbatch $s\n")
    else:
        script.write("   $s & \n")
    script.write("done \n")

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)



def writeScript(code, port, sut):
    script_path = SCRIPT_DIR + "/evomaster_" + str(port) + "_" + sut.name + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

    return script


# A cluster can have several configurations, which can be set with #SBATCH comments
def getScriptHead(timeoutMinutes):
    s = "#!/bin/bash \n"

    if CLUSTER:
        s += "#SBATCH --job-name=" + EXP_ID + " \n"
        s += "#SBATCH --account=nn9476k \n"
        s += "#SBATCH --mem-per-cpu=4G \n"
        s += "#SBATCH --nodes=1 --ntasks-per-node=" + str(CPUS) + " \n"
        s += "#SBATCH --time=" + str(timeoutMinutes) + ":00 \n\n"
    return s


def createJobHead(port, sut, timeoutMinutes):
    script = io.StringIO()

    script.write(getScriptHead(timeoutMinutes))

    sut_log = LOG_DIR + "/log_sut_" + sut.name + "_" + str(port) + ".txt"

    # Start SUT as background process on the given port
    controllerPort = str(port)
    sutPort = str(port + 1)

    if CLUSTER:

        if sut.platform == JDK_8:
            script.write("\nmodule load Java/1.8.0_212\n\n")
        else:
            print("ERROR: currently not handling " + sut.platform + " for experiments on cluster")
            exit(1)

        # To speed-up I/O, copy files over to SCRATCH folder
        script.write("cd $SCRATCH \n")
        script.write("cp " + EVOMASTER_DIR + "/evomaster.jar . \n")

        # Not sure if great idea to copy 1000s of files for JS intro SCRATCH
        if sut.platform == JDK_8 or sut.platform == JDK_11:
            sut_em_path = os.path.join(CASESTUDY_DIR, sut.name + EM_POSTFIX)
            sut_jar_path = os.path.join(CASESTUDY_DIR, sut.name + SUT_POSTFIX)
            agent_path = os.path.join(CASESTUDY_DIR, AGENT)
            script.write("cp " + sut_em_path + " . \n")
            script.write("cp " + sut_jar_path + " . \n")
            script.write("cp " + agent_path + " . \n")

        script.write("\n")

    script.write("\n")

    timeoutStart = TIMEOUT_SUT_START_MINUTES * 60

    command = ""

    if sut.platform == JDK_8 or sut.platform == JDK_11:
        params = " " + controllerPort + " " + sutPort + " " + sut.name + SUT_POSTFIX + " " + str(timeoutStart) + " " + getJavaCommand(sut)

        jvm = " -Xms1G -Xmx4G -Dem.muteSUT=true -Devomaster.instrumentation.jar.path="+AGENT
        JAVA = getJavaCommand(sut)
        command = JAVA + jvm + " -jar " + sut.name + EM_POSTFIX + " " + params + " > " + sut_log + " 2>&1 &"

    elif sut.platform == JS:
        # TODO sutPort
        before = "pushd " + sut.name + "\n"
        command = "node instrumented/em/em-main.js"
        #command = "npm run em:run" # This does not work when trying then to kill this process
        command = " EM_PORT=" + controllerPort + " " + command +" > " + sut_log + " 2>&1 & "
        command = before + command

    elif sut.platform == DOTNET_3:
        params = " " + controllerPort + " " + sutPort
        command = "dotnet " + sut.name+"/"+sut.name + EM_POSTFIX_DOTNET + " " + params + " > " + sut_log + " 2>&1 &"

    if not CLUSTER:
        script.write("\n\necho \"Starting EM Runner with: " + command + "\"\n")
        script.write("echo\n\n")

    script.write(command + "\n\n")
    # as the process running NPM dies immediately after spawning Node, to make this work we need to run Node
    # directly and not NPM
    script.write(CONTROLLER_PID + "=$! \n\n")  # store pid of process, so can kill it

    if sut.platform == JS:
        script.write("popd\n\n")

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

    def getTimeoutMinutes(self):
        # the timeout we want to wait for does depend not only on the number of runs, but
        # also on the weights of the SUT (this is captured by self.counter).
        # Note: we add a 10% just in case...
        timeoutMinutes = TIMEOUT_SUT_START_MINUTES + int(math.ceil(1.1 * self.counter * TIMEOUT_MINUTES))
        self.waits.append(timeoutMinutes)
        return timeoutMinutes

    def resetTmpForNewRun(self):
        self.counter = 0
        self.opened = False
        self.updatePerJob()
        self.updatePort()

    def hasSpareJobs(self):
        return self.jobsLeft > self.sutsLeft


def writeWithHeadAndFooter(code, port, sut, timeout):
    head = createJobHead(port, sut, timeout)
    footer = closeJob(port, sut)
    code = head + code + footer
    writeScript(code, port, sut)



def createOneJob(state, sut, seed, setting):
    code = addJobBody(state.port, sut, seed, setting)
    state.updateBudget(sut.timeWeight)
    state.jobsLeft -= 1
    state.opened = True
    state.generated += 1
    return code


def getJavaCommand(sut):
    JAVA = "java "
    if not CLUSTER:
        if sut.platform == JDK_8:
            JAVA = "\"" + JAVA_HOME_8 +"\"/bin/java "
        elif sut.platform == JDK_11:
            JAVA = "\"" + JAVA_HOME_11 +"\"/bin/java "
        JAVA = str(pathlib.PurePath(JAVA).as_posix())
    return JAVA



JACOCO_AGENT=str(pathlib.PurePath(os.path.abspath("./tools/jacocoagent.jar")).as_posix())
JACOCO_CLI=str(pathlib.PurePath(os.path.abspath("./tools/jacococli.jar")).as_posix())



def addJobBody(port, sut, seed, setting):
    script = io.StringIO()

    em_log = LOG_DIR + "/log_em_" + sut.name + "_" + str(port) + ".txt"

    custom = customParameters(seed, setting)
    params = custom

    ### standard
    if ("s" in BUDGET) or ("m" in BUDGET) or ("h" in BUDGET):
        params += " --stoppingCriterion=TIME"
        params += " --maxTime=" + BUDGET
    else:
        params += " --stoppingCriterion=FITNESS_EVALUATIONS"
        params += " --maxActionEvaluations=" + BUDGET

    params += " --statisticsColumnId=" + sut.name
    params += " --seed=" + str(seed)
    params += " --sutControllerPort=" + str(port)
    params += " --outputFolder=" + TEST_DIR + "/" + sut.name
    params += " --statisticsFile=" + \
              REPORT_DIR + "/statistics_" + sut.name + "_" + str(seed) + ".csv"
    params += " --snapshotInterval=5"
    params += " --snapshotStatisticsFile=" + \
              REPORT_DIR + "/snapshot_" + sut.name + "_" + str(seed) + ".csv"
    params += " --appendToStatisticsFile=true"
    params += " --writeStatistics=true"
    params += " --showProgress=false"
    params += " --testSuiteSplitType=NONE"


    JAVA = getJavaCommand(sut)

    #### NEW ####
    params += " --jaCoCoAgentLocation="+JACOCO_AGENT
    params += " --jaCoCoCliLocation="+JACOCO_CLI

    if "RANDOM" in custom:
        alg = "RS"
    elif "MIO" in custom:
        alg = "MIO"
    else:
        raise Exception("No valid algorithm in " + custom)

    params += " --jaCoCoOutputFile="+str(pathlib.PurePath(os.path.abspath("./exec/"+sut.name+"__wb"+alg+"__"+str(port)+"__jacoco.exec")).as_posix())
    params += " --javaCommand="+JAVA
    params += " --enableBasicAssertions=false"
    #############

    command = JAVA + EVOMASTER_JAVA_OPTIONS + params + " >> " + em_log + " 2>&1"

    if not CLUSTER:
        script.write("\n\necho \"Starting EvoMaster with: " + command + "\"\n")
        script.write("echo\n\n")

    if CLUSTER:
        timeout = int(math.ceil(1.1 * sut.timeWeight * TIMEOUT_MINUTES * 60))
        errorMsg = "ERROR: timeout for " + sut.name
        command = "timeout " +str(timeout) + "  " + command \
                  + " || ([ $? -eq 124 ] && echo " + errorMsg + " >> " + em_log + " 2>&1" + ")"

    script.write(command + " \n\n")

    return script.getvalue()


def createJobs():

    CONFIGS = getConfigs()

    ## filter config if specified
    if CONFIGFILTER is not None and CONFIGFILTER.lower() != "all":
        filtered = list(filter(lambda x: x.filterKey is None or x.filterKey.lower() == CONFIGFILTER.lower(), CONFIGS))
        if len(filtered) > 0:
            CONFIGS = filtered
        else:
            print("ERROR: Specified filter tag is undefined")
            exit(1)


    NRUNS_PER_SUT = (1 + MAX_SEED - MIN_SEED) * sum(map(lambda o: o.numOfSettings, CONFIGS))
    SUT_WEIGHTS = sum(map(lambda x: x.timeWeight, SUTS))
    # For example, if we have 30 runs and 5 SUTs, the total budget
    # to distribute among the different jobs/scripts is 150.
    # However, some SUTs might have weights greater than 1 (ie, they run slower, so
    # need more budget)
    TOTAL_BUDGET = NRUNS_PER_SUT * SUT_WEIGHTS

    state = State(TOTAL_BUDGET)

    SUTS.sort(key=lambda x: -x.timeWeight)

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
                        code = createOneJob(state, sut, seed, setting)

                    # can we add this new run to the current opened script?
                    elif(
                            # we need to check if we would not exceed the budget limit per job
                            (state.counter + sut.timeWeight) <= state.perJob
                            # however, that check must be ignored if we cannot open/create any new script file
                            # for the current SUT
                            or not state.hasSpareJobs() or
                            # this case is bit more tricky... let's say only few runs are left that
                            # we need to allocate in a script, but they are so few that they would need
                            # only a small percentage of a new script capacity (eg, less than 30%).
                            # In such a case, to avoid getting very imbalanced execution times,
                            # we could just add those few runs to the current script.
                            (NRUNS_PER_SUT - completedForSut < 0.3 * state.perJob / sut.timeWeight)
                    ):
                        code += addJobBody(state.port, sut, seed, setting)
                        state.updateBudget(sut.timeWeight)

                    else:
                        writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes())
                        state.resetTmpForNewRun()
                        code = createOneJob(state, sut, seed, setting)

                    # keep track that a new run has been handled
                    completedForSut += 1

        if state.opened:
            writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes())

    print("Generated scripts: " + str(state.generated))
    print("Max wait for a job: " + str(max(state.waits)) + " minutes")
    print("Median wait for a job: " + str(statistics.median(state.waits)) + " minutes")
    print("Budget left: " + str(state.budget))
    print("Total time: " + str(sum(state.waits) / 60) + " hours")
    print("Total budget: " + str(CPUS * sum(state.waits) / 60) + " hours")


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
    def __init__(self, settings, filterKey=None):
        self.filterKey = filterKey
        self.settings = settings
        self.numOfSettings = 1
        for s in self.settings:
            self.numOfSettings *= s.count

    # generate all settings for configured parameters
    def generateAllSettings(self):
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

# set customized parameters
def customParameters(seed, setting):

    params = ""

    label = ""

    ### set parameter based on the setting
    for ps in setting:
        params += " --" + str(ps[0]) + "=" + str(ps[1])
        ### set label based on each value of the parameter
        if is_float(str(ps[1])) and float(ps[1]) <= 1.0:
            label += "_" + str(int(float(ps[1]) * 100))
        else:
            label += "_" + str(ps[1])

    params += " --testSuiteFileName=EM_" + label + "_" + str(seed) + "_Test"

    return params

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

    CONFIGS = []

    ALG_MIO = ParameterSetting("algorithm", ["MIO"])
    ALG_RANDOM = ParameterSetting("algorithm", ["RANDOM"])

    RS0 = ParameterSetting("heuristicsForSQL", [False])
    RS1 = ParameterSetting("extractSqlExecutionInfo", [False])
    RS2 = ParameterSetting("generateSqlDataWithSearch", [False])

    crs = Config([ALG_RANDOM, RS0, RS1, RS2], "RS")
    cmio = Config([ALG_MIO], "MIO")

    CONFIGS.append(crs)
    CONFIGS.append(cmio)

    return CONFIGS


############################################################################
#### END of custom configurations
############################################################################


# Create the actual job scripts
createJobs()

# Create a single ./runall.sh script to submit all the job scripts
createRunallScript()
