#!/usr/bin/env python

# When we generate an experiment folder (FOLDER) with exp.py script on local machine,
# with K generated bash scripts, we can use this schedule.py to run all these scripts
# with N of them in parallel. The others (assuming N << K) will be started as soon as
# one current running job is completed

import random
import sys
import os
import subprocess
import time
import platform
import pathlib

# Note: here we for flush on ALL prints, otherwise we would end up with messed up logs

if len(sys.argv) != 3:
    print("Usage:\nschedule.py <N> <FOLDER>", flush=True)
    exit(1)

# The number of jobs to run in parallel
N = int(sys.argv[1])

if N < 1:
    print("Invalid value for N: " + str(N), flush=True)
    exit(1)

# Location of experiment folder
FOLDER = sys.argv[2]

SHELL = platform.system() == 'Windows'

# Changed, as now we might have few subfolders with scripts, eg for generated tests
# SCRIPTS_FOLDER = os.path.join(FOLDER, "scripts")
SCRIPTS_FOLDER = pathlib.PurePath(FOLDER).as_posix()

buffer = []

#collect name of all bash files
scripts = [f for f in os.listdir(SCRIPTS_FOLDER) if os.path.isfile(os.path.join(SCRIPTS_FOLDER, f))  and f.endswith(".sh")]

print("There are " + str(len(scripts)) + " Bash script files", flush=True)

random.shuffle(scripts)

k = 1

def runScript(s):
    global k
    print("Running script " + str(k)+ "/"+ str(len(scripts)) +": " + s, flush=True)
    k = k + 1

    command = ["bash", s]

    handler = subprocess.Popen(command, shell=SHELL, cwd=SCRIPTS_FOLDER, start_new_session=True)
    buffer.append(handler)

for s in scripts:
    if len(buffer) < N:
       runScript(s)
    else:
        while len(buffer) == N:
            for h in buffer:
                h.poll()
                if h.returncode is not None and h.returncode != 0:
                    print("Process terminated with code: " + str(h.returncode), flush=True)

            # keep the ones running... those have return code not set yet
            buffer = [h for h in buffer if h.returncode is None]
            if len(buffer) == N :
                time.sleep(5)
            else:
                runScript(s)
                break

print("Waiting for last scripts to end", flush=True)

for h in buffer:
    h.wait()
    if h.returncode != 0:
        print("Process terminated with code: " + str(h.returncode), flush=True)

print("All jobs are completed", flush=True)

#TODO how to make sure no subprocess is left hanging?


