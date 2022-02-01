#!/usr/bin/env python

import random
import sys
import os
import subprocess
import time

if len(sys.argv) != 3:
    print("Usage:\nschedule.py <N> <FOLDER> ")
    exit(1)


N = int(sys.argv[1])

if(N < 1)
    print("Invalid value for N: " + str(N))
    exit(1)

FOLDER = sys.argv[2]

# SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))
# SHELL = platform.system() == 'Windows'


buffer = []

scripts = [f for f in os.listdir(FOLDER) if os.path.isfile(os.path.join(FOLDER, f)) and f.startswith("evomaster") and f.endswith(".sh")]

print("There are " + str(len(scripts)) + " EvoMaster script files")

random.shuffle(scripts)

k = 1

def runScript(s):
    print("Running script " + str(k)+ "/"+ str(len(scripts)) +": " + s)
    nonlocal k
    k = k + 1
    handler = subprocess.Popen(os.path.join(FOLDER, s))
    buffer.append(handler)

for s in scripts:
    if len(buffer) < N:
       runScript(s)
    else:
        while len(buffer) == N:
            for h in buffer:
                h.poll()
            buffer = [h for h in buffer if h.returncode is None]
            if len(buffer) == N :
                time.sleep(5)
            else:
                runScript(s)
                break

print("Waiting for last scripts to end")

for h in buffer:
    h.wait()

print("All jobs are completed")



