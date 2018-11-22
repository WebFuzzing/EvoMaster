#!/usr/bin/env python3

### Python script used to generate Bash scripts to run on a cluster or locally.
#   Given N jobs (ie EvoMaster runs), those will be divided equally among M different
#   Bash scripts.
#   Once such Bash scripts are generated, on the cluster all of those can be submitted with:
#
#   for s in `ls *.sh`; do sbatch $s; done

### Other useful commands:
# scancel --user=<your_username>
# squeue -u <your_username>

### For interactive session:
# qlogin --account=nn9476k

### More info:
# http://www.uio.no/english/services/it/research/hpc/abel/help/user-guide/queue-system.html
#
# Max 400 submitted jobs per user at any time.


### Some other useful commands as a user on the Abel system are:
#
# sbatch <job-script-file>   Submit a job script to the queue system
# squeue                     List of all jobs
# pending                    List of all pending jobs
# qsumm                      Summary information about queue usage
# cost -u                    User CPU Usage information
# cost -p                    Project CPU Usage information
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
import random
import shutil
import stat
import statistics

import math
import sys

### CHANGE_ME
EXP_ID = "an_id_of_your_choice"

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



### We might want to have different settings based on whether we are running the
### scripts on cluster or locally.
if CLUSTER:

    # To ge the SUTs, you need in EMB to run the script "scripts/dist.py" to
    # generate a dist.zip file that you can upload on cluster.
    # Note: the values after the SUT names is multiplicative factor for how long
    # experiments should be run. For example, all SUTs have similar runtime, but
    # proxyprint is roughly twice as slow.

    SUTS = [
            ("features-service", 1),
            ("scout-api", 1),
            ("proxyprint", 2),
            ("rest-ncs", 1),
            ("rest-scs", 1),
            ("rest-news", 1),
            ("catwatch", 1)  # ,
            # ("ocvn", 1) # currently having problems
            ]

    HOME = os.environ['HOME']

    # How to run EvoMaster
    EVOMASTER = "java  -Xms2G -Xmx4G  -jar evomaster.jar"
    EVOMASTER_DIR = HOME + "/tools"
    CASESTUDY_DIR = HOME + "/casestudies/dist"
    LOGS_DIR = HOME + "/nobackup"

## Local configurations
else:
    SUTS = [("ocvn", 1)]  # eg, for debugging for debugging

    # You will need to define environment variables on your OS

    EVOMASTER = os.environ['EVOMASTER']
    CASESTUDY_DIR = os.environ['EMB_DIR']
    LOGS_DIR = BASE_DIR



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
shutil.rmtree(ALL_LOGS, ignore_errors=True)
LOG_DIR = ALL_LOGS + "/" + EXP_ID
os.makedirs(LOG_DIR)

CONTROLLER_PID = "CONTROLLER_PID"

### By default, we allocate 3 CPUs per run.
### Recall that we are running 3 processes, and they are multithreaded.
CPUS = 3


def writeScript(code, port, sut_name):
    script_path = SCRIPT_DIR + "/evomaster_" + str(port) + "_" + sut_name + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

    return script


def getScriptHead(timeoutMinutes):
    s = "#!/bin/bash \n"

    if CLUSTER:
        s += "#SBATCH --job-name=" + EXP_ID + " \n"
        s += "#SBATCH --account=nn9476k \n"
        s += "#SBATCH --mem-per-cpu=4G \n"
        s += "#SBATCH --nodes=1 --ntasks-per-node=" + str(CPUS) + " \n"
        s += "#SBATCH --time=" + str(timeoutMinutes) + ":00 \n\n"
    return s


def createJobHead(port, sut_name, timeoutMinutes):
    script = io.StringIO()

    script.write(getScriptHead(timeoutMinutes))

    sut_log = LOG_DIR + "/log_sut_" + sut_name + "_" + str(port) + ".txt"

    # Start SUT as background process on the given port
    controllerPort = str(port)
    sutPort = str(port + 1)

    EM_POSTFIX = "-evomaster-runner.jar"

    if CLUSTER:
        sut_em_path = os.path.join(CASESTUDY_DIR, sut_name + EM_POSTFIX)
        sut_jar_path = os.path.join(CASESTUDY_DIR, sut_name + ".jar")
        agent_path = os.path.join(CASESTUDY_DIR, "evomaster-agent.jar")

        script.write("\nmodule load java/jdk1.8.0_112\n\n")
        script.write("cd $SCRATCH \n")
        script.write("cp " + EVOMASTER_DIR + "/evomaster.jar . \n")
        script.write("cp " + sut_em_path + " . \n")
        script.write("cp " + sut_jar_path + " . \n")
        script.write("cp " + agent_path + " . \n")
        script.write("\n")

        em_runner = sut_name + EM_POSTFIX
        em_sut = sut_name + ".jar"
        agent = "evomaster-agent.jar"

    else:
        em_runner = CASESTUDY_DIR + "/" + sut_name + EM_POSTFIX
        em_sut = CASESTUDY_DIR + "/" + sut_name + ".jar"
        agent = CASESTUDY_DIR + "/evomaster-agent.jar"

    script.write("\n")

    params = " " + controllerPort + " " + sutPort + " " + em_sut + "  600 "

    # JVM properties
    jvm = " -Xms1G -Xmx4G -Dem.muteSUT=true -Devomaster.instrumentation.jar.path="+agent

    command = "java " + jvm + " -jar " + em_runner + " " + params + " > " + sut_log + " 2>&1 &"

    if not CLUSTER:
        script.write("\n\necho \"Starting EM Runner with: " + command + "\"\n")
        script.write("echo\n\n")

    script.write(command + "\n\n")
    script.write(CONTROLLER_PID + "=$! \n\n")  # store pid of process, so can kill it
    script.write("sleep 20 \n\n")  # wait a bit to be sure the SUT handler can respond

    return script.getvalue()


