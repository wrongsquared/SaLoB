#!/usr/bin/env python3
"""Boot all SaLoB services.

Runs backend/run.py (which opens terminals for each backend service),
then opens a terminal for the frontend.

Usage:
  python3 boot_all.py         # normal profile
  python3 boot_all.py --dev   # dev profile for food-service & user-service
"""

import argparse
import os
import platform
import shutil
import subprocess
import time

ROOT = os.path.dirname(os.path.abspath(__file__))


def _open_terminal(name: str, cwd: str, cmd: list[str]) -> None:
    print(f"  Opening terminal for {name} ...")
    if platform.system() == "Windows":
        subprocess.Popen(
            ["start", "cmd", "/k", f"title {name} && {' '.join(cmd)}"],
            shell=True,
            cwd=cwd,
        )
    elif shutil.which("alacritty"):
        subprocess.Popen(["alacritty", "-T", name, "-e", *cmd], cwd=cwd)
    elif shutil.which("gnome-terminal"):
        subprocess.Popen(["gnome-terminal", "--title", name, "--", *cmd], cwd=cwd)
    else:
        subprocess.Popen(["xterm", "-title", name, "-e", *cmd], cwd=cwd)


def main() -> None:
    parser = argparse.ArgumentParser(description="Boot all SaLoB services")
    parser.add_argument(
        "--dev",
        action="store_true",
        help="Use dev profile for food-service & user-service",
    )
    args = parser.parse_args()

    print("=== Booting all SaLoB services ===")

    # Backend in their own terminals (launched by backend/run.py)
    backend_cmd = ["python3", "run.py"]
    if args.dev:
        backend_cmd.append("--dev")
    subprocess.Popen(backend_cmd, cwd=os.path.join(ROOT, "backend"))
    time.sleep(0.333)

    # Frontend in its own terminal
    _open_terminal("Frontend", os.path.join(ROOT, "frontend"), ["npm", "run", "dev"])

    print("  All services launched. Close terminal windows to stop.")


if __name__ == "__main__":
    main()
