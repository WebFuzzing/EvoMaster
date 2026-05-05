import sys
import os
import subprocess


def main():
    base = os.path.dirname(__file__)

    exe = None

    osx = os.path.join(base, "release","evomaster.app", "Contents", "MacOS", "evomaster")
    if os.path.exists(osx):
        exe = osx

    windows = os.path.join(base, "release","evomaster", "evomaster.exe")
    if os.path.exists(windows):
        exe = windows

    linux = os.path.join(base, "release","evomaster", "bin", "evomaster")
    if os.path.exists(linux):
        exe = linux

    if exe is None:
        print("Error: could not find EvoMaster executable. This mean the release of EvoMaster failed for your OS."
              " This is a critical issue. Please report this bug on evomaster.org", file=sys.stderr)
        sys.exit(1)

    cmd = [exe] + sys.argv[1:]

    # Forward exit code
    result = subprocess.run(cmd)
    sys.exit(result.returncode)

if __name__ == "__main__":
    main()