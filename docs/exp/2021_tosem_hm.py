#!/usr/bin/python

import argparse
import io
import os
import pathlib
import shutil
import stat
import statistics
import math

############################################################################
### cluster settings
############################################################################
EXP_ID = "evomaster"
ACCOUNT = None
CPUS = 3
TIMEOUT_SUT_START_MINUTES = 20
CONTROLLER_PID = "CONTROLLER_PID"
MINUTES_PER_RUN = 200

############################################################################
### general settings
############################################################################
JDK_8 = "JDK_8"
JDK_11 = "JDK_11"
JS = "JS"

JAVA_HOME_8 = os.environ.get("JAVA_HOME_8", "")
JAVA_HOME_11 = os.environ.get("JAVA_HOME_11", "")

EVOMASTER_JAVA_OPTIONS = " -Xms2G -Xmx4G  -jar evomaster.jar "
AGENT = "evomaster-agent.jar"
EM_POSTFIX = "-evomaster-runner.jar"
SUT_POSTFIX = "-sut.jar"

############################################################################
### where to find evomaster, suts and results
############################################################################
DEFAULT_FOLDER = "ahw-evomaster-exp"
EVOMASTER_DIR = os.environ.get("EVOMASTER_DIR", "")
EMB_DIR = os.environ.get('EMB_DIR', "")
BASE_DIR = os.path.abspath(DEFAULT_FOLDER)
REPORT_DIR = BASE_DIR + "/reports"
SCRIPT_DIR = BASE_DIR + "/scripts"
TEST_DIR = BASE_DIR + "/tests"
LOGS_DIR = BASE_DIR + "/logs"

ROOT_SCRIPT = BASE_DIR + "/runall.sh"

############################################################################
### sut settings and configuration
############################################################################
class Sut:
    def __init__(self, name, timeWeight, db, platform):
        self.name = name
        # the higher value, the more time it will need compared to the other SUTS
        self.timeWeight = timeWeight
        # include database
        self.db = db
        # Java? JS? NodeJS
        self.platform = platform

SUTS = [
        Sut("features-service", 1, True, JDK_8),
        Sut("scout-api", 2, True, JDK_8),
        Sut("proxyprint", 2, True, JDK_8),
        Sut("rest-ncs", 2, False, JDK_8),
        Sut("rest-scs", 1, False, JDK_8),
        Sut("rest-news", 1, True, JDK_8),
        Sut("catwatch", 1, True, JDK_8)
    ]

SELECTED_SUT = SUTS

############################################################################
### prehandling
############################################################################

def setSut(args):
    if args.sut is not None:
        global SELECTED_SUT
        if args.sut.lower() == "all":
            SELECTED_SUT = SUTS
        elif args.sut.lower() == "sql":
            SELECTED_SUT = list(filter(lambda x: x.db, SUTS))
        elif args.sut.lower() == "nsql":
            SELECTED_SUT = list(filter(lambda x: not x.db, SUTS))
        else:
            SELECTED_SUT = list(filter(lambda x: x.name.lower() == args.sut.lower(), SUTS))
            if not len(SELECTED_SUT):
                print("ERROR: " + args.sut + " is an improper setting for selecting SUT(s)")
                exit(1)