def closeJob(port, sut_name):
    return "kill $" + CONTROLLER_PID + "\n"


####################################


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
        timeoutMinutes = 10 + int(math.ceil(1.2 * self.counter * MINUTES_PER_RUN))
        self.waits.append(timeoutMinutes)
        return timeoutMinutes

    def resetTmpForNewRun(self):
        self.counter = 0
        self.opened = False
        self.updatePerJob()
        self.updatePort()

    def hasSpareJobs(self):
        return self.jobsLeft > self.sutsLeft


def writeWithHeadAndFooter(code, port, sut_name, timeout):
    head = createJobHead(port, sut_name, timeout)
    footer = closeJob(port, sut_name)
    code = head + code + footer
    writeScript(code, port, sut_name)


############################################################################
### CUSTOM.
### Following will need to be changed based on what kind of experiments
### we want to run.
### At the moment, we just show an example of running experiments based on
### 2 boolean properties: heuristicsForSQL and generateSqlDataWithSearch
############################################################################


def createOneJob(state, sut_name, seed, weight, heuristic, direct):
    code = addJobBody(state.port, sut_name, seed, heuristic, direct)
    state.updateBudget(weight)
    state.jobsLeft -= 1
    state.opened = True
    state.generated += 1
    return code


def addJobBody(port, sut_name, seed, heuristic, direct):
    script = io.StringIO()

    em_log = LOG_DIR + "/log_em_" + sut_name + "_" + str(port) + ".txt"

    params = ""

    label = str(heuristic) + "_" + str(direct)

    ### Custom for these experiments
    params += " --testSuiteFileName=EM_" + label + "_" + str(seed) + "_Test"
    params += " --heuristicsForSQL=" + str(heuristic)
    params += " --generateSqlDataWithSearch=" + str(direct)

    ### standard
    params += " --stoppingCriterion=FITNESS_EVALUATIONS"
    params += " --maxActionEvaluations=" + str(MAX_ACTIONS)
    params += " --statisticsColumnId=" + sut_name
    params += " --seed=" + str(seed)
    params += " --sutControllerPort=" + str(port)
    params += " --outputFolder=" + TEST_DIR + "/" + sut_name
    params += " --statisticsFile=" + \
              REPORT_DIR + "/statistics_" + sut_name + "_" + str(seed) + ".csv"
    params += " --snapshotInterval=5"
    params += " --snapshotStatisticsFile=" + \
              REPORT_DIR + "/snapshot_" + sut_name + "_" + str(seed) + ".csv"
    params += " --appendToStatisticsFile=true"
    params += " --writeStatistics=true"
    params += " --showProgress=false"

    command = EVOMASTER + params + " >> " + em_log + " 2>&1"

    if not CLUSTER:
        script.write("\n\necho \"Starting SUT with: " + command + "\"\n")
        script.write("echo\n\n")

    script.write(command + " \n\n")

    return script.getvalue()


def createJobs():
    H = [False, True]
    D = [False, True]

    ### Here, we need to computer how many experiments we expect per SUT.
    ### For example, we need to take into account how many repetitions, and
    ### how many combinations of settings we experiment on.
    ### Here these are 3 and not 4 because we skip the combination F-T.
    NRUNS_PER_SUT = (1 + MAX_SEED - MIN_SEED) * 3
    SUT_WEIGHTS = sum(map(lambda x: x[1], SUTS))

    state = State(NRUNS_PER_SUT * SUT_WEIGHTS)

    SUTS.sort(key=lambda x: -x[1])

    for sut in SUTS:
        sut_name = sut[0]
        weight = sut[1]

        state.sutsLeft -= 1
        state.resetTmpForNewRun()

        code = ""
        completedForSut = 0

        for seed in range(MIN_SEED, MAX_SEED + 1):

            random.shuffle(H)

            for heuristic in H:

                random.shuffle(D)

                for direct in D:

                    if not heuristic and direct:
                        continue

                    if state.counter == 0:
                        code = createOneJob(state, sut_name, seed, weight, heuristic, direct)

                    elif (state.counter + weight) < state.perJob \
                            or not state.hasSpareJobs() or \
                            (NRUNS_PER_SUT - completedForSut < 0.3 * state.perJob / weight):
                        code += addJobBody(state.port, sut_name, seed, heuristic, direct)
                        state.updateBudget(weight)

                    else:
                        writeWithHeadAndFooter(code, state.port, sut_name, state.getTimeoutMinutes())
                        state.resetTmpForNewRun()
                        code = createOneJob(state, sut_name, seed, weight, heuristic, direct)
                    completedForSut += 1

        if state.opened:
            writeWithHeadAndFooter(code, state.port, sut_name, state.getTimeoutMinutes())

    print("Generated scripts: " + str(state.generated))
    print("Max wait for a job: " + str(max(state.waits)) + " minutes")
    print("Median wait for a job: " + str(statistics.median(state.waits)) + " minutes")
    print("Budget left: " + str(state.budget))
    print("Total time: " + str(sum(state.waits) / 60) + " hours")
    print("Total budget: " + str(CPUS * sum(state.waits) / 60) + " hours")


createJobs()
