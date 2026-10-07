import sys

for line in sys.stdin:
    if line.strip():
        print("ERROR: simulated error", flush=True)