def checkAndPrehandling(args):
    setSut(args)

    global ROOT_SCRIPT
    global REPORT_DIR
    global SCRIPT_DIR
    global TEST_DIR
    global LOGS_DIR
    global MINUTES_PER_RUN
    global BASE_DIR
    global EVOMASTER_DIR
    global EMB_DIR

    if args.maxMinutesPerRun is not None:
        MINUTES_PER_RUN = args.maxMinutesPerRun

    if args.minSeed > args.maxSeed:
        print("ERROR: min seed is greater than max seed")
        exit(1)

    if args.dir is not None:
        BASE_DIR = os.path.abspath(args.dir)
        REPORT_DIR = BASE_DIR + "/reports"
        SCRIPT_DIR = BASE_DIR + "/scripts"
        TEST_DIR = BASE_DIR + "/tests"
        LOGS_DIR = BASE_DIR + "/logs"
        if not os.path.isdir(BASE_DIR):
            print("creating folder: " + BASE_DIR)
            os.makedirs(BASE_DIR)
        else:
            print("ERROR: target folder already exists")
            exit(1)

    if args.cluster:
        HOME = os.environ['HOME']
        EVOMASTER_DIR = HOME + "/tools"
        if args.evomaster is not None:
            EVOMASTER_DIR = HOME +"/"+ args.evomaster + "/tools"
        EMB_DIR = HOME + "/dist"
        if args.emb is not None:
            EMB_DIR = HOME + "/" + args.emb + "/dist"
        LOGS_DIR = os.environ['USERWORK'] + "/logs"
    else:
        if args.evomaster is not None:
            EVOMASTER_DIR = os.path.abspath(args.evomaster)
        if args.emb is not None:
            EMB_DIR = os.path.abspath(args.emb)

        if EVOMASTER_DIR == "":
            raise Exception(
                "You must specify a EVOMASTER_DIR env variable or a path specifying where evomaster.jar can be found")

        if EMB_DIR == "":
            raise Exception(
                "You must specify a EMB_DIR env variable or a path specifying the '/dist' folder from where EMB repository was cloned")

        if not os.path.exists(EMB_DIR):
            raise Exception(EMB_DIR + " does not exist. Did you run script/dist.py?")

        if JAVA_HOME_8 == "":
            raise Exception("You must specify a JAVA_HOME_8 env variable specifying where JDK 8 is installed")

        # diable jdk 11
        # if JAVA_HOME_11 == "":
        #     raise Exception("You must specify a JAVA_HOME_11 env variable specifying where JDK 11 is installed")

    # creating required dirs
    os.makedirs(REPORT_DIR)
    os.makedirs(SCRIPT_DIR)
    os.makedirs(TEST_DIR)
    os.makedirs(LOGS_DIR)

    ROOT_SCRIPT = BASE_DIR + "/runall.sh"

    # if not on the cluster, we need to copy jars to the exp dir
    if not args.cluster:

        REPORT_DIR = str(pathlib.PurePath(REPORT_DIR).as_posix())
        SCRIPT_DIR = str(pathlib.PurePath(SCRIPT_DIR).as_posix())
        TEST_DIR = str(pathlib.PurePath(TEST_DIR).as_posix())
        LOGS_DIR = str(pathlib.PurePath(LOGS_DIR).as_posix())

        # Due to Windows limitations (ie crappy FS), we need to copy JARs over
        for sut in SELECTED_SUT:
            if sut.platform == JDK_8 or sut.platform == JDK_11:
                # copy jar files
                shutil.copy(os.path.join(EMB_DIR, sut.name + EM_POSTFIX), BASE_DIR)
                shutil.copy(os.path.join(EMB_DIR, sut.name + SUT_POSTFIX), BASE_DIR)
            elif sut.platform == JS:
                # copy folders, which include both SUT and EM Controller
                shutil.copytree(os.path.join(EMB_DIR, sut.name), os.path.join(BASE_DIR, sut.name))
        shutil.copy(os.path.join(EMB_DIR, AGENT), BASE_DIR)
        shutil.copy(os.path.join(EVOMASTER_DIR, "evomaster.jar"), BASE_DIR)

    setExp(args)

def createRunallScript(cluster):
    script = open(ROOT_SCRIPT, "w")

    script.write("#!/bin/bash \n\n")

    script.write("cd \"$(dirname \"$0\")\"\n\n")

    script.write("for s in `ls scripts/*.sh`; do\n")
    script.write("   echo Going to start $s\n")
    if cluster:
        script.write("   sbatch $s\n")
    else:
        script.write("   $s & \n")
    script.write("done \n")

    st = os.stat(ROOT_SCRIPT)
    os.chmod(ROOT_SCRIPT, st.st_mode | stat.S_IEXEC)

def writeScript(code, port, sut):
    script_path = SCRIPT_DIR + "/evomaster_" + str(port) + "_" + sut.name + ".sh"
    script = open(script_path, "w")
    script.write(code)

    st = os.stat(script_path)
    os.chmod(script_path, st.st_mode | stat.S_IEXEC)

    return script

def getScriptHead(timeoutMinutes, cluster):
    s = "#!/bin/bash \n"

    if cluster:
        s += "#SBATCH --job-name=" + EXP_ID + " \n"
        if ACCOUNT is None:
            raise Exception("you require to specify a valid account info for running this experiment")
        s += "#SBATCH --account= "+ACCOUNT+"\n"
        s += "#SBATCH --mem-per-cpu=4G \n"
        s += "#SBATCH --nodes=1 --ntasks-per-node=" + str(CPUS) + " \n"
        s += "#SBATCH --time=" + str(timeoutMinutes) + ":00 \n\n"
    return s


