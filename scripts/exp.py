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
#
#  Currently, for 100k budget, use 300 minutes as timeout


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



if len(sys.argv) != 9:
    print(
        "Usage:\n<nameOfScript>.py <cluster> <baseSeed> <dir> <minSeed> <maxSeed> <maxActions> <minutesPerRun> <nJobs>")
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

# By default, experiments are run with stopping criterion of number of actions and not time.
MAX_ACTIONS = int(sys.argv[6])

# How many minutes we expect each EM run to last AT MOST.
# Warning: if this value is under-estimated, it will happen the cluster will kill jobs
# that are not finished withing the specified time.
MINUTES_PER_RUN = int(sys.argv[7])

# How many scripts M we want the N jobs to be divided into.
# Note: on cluster we can at most submit 400 scripts.
# Also not that in the same .sh script there can be experiments only for a single SUT.
NJOBS = int(sys.argv[8])


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

class Sut:
    def __init__(self, name, timeWeight, platform):
        self.name = name
        # the higher value, the more time it will need compared to the other SUTS
        self.timeWeight = timeWeight
        # Java? JS? NodeJS
        self.platform = platform



### We need different settings based on whether we are running the
### scripts on cluster or locally.
if CLUSTER:

    # To ge the SUTs, you need in EMB to run the script "scripts/dist.py" to
    # generate a dist.zip file that you can upload on cluster.
    # Note: the values after the SUT names is multiplicative factor for how long
    # experiments should be run.
    # Depending on what experiments you are running, might want to de-select some
    # of the SUTs (eg, by commenting them out)

    SUTS = [
        Sut("features-service", 1, JDK_8),
        Sut("scout-api", 2, JDK_8),
        Sut("proxyprint", 2, JDK_8),
        Sut("rest-ncs", 2, JDK_8),
        Sut("rest-scs", 1, JDK_8),
        Sut("rest-news", 1, JDK_8),
        Sut("catwatch", 1, JDK_8)
    ]

    HOME = os.environ['HOME']
    EVOMASTER_DIR = HOME
    CASESTUDY_DIR = HOME + "/dist"
    LOGS_DIR = os.environ['USERWORK']


## Local configurations
else:
    # These SUTs requires Docker
    SUTS = [
        Sut("ind0", 1, JDK_8),
        Sut("ocvn-rest", 1, JDK_8),
        # Sut("ncs-js", 1, JS),
        # Sut("scs-js", 1, JS)
    ]

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

    ind0_package = os.environ.get("SUT_PACKAGE_IND0", "")
    if ind0_package == "":
        raise Exception("You cannot run experiments on IND0 without specify target package to cover with SUT_PACKAGE_IND0 env variable")

    LOGS_DIR = BASE_DIR


# How to run EvoMaster
EVOMASTER = "java  -Xms2G -Xmx4G  -jar evomaster.jar"
AGENT = "evomaster-agent.jar"
EM_POSTFIX = "-evomaster-runner.jar"
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
        elif sut.platform == JS:
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
            print("ERROR: currently not handling " + sut.platform)
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
        params = " " + controllerPort + " " + sutPort + " " + sut.name + SUT_POSTFIX + " " + str(timeoutStart)
        jvm = " -Xms1G -Xmx4G -Dem.muteSUT=true -Devomaster.instrumentation.jar.path="+AGENT
        command = "java " + jvm + " -jar " + sut.name + EM_POSTFIX + " " + params + " > " + sut_log + " 2>&1 &"

    elif sut.platform == JS:
        # TODO sutPort
        before = "pushd " + sut.name + "\n"
        command = " EM_PORT=" + controllerPort + " npm run em > " + sut_log + " 2>&1 & "
        command = before + command


    if not CLUSTER:
        script.write("\n\necho \"Starting EM Runner with: " + command + "\"\n")
        script.write("echo\n\n")

    script.write(command + "\n\n")
    # FIXME: this does not work for JS... as the process running NPM dies immediately after spawning Node
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
        self.budget = budget

    generated = 0
    waits = []
    jobsLeft = NJOBS
    sutsLeft = len(SUTS)
    counter = 0
    opened = False
    perJob = 0
    port = BASE_SEED

    def updatePerJob(self):
        if self.jobsLeft == 0:
            self.perJob = 0
        else:
            self.perJob = self.budget / self.jobsLeft

    def updatePort(self):
        self.port += 10

    def updateBudget(self, weight):
        self.counter += weight
        self.budget -= weight

    def getTimeoutMinutes(self):
        timeoutMinutes = TIMEOUT_SUT_START_MINUTES + int(math.ceil(1.1 * self.counter * MINUTES_PER_RUN))
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



