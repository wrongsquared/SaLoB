import argparse
import os
import platform
import subprocess
import sys
import time

all_services = [
    {"name": "Food Service", "path": "./food-service"},
    {"name": "User Service", "path": "./user-service"},
    {"name": "API Gateway", "path": "./api-gateway"},
]

shared_proto = {"name": "Shared Proto", "path": "./shared-proto"}


def gradle_command(tasks):
    script_name = "gradlew.bat" if platform.system() == "Windows" else "./gradlew"
    return [script_name, *tasks]


def run_gradle(name, path, tasks):
    abs_path = os.path.abspath(path)
    print(f"--- {name}: {' '.join(tasks)} ---")
    result = subprocess.run(gradle_command(tasks), cwd=abs_path)
    if result.returncode != 0:
        print(f"!!! {name} failed with exit code {result.returncode}")
        sys.exit(result.returncode)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Build executable JARs for all backend services."
    )
    parser.add_argument(
        "--skip-shared-proto", action="store_true", help="Skip shared-proto publish."
    )
    parser.add_argument(
        "--clean", action="store_true", help="Run 'clean' before building."
    )
    parser.add_argument(
        "--skip-tests", action="store_true", help="Skip tests during build."
    )
    args = parser.parse_args()

    if not args.skip_shared_proto:
        shared_tasks = ["publishToMavenLocal"]
        if args.skip_tests:
            shared_tasks += ["-x", "test"]
        run_gradle(shared_proto["name"], shared_proto["path"], shared_tasks)
        time.sleep(0.2)

    for service in all_services:
        tasks = []
        if args.clean:
            tasks.append("clean")
        tasks.append("bootJar")
        if args.skip_tests:
            tasks += ["-x", "test"]
        run_gradle(service["name"], service["path"], tasks)
        time.sleep(0.2)