def createJobHead(port, sut, timeoutMinutes, cluster):
    script = io.StringIO()

    script.write(getScriptHead(timeoutMinutes, cluster))

    sut_log = LOGS_DIR + "/log_sut_" + sut.name + "_" + str(port) + ".txt"

    # Start SUT as background process on the given port
    controllerPort = str(port)
    sutPort = str(port + 1)

    if cluster:

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
            sut_em_path = os.path.join(EMB_DIR, sut.name + EM_POSTFIX)
            sut_jar_path = os.path.join(EMB_DIR, sut.name + SUT_POSTFIX)
            agent_path = os.path.join(EMB_DIR, AGENT)
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
        JAVA = getJavaCommand(sut, cluster)
        command = JAVA + jvm + " -jar " + sut.name + EM_POSTFIX + " " + params + " > " + sut_log + " 2>&1 &"

    elif sut.platform == JS:
        # TODO plugin script for js experiment here
        before = "pushd " + sut.name + "\n"
        command = " EM_PORT=" + controllerPort + " npm run em > " + sut_log + " 2>&1 & "
        command = before + command


    if not cluster:
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
    def __init__(self, budget, njobs, port):
        # total budget for the search which is left
        self.budget = budget
        # how many jobs/scripts we still need to create
        self.jobsLeft = njobs
        # to avoid TCP conflicts, each job uses a different port range
        self.port = port

    # number of generated script files, so far
    generated = 0
    # each job will have a different time duration, and we keep track
    # of those durations for every single generated script
    waits = []
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


def writeWithHeadAndFooter(code, port, sut, timeout, cluster):
    head = createJobHead(port, sut, timeout, cluster)
    footer = closeJob(port, sut)
    code = head + code + footer
    writeScript(code, port, sut)



def createOneJob(state, sut, seed, setting, sol_name, budget, stopping_criterion, cluster):
    code = addJobBody(state.port, sut, seed, setting, sol_name, budget, stopping_criterion, cluster)
    state.updateBudget(sut.timeWeight)
    state.jobsLeft -= 1
    state.opened = True
    state.generated += 1
    return code


def getJavaCommand(sut, cluster):
        JAVA = "java "
        if not cluster:
             if sut.platform == JDK_8:
                    JAVA = "\"" + JAVA_HOME_8 +"\"/bin/java "
             elif sut.platform == JDK_11:
                    JAVA = "\"" + JAVA_HOME_11 +"\"/bin/java "
        return JAVA



def addJobBody(port, sut, seed, setting, sol_name, budget, stopping_criterion, cluster):
    script = io.StringIO()

    em_log = LOGS_DIR + "/log_em_" + sut.name + "_" + str(port) + ".txt"

    params = ""

    label = sol_name

    ### Custom for these experiments
    for ps in setting:
        params += " --" + str(ps[0]) + "=" + str(ps[1])
        if is_float(str(ps[1])) and float(ps[1]) <= 1.0:
            label += "_" + str(int(float(ps[1]) * 100))
        else:
            label += "_" + str(ps[1])

    params += " --testSuiteFileName=EM_" + label + "_" + str(seed) + "_Test"

    params += " --dependencyFile=" + \
              REPORT_DIR + "/dependencies_" + sut.name + "_" + label + "_" + str(seed) + ".csv"

    ### standard
    params += " --stoppingCriterion="+stopping_criterion
    if stopping_criterion == "TIME":
        params += " --maxTime="+ str(budget)
    elif stopping_criterion == "FITNESS_EVALUATIONS":
        params += " --maxActionEvaluations=" + str(budget)
    else:
        raise Exception(""+stopping_criterion+ "is not supported by evomaster")

    params += " --statisticsColumnId=" + sut.name
    params += " --seed=" + str(seed)
    params += " --sutControllerPort=" + str(port)
    params += " --outputFolder=" + TEST_DIR + "/" + sut.name
    params += " --statisticsFile=" + \
              REPORT_DIR + "/statistics_" + sut.name + "_" + str(seed) + ".csv"
    params += " --snapshotInterval=5"
    params += " --snapshotStatisticsFile=" + \
              REPORT_DIR + "/snapshot_" + sut.name + "_" + str(seed) + ".csv"
    params += " --exceedTargetsFile=" + \
              REPORT_DIR + "/exceedTarget" + sut.name + "_" + str(seed) + ".txt"
    params += " --appendToStatisticsFile=true"
    params += " --writeStatistics=true"
    params += " --showProgress=false"

    JAVA = getJavaCommand(sut, cluster)
    command = JAVA + EVOMASTER_JAVA_OPTIONS + params + " >> " + em_log + " 2>&1"

    if not cluster:
        script.write("\n\necho \"Starting SUT with: " + command + "\"\n")
        script.write("echo\n\n")

    if cluster:
        timeout = int(math.ceil(1.1 * sut.timeWeight * MINUTES_PER_RUN * 60))
        errorMsg = "ERROR: timeout for " + sut.name
        command = "timeout " + str(timeout) + "  " + command \
                  + " || ([ $? -eq 124 ] && echo " + errorMsg + " >> " + em_log + " 2>&1" + ")"

    script.write(command + " \n\n")

    return script.getvalue()

