import sys
import os
import subprocess
import shutil
import platform
import tarfile
import zipfile
import requests
from tqdm import tqdm

evomaster_version = "6.0.1"
jdk_version = 17

def download_file(url, target_path) -> None:

    # Create parent directories if needed
    directory = os.path.dirname(target_path)

    # could just pass file name, which would have no parent folder
    if directory:
        os.makedirs(directory, exist_ok=True)

    temp_path = f"{target_path}.tmp"
    # in case it lingers from previous failed attempts
    if os.path.exists(temp_path):
        os.remove(temp_path)

    try:
        with requests.get(url, stream=True) as response:
            response.raise_for_status()

            total_size = int(response.headers.get("content-length", 0))

            with open(temp_path, "wb") as file, tqdm(
                    desc=os.path.basename(target_path),
                    total=total_size,
                    unit="iB",
                    unit_scale=True,
                    unit_divisor=1024,
            ) as progress_bar:

                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        file.write(chunk)
                        progress_bar.update(len(chunk))

        # Atomic rename after successful download
        os.replace(temp_path, target_path)

    except Exception:
        # Remove incomplete temp file
        if os.path.exists(temp_path):
            os.remove(temp_path)

        print(f"ERROR: could not download {url} to {target_path}."
              f" If problem persists, you can download the file manually into that folder.", file=sys.stderr)

        raise


def find_java_home(jdk_dir: str) -> str:

    system = platform.system()

    # macOS JDK bundle layout
    macos_home = os.path.join(jdk_dir, "Contents", "Home")

    if system == "Darwin" and os.path.exists(macos_home):
        return macos_home

    # Linux / Windows standard layout
    return jdk_dir


def install_corretto(base_dir: str, jdk_folder_name: str) -> str:

    system = platform.system()
    machine = platform.machine().lower()

    # Normalize architecture names
    if machine in ("x86_64", "amd64"):
        arch = "x64"
    elif machine in ("arm64", "aarch64"):
        arch = "aarch64"
    else:
        raise RuntimeError(f"Unsupported architecture: {machine}")

    # Select correct Corretto download URL
    if system == "Linux":
        url = (
            f"https://corretto.aws/downloads/latest/"
            f"amazon-corretto-{jdk_version}-{arch}-linux-jdk.tar.gz"
        )
        extension = ".tar.gz"

    elif system == "Darwin":
        url = (
            f"https://corretto.aws/downloads/latest/"
            f"amazon-corretto-{jdk_version}-{arch}-macos-jdk.tar.gz"
        )
        extension = ".tar.gz"

    elif system == "Windows":

        if arch != "x64":
            raise RuntimeError(f"Windows ARM is currently unsupported by Corretto {jdk_version} downloads")

        url = (
            "https://corretto.aws/downloads/latest/"
            f"amazon-corretto-{jdk_version}-x64-windows-jdk.zip"
        )

        extension = ".zip"

    else:
        raise RuntimeError(f"Unsupported operating system: {system}")

    # Create installation directory
    base_dir = os.path.expanduser(base_dir)
    os.makedirs(base_dir, exist_ok=True)

    archive_path = os.path.join(base_dir, f"corretto{jdk_version}{extension}")

    # Download archive
    download_file(url, archive_path)

    # Temporary extraction directory
    extract_dir = os.path.join(base_dir, "jdk_extract")

    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)

    os.makedirs(extract_dir)

    # Extract archive
    if extension == ".zip":
        with zipfile.ZipFile(archive_path, "r") as zf:
            zf.extractall(extract_dir)

    else:
        with tarfile.open(archive_path, "r:gz") as tf:
            tf.extractall(extract_dir)

    # Remove downloaded archive
    os.remove(archive_path)

    # Find extracted JDK directory
    entries = os.listdir(extract_dir)

    if len(entries) != 1:
        raise RuntimeError(
            f"Unexpected archive contents: {entries}"
        )

    jdk_dir = os.path.join(extract_dir, entries[0])

    # Final installation location
    final_dir = os.path.join(base_dir, jdk_folder_name)

    if os.path.exists(final_dir):
        shutil.rmtree(final_dir)

    os.replace(jdk_dir, final_dir)

    # Cleanup temporary extraction directory
    shutil.rmtree(extract_dir)

    return final_dir


def is_java_working(java: str) -> bool:
    try:
        result = subprocess.run(
            [java, "-version"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        return result.returncode == 0

    except FileNotFoundError:
        return False


def main():

    evomaster_folder = os.path.join(os.path.expanduser("~"),".evomaster")

    jar_path = os.path.join(evomaster_folder,evomaster_version,"evomaster.jar")

    if not os.path.exists(jar_path):
        evomaster_url = f"https://github.com/WebFuzzing/EvoMaster/releases/download/v{evomaster_version}/evomaster.jar"
        print("Downloading evomaster.jar from " + evomaster_url)
        download_file(evomaster_url, jar_path)

    ### NOTE: removed, as it might be brittle
    # if shutil.which("java") is not None:
    #     print(f"Going to use already installed Java. Note you will need to have at least JDK {jdk_version} to be able to run EvoMaster.")
    #     java = "java"
    # else:
    jdk_dir_name = "jdk"
    jdk_folder = os.path.join(evomaster_folder, jdk_dir_name)
    if not os.path.exists(jdk_folder):
        print(f"Going to install a JDK under {evomaster_folder}")
        install_corretto(evomaster_folder, jdk_dir_name)
    else:
        print(f"Going to use JDK installed under {jdk_folder}")
    java_home = find_java_home(jdk_folder)
    java = os.path.join(java_home, "bin", "java.exe" if platform.system() == "Windows" else "java")

    if not is_java_working(java):
        print(f"ERROR: failed to run installed Java under {java}."
              f" Please report this issue at https://github.com/WebFuzzing/EvoMaster/issues ,"
              f" stating which OS and architecture you are using."
              f" Meanwhile, you can manually download a JDK {jdk_version} and unpack it under {jdk_folder}")

    cmd = [java, "-jar", jar_path] + sys.argv[1:]

    result = subprocess.run(cmd)
    sys.exit(result.returncode)

if __name__ == "__main__":
    main()