def createOneJob(state, sut, seed, config):
    code = addJobBody(state.port, sut, seed, config)
    state.updateBudget(sut.timeWeight)
    state.jobsLeft -= 1
    state.opened = True
    state.generated += 1
    return code


def addJobBody(port, sut, seed, config):
    script = io.StringIO()

    em_log = LOG_DIR + "/log_em_" + sut.name + "_" + str(port) + ".txt"

    params = customParameters(seed, config)

    ### standard
    params += " --stoppingCriterion=FITNESS_EVALUATIONS"
    params += " --maxActionEvaluations=" + str(MAX_ACTIONS)
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

    command = EVOMASTER + params + " >> " + em_log + " 2>&1"

    if not CLUSTER:
        script.write("\n\necho \"Starting EvoMaster with: " + command + "\"\n")
        script.write("echo\n\n")

    if CLUSTER:
        timeout = int(math.ceil(1.1 * sut.timeWeight * MINUTES_PER_RUN * 60))
        errorMsg = "ERROR: timeout for " + sut.name
        command = "timeout " +str(timeout) + "  " + command \
                  + " || ([ $? -eq 124 ] && echo " + errorMsg + " >> " + em_log + " 2>&1" + ")"

    script.write(command + " \n\n")

    return script.getvalue()


def createJobs():

    CONFIGS = getConfigs()

    NRUNS_PER_SUT = (1 + MAX_SEED - MIN_SEED) * len(CONFIGS)
    SUT_WEIGHTS = sum(map(lambda x: x.timeWeight, SUTS))

    state = State(NRUNS_PER_SUT * SUT_WEIGHTS)

    SUTS.sort(key=lambda x: -x.timeWeight)

    for sut in SUTS:

        state.sutsLeft -= 1
        state.resetTmpForNewRun()

        code = ""
        completedForSut = 0

        for seed in range(MIN_SEED, MAX_SEED + 1):

            random.shuffle(CONFIGS)

            for config in CONFIGS:

                if state.counter == 0:
                    code = createOneJob(state, sut, seed, config)

                elif (state.counter + sut.timeWeight) < state.perJob \
                        or not state.hasSpareJobs() or \
                        (NRUNS_PER_SUT - completedForSut < 0.3 * state.perJob / sut.timeWeight):
                    code += addJobBody(state.port, sut, seed, config)
                    state.updateBudget(sut.timeWeight)

                else:
                    writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes())
                    state.resetTmpForNewRun()
                    code = createOneJob(state, sut, seed, config)
                completedForSut += 1

        if state.opened:
            writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes())

    print("Generated scripts: " + str(state.generated))
    print("Max wait for a job: " + str(max(state.waits)) + " minutes")
    print("Median wait for a job: " + str(statistics.median(state.waits)) + " minutes")
    print("Budget left: " + str(state.budget))
    print("Total time: " + str(sum(state.waits) / 60) + " hours")
    print("Total budget: " + str(CPUS * sum(state.waits) / 60) + " hours")



############################################################################
### Custom
### Following will need to be changed based on what kind of experiments
### we want to run.
############################################################################


class Config:
    def __init__(self, blackBox, algorithm):
        self.blackBox = blackBox
        self.bbExperiments = blackBox
        self.algorithm = algorithm



def customParameters(seed, config):

    params = ""

    label = str(config.algorithm)

    ### Custom for these experiments
    params += " --testSuiteFileName=EM_" + label + "_" + str(seed) + "_Test"
    params += " --blackBox=" + str(config.blackBox)
    params += " --bbExperiments=" + str(config.blackBox)
    params += " --algorithm=" + str(config.algorithm)

    return params


def getConfigs():

    # array of configuration objects. We will run experiments for each of
    # these configurations
    CONFIGS = []

    CONFIGS.append(Config(False, "RANDOM"))
    CONFIGS.append(Config(False, "MIO"))

    return CONFIGS


############################################################################
#### END of custom configurations
############################################################################



# Create the actual job scripts
createJobs()

# Create a single ./runall.sh script to submit all the job scripts
createRunallScript()