############################################################################
### experiment settings
############################################################################

class ParameterSetting:
    def __init__(self, name, values):
        self.name = name
        self.values = values
        self.count = len(self.values)

    def pvalue(self, index):
        if index >= len(self.values):
            exit("a value at the index "+ str(index) + " does not exist")
        return (self.name, self.values[index])

class Solution:
    def __init__(self, name, settings):
        self.name = name
        self.settings = settings
        self.numOfSettings = 1
        for s in self.settings:
            self.numOfSettings *= s.count

    def genAllSettings(self):
        all = []
        lst = [0] * len(self.settings)
        while lst is not None:
            e = []
            for i in range(len(lst)):
                e.append(self.settings[i].pvalue(lst[i]))
            all.append(e)
            lst = self.plus1(lst)
        return all


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

    def setting(self, index):
        if index >= self.count:
            exit("a setting at the index "+ str(index) + " does not exist")
        return self.values[index]

class Experiment:
    def __init__(self, name, solutions):
        self.name = name
        self.solutions = solutions
        self.numOfSettings = 0
        for s in self.solutions:
            self.numOfSettings += s.numOfSettings

    def numOfSolutions(self):
        return len(self.solutions)

    def solution(self, index):
        if index >= len(self.solutions):
            exit("a solution at the index "+ str(index) + " does not exist")
        return self.solutions[index]
############################################################################
### adaptive hypermutation experiment settings
############################################################################

#SQL handling
STR_SQL = ParameterSetting("geneMutationStrategy", ["ONE_OVER_N", "ONE_OVER_N_BIASED_SQL"])
STR_SQL_D = ParameterSetting("geneMutationStrategy", ["ONE_OVER_N_BIASED_SQL"])
STR_SQL_BEST = ParameterSetting("geneMutationStrategy", ["ONE_OVER_N_BIASED_SQL"])
WMR_SQL_NONE = ParameterSetting("specializeSQLGeneSelection",[False])
WMR_SQL = ParameterSetting("specializeSQLGeneSelection",[True, False])
WMR_BEST = ParameterSetting("specializeSQLGeneSelection",[True])

#weight-based hypermutation mutation
WMR_F= ParameterSetting("weightBasedMutationRate",[False])
WMR_T = ParameterSetting("weightBasedMutationRate",[True])
D_W_F = ParameterSetting("d",[1.0])
D_W = ParameterSetting("d",[0.2, 0.5, 0.8])
D_W_BEST=ParameterSetting("d",[0.8])
HM_0 = ParameterSetting("startingPerOfGenesToMutate",[0.0])
HM_T = ParameterSetting("startingPerOfGenesToMutate",[0.2, 0.5])
HM_T_BEST = ParameterSetting("startingPerOfGenesToMutate",[0.5])

#adaptive setting
P_APC_0 = ParameterSetting("probOfArchiveMutation",[0.0])
P_APC_T = ParameterSetting("probOfArchiveMutation",[0.5, 0.8])
P_APC_BEST = ParameterSetting("probOfArchiveMutation",[0.5])
STR_APC_NONE = ParameterSetting("adaptiveGeneSelectionMethod",["NONE"])
STR_APC = ParameterSetting("adaptiveGeneSelectionMethod",["APPROACH_IMPACT"])
STR_EAM_NONE = ParameterSetting("archiveGeneMutation",["NONE"])
STR_EAM = ParameterSetting("archiveGeneMutation",["NONE","SPECIFIED_WITH_SPECIFIC_TARGETS"])
STR_EAM_BEST = ParameterSetting("archiveGeneMutation",["SPECIFIED_WITH_SPECIFIC_TARGETS"])

