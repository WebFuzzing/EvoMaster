#!/usr/bin/env python3


# for s in `ls *.sh`; do sbatch $s; done
# qlogin --account=nn9476k

# http://www.uio.no/english/services/it/research/hpc/abel/help/user-guide/queue-system.html
#
# Max 400 submitted jobs per user at any time.


# Some useful commands as a user on the Abel system are:
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


import os
import random
import shutil
import sys

EXP_ID = "mio_ext"

HOME = os.environ['HOME']

# How to run EvoMaster
EVOMASTER = "java  -Xmx2048m  -jar " + HOME + "/tools/evomaster.jar"
AGENT = HOME + "/tools/evomaster-client-java-instrumentation-0.0.2-SNAPSHOT.jar"

CASESTUDY_DIR = HOME + "/casestudies/rest"



if len(sys.argv) != 6:
    print("Usage:\n<nameOfScript>.py <dir> <minSeed> <maxSeed> <maxFitness> <hours>")
    exit(1)

BASE_DIR = os.path.abspath(sys.argv[1])
MIN_SEED = int(sys.argv[2])
MAX_SEED = int(sys.argv[3])
MAX_FITNESS = int(sys.argv[4])
HOURS = int(sys.argv[5])


def getScriptHead():
    s = "#!/bin/bash \n"
    s += "#SBATCH --job-name=em-smart \n"
    s += "#SBATCH --account=nn9476k \n"
    s += "#SBATCH --mem-per-cpu=4096m \n"
    s += "#SBATCH --cpus-per-task=3 \n"
    s += "#SBATCH --time=" + str(HOURS) + ":00:00 \n\n"
    return s


if MIN_SEED > MAX_SEED:
    print("ERROR: min seed is greater than max seed")
    exit(1)

if not os.path.isdir(BASE_DIR):
    print("creating folder: " + BASE_DIR)
    os.makedirs(BASE_DIR)
else:
    print("ERROR: target folder already exists")
    exit(1)

# Where to put stuff (default in subdirs of BASEDIR)
REPORT_DIR = BASE_DIR + "/reports"
os.makedirs(REPORT_DIR)

SCRIPT_DIR = BASE_DIR + "/scripts"
os.makedirs(SCRIPT_DIR)

TEST_DIR = BASE_DIR + "/tests"
os.makedirs(TEST_DIR)

ALL_LOGS = HOME + "/nobackup/logs"
shutil.rmtree(ALL_LOGS, ignore_errors=True)
LOG_DIR = ALL_LOGS + "/" + EXP_ID
os.makedirs(LOG_DIR)

POSTFIX = "-evomaster-runner.jar"

SUTS = [f for f in os.listdir(CASESTUDY_DIR) if f.endswith(POSTFIX) and
        os.path.isfile(os.path.join(CASESTUDY_DIR, f))]

if len(SUTS) == 0:
    print("ERROR: No JAR files in " + CASESTUDY_DIR)

# Fixed set of parameters to use in all jobs
FIXED = " --maxFitnessEvaluations=" + str(MAX_FITNESS) + \
        " --appendToStatisticsFile=true" \
        " --writeStatistics=true" \
        " --alg=WTS"


####################################

def createJobs(port, sut, seed):
    sut_name = sut[0:len(sut) - len(POSTFIX)]
    sut_path = os.path.join(CASESTUDY_DIR, sut)

    script_path = SCRIPT_DIR + "/evomaster_" + str(seed) + "_" + sut_name + ".sh"
    script = open(script_path, "a")

    script.write(getScriptHead())

    sut_log = LOG_DIR + "/log_sut_" + sut_name + "_" + str(seed) + ".txt"
    em_log = LOG_DIR + "/log_em_" + sut_name + "_" + str(seed) + ".txt"

    # Start SUT as background process on the given port
    controllerPort = str(port)
    sutPort = str(port + 1)
    sutDir = CASESTUDY_DIR
    params = " " + controllerPort + " " + sutPort + " " + sutDir + " " + str(seed)

    # properties
    props = " -Devomaster.instrumentation.jar.path=" + AGENT + " "

    script.write("java -Xmx1024m " + props + " -jar " + sut_path + " " + params + " > " + sut_log + " 2>&1 & \n\n")
    script.write("sleep 20 \n\n")  # wait a bit to be sure the SUT handler can respond


    # SMART = [0.0, 0.2, 0.4, 0.6, 0.8, 1.0]
    SMART = [0.0, 0.8]
    random.shuffle(SMART)

    for smart in SMART:
        params = "  --probOfSmartSampling=" + str(smart)
        params += " --statisticsColumnId=" + sut_name
        params += " --seed=" + str(seed)
        params += " --sutControllerPort=" + controllerPort
        params += " --outputFolder=" + TEST_DIR + "/" + sut_name
        params += " --testSuiteFileName=EM_" + str(smart) + "_" + str(seed) + "_Test"
        params += " --statisticsFile=" + \
                  REPORT_DIR + "/statistics_" + sut_name + "_" + str(seed) + ".csv"

        script.write(EVOMASTER + FIXED + params + " >> " + em_log + " 2>&1 \n\n")

    script.close()


####################################

port = 40000
for seed in range(MIN_SEED, MAX_SEED + 1):
    for sut in SUTS:
        createJobs(port, sut, seed)
        port += 10
