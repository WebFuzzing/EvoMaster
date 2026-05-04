import sys
import os
import subprocess


def main():
    print("Work in Progress - The release of WebFuzzing/EvoMaster on PyPi will hopefully happen soon in 2026")

    base = os.path.dirname(__file__)

    exe = os.path.join(base, "dmg","evomaster.app", "Contents", "MacOS", "evomaster")

    if not os.path.exists(exe):
        print("Error: could not find EvoMaster executable at:", exe, file=sys.stderr)
        sys.exit(1)

    cmd = [exe] + sys.argv[1:]

    # Forward exit code
    result = subprocess.run(cmd)
    sys.exit(result.returncode)

if __name__ == "__main__":
    main()