#tracking evaluated mutation
TRACK_EVA_F = ParameterSetting("enableTrackEvaluatedIndividual",[False])
TRACK_EVA_T = ParameterSetting("enableTrackEvaluatedIndividual",[True])

#probablity sampling
PR5 = ParameterSetting("probOfRandomSampling",[0.5])
PR = ParameterSetting("probOfRandomSampling",[0.2, 0.5])

#focused search
FS5 = ParameterSetting("focusedSearchActivationTime",[0.5])
FS = ParameterSetting("focusedSearchActivationTime",[0.5, 0.8])

E1= Solution("E1", [STR_SQL, WMR_SQL_NONE, WMR_F, D_W_F, HM_0, P_APC_0, STR_APC_NONE, STR_EAM_NONE, TRACK_EVA_F, PR5, FS5])
E2= Solution("E2", [STR_SQL_D, WMR_SQL, WMR_T, D_W, HM_0, P_APC_0, STR_APC_NONE, STR_EAM_NONE, TRACK_EVA_F, PR5, FS5])
E3= Solution("E3", [STR_SQL_D, WMR_BEST, WMR_T, D_W, HM_T, P_APC_0, STR_APC_NONE, STR_EAM_NONE, TRACK_EVA_F, PR5, FS5])
E4= Solution("E4", [STR_SQL_D, WMR_BEST, WMR_T, D_W_BEST, HM_T_BEST, P_APC_T, STR_APC, STR_EAM, TRACK_EVA_T, PR5, FS5])
E5= Solution("E5", [STR_SQL_D, WMR_BEST, WMR_T, D_W_BEST, HM_T_BEST, P_APC_BEST, STR_APC, STR_EAM_BEST, TRACK_EVA_T, PR, FS])
E6= Solution("E6", [STR_SQL_BEST, WMR_SQL_NONE, WMR_F, D_W_F, HM_0, P_APC_0, STR_APC_NONE, STR_EAM_NONE, TRACK_EVA_F, PR, FS])

AHW_EXP = Experiment("AHW",[E1, E2, E3, E4, E5, E6])

############################################################################
### configure experiments
############################################################################

EXPS = [AHW_EXP]
SELECTED_EXP = EXPS[0]
SELECTED_SOLUTION= SELECTED_EXP.solutions

def setExp(args):
    global SELECTED_EXP
    global SELECTED_SOLUTION

    if args.setting is not None:
        SELECTED_EXP = list(filter(lambda x: x.name.lower() == args.setting.lower(), EXPS))[0]
        if SELECTED_EXP is None:
            raise Exception("" + args.setting + " cannot be found.")
        SELECTED_SOLUTION = SELECTED_EXP.solutions

    if args.solution is not None:
        if args.solution.lower() == "all":
            SELECTED_SOLUTION = SELECTED_EXP.solutions
        else:
            SELECTED_SOLUTION = list(filter(lambda x: x.name.lower() == args.solution.lower(), SELECTED_EXP.solutions))
            if not len(SELECTED_SOLUTION):
                raise Exception(""+args.solution+" cannot be found in the setting "+ SELECTED_EXP.name)



def createJobs(minSeed, maxSeed, njobs, portSeed, budget, stoppingCriterion, cluster):

    num = 0
    for s in SELECTED_SOLUTION:
        num += s.numOfSettings
    NRUNS_PER_SUT = (1 + maxSeed - minSeed) * num

    SUT_WEIGHTS = sum(map(lambda x: x.timeWeight, SELECTED_SUT))

    state = State(NRUNS_PER_SUT * SUT_WEIGHTS, njobs, portSeed)

    SELECTED_SUT.sort(key=lambda x: -x.timeWeight)

    for sut in SELECTED_SUT:
        weight = sut.timeWeight

        state.sutsLeft -= 1
        state.resetTmpForNewRun()

        code = ""
        completedForSut = 0

        for seed in range(minSeed, maxSeed + 1):

            for solution in SELECTED_SOLUTION:
                sol_name = solution.name
                print("processing " + sol_name +" for "+sut.name)

                for setting in solution.genAllSettings():

                    if state.counter == 0:
                        code = createOneJob(state, sut, seed, setting, sol_name, budget, stoppingCriterion, cluster)

                    elif (state.counter + weight) < state.perJob \
                            or not state.hasSpareJobs() or \
                            (NRUNS_PER_SUT - completedForSut < 0.3 * state.perJob / weight):
                        code += addJobBody(state.port, sut, seed, setting, sol_name, budget, stoppingCriterion, cluster)
                        state.updateBudget(weight)

                    else:
                        writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes(), cluster)
                        state.resetTmpForNewRun()
                        code = createOneJob(state, sut, seed, setting, sol_name, budget, stoppingCriterion, cluster)
                    completedForSut += 1

        if state.opened:
            writeWithHeadAndFooter(code, state.port, sut, state.getTimeoutMinutes(), cluster)

    print("Generated scripts: " + str(state.generated))
    print("Max wait for a job: " + str(max(state.waits)) + " minutes")
    print("Median wait for a job: " + str(statistics.median(state.waits)) + " minutes")
    print("Budget left: " + str(state.budget))
    print("Total time: " + str(sum(state.waits) / 60) + " hours")
    print("Total budget: " + str(CPUS * sum(state.waits) / 60) + " hours")

