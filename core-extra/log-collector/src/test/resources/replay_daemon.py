"""
Mock daemon for unit tests — same stdin/stdout protocol as parser.py but uses
only Python stdlib. Reads log files from log_dir, assigns a deterministic
drain ID per unique line (hash-based), and writes the templates file.
Tests the Java IPC protocol and file-parsing logic without needing loglead.
"""
import hashlib
import os
import sys

ALL_SERVICES = ["client", "code", "key", "refresh-token", "service", "token", "user"]


def process_log_dir(log_dir, output_file):
    templates = []
    for service in ALL_SERVICES:
        log_path = os.path.join(log_dir, f"light-oauth2-repl_v100-oauth2-{service}-1.log")
        if not os.path.exists(log_path):
            continue
        seen = set()
        with open(log_path, "r", encoding="utf-8", errors="replace") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                if line in seen:
                    continue
                seen.add(line)
                drain_id = hashlib.md5(line.encode()).hexdigest()[:8]
                templates.append(f"{service}:{drain_id}\t{line[:120]}")

    with open(output_file, "w", encoding="utf-8") as f:
        for t in sorted(set(templates)):
            f.write(t + "\n")


def daemon_loop(log_dir, output_file):
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) != 2:
            print(f"ERROR: invalid request: {line}", flush=True)
            continue
        try:
            process_log_dir(log_dir, output_file)
            print("OK", flush=True)
        except Exception as e:
            error_msg = str(e).replace("\n", " ").replace("\r", " ")
            print(f"ERROR: {error_msg}", flush=True)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: replay_daemon.py <log_dir> <output_file>", file=sys.stderr)
        sys.exit(1)
    daemon_loop(sys.argv[1], sys.argv[2])