def is_float(input):
  try:
    float(input)
  except ValueError:
    return False
  return True

############################################################################
### Main
############################################################################
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('--stoppingCriterion',
                        help='a criterion to terminate the search, i.e., TIME or FITNESS_EVALUATIONS. DEFAULT is TIME.',
                        type=str, required=False, default="TIME")
    parser.add_argument('--budget',
                        help='a search budget. DEFAULT is 5 minutes (5m).',
                        type=str, required=False, default="5m")
    parser.add_argument('--setting',
                        help='a predefined experiment setting. Now only AHW is available.',
                        type=str, required=False, default="AHW")
    parser.add_argument('--solution',
                        help='a specified solution (e.g., E1) for the employed experiment setting. DEFAULT is all.',
                        type=str, required=False, default=None)
    parser.add_argument('--minSeed',
                        help="Experiments are repeated a certain number of times, with different seed for the random generator. This specify the starting seed. DEFAULT is 1.",
                        type=int, required=False, default=1)
    parser.add_argument('--maxSeed',
                        help="Max seed, included. For example, if running min=10 and max=39, each experiment is  going to be repeated 30 times, starting from seed 10 to seed 39 (both included). DEFAULT is 2.",
                        type=int, required=False, default=2)
    # base seeds used for EM runs. TCP port bindings will be based on such seed.
    # If running new experiments while some previous are still running, to avoid TCP port
    # conflict, can use an higher base seed. Each EM run reserves 10 ports. So, if you run
    # 500 jobs with starting seed 10000, you will end up using ports up to 15000
    parser.add_argument('--portSeed',
                        help="port seed used for EM runs. TCP port bindings will be based on such seed. DEFAULT is 30000.",
                        type=int, required=False, default=30000)
    parser.add_argument('--sut',
                        help="specifying including a sut in this experiment which can be all, sql, nsql or a name of the sut. DEFAULT is all.",
                        type=str, required=False, default="all")
    parser.add_argument('--dir',
                        help="when creating a new set of experiments, all needed files will be saved in a folder. DEFAULT is all.",
                        type=str, required=False, default=DEFAULT_FOLDER)
    parser.add_argument('--evomaster',
                        help="a folder where the evomaster.jar is",
                        type=str, required=False, default=None)
    parser.add_argument('--emb',
                        help="a folder where jars of suts are",
                        type=str, required=False, default=None)
    parser.add_argument('--cluster',
                        help="whether .sh are meant to run on cluster or locally. DEFAULT is False.",
                        type=bool, required=False, default=False)
    # How many minutes we expect each EM run to last AT MOST.
    # Warning: if this value is under-estimated, it will happen the cluster will kill jobs
    # that are not finished withing the specified time.
    parser.add_argument('--maxMinutesPerRun',
                        help="How many minutes we expect each EM run to last AT MOST",
                        type=int, required=False, default=None)
    # How many scripts M we want the N jobs to be divided into.
    # Note: on cluster we can at most submit 400 scripts.
    # Also not that in the same .sh script there can be experiments only for a single SUT.
    parser.add_argument('--njobs',
                        help="how many scripts M we want the N jobs to be divided into. If njobs is 1, there would be one script per sut. DEFAULT is 1.",
                        type=int, required=False, default="1")


    args = parser.parse_args()

    ## global variable can only be changed with this method. This design might be modified later.
    checkAndPrehandling(args)

    createJobs(args.minSeed, args.maxSeed, args.njobs, args.portSeed, args.budget, args.stoppingCriterion, args.cluster)
    createRunallScript(args.cluster)



