import glob
import os
import platform
import shlex
import shutil
import subprocess
import time

all_services = [
    {"name": "Food Service", "path": "./food-service"},
    {"name": "User Service", "path": "./user-service"},
    {"name": "API Gateway", "path": "./api-gateway"},
]


def get_linux_terminal_command(name, cmd):
    if shutil.which("alacritty"):
        return ["alacritty", "-T", name, "-e", "zsh", "-c", f"{cmd}; zsh"]
    if shutil.which("gnome-terminal"):
        return ["gnome-terminal", "--title", name, "--", "zsh", "-c", f"{cmd}; zsh"]
    return ["xterm", "-title", name, "-e", f"{cmd}; bash"]


def ensure_env_file(service_path):
    env_path = os.path.join(service_path, ".env")
    example_path = env_path + ".example"
    if not os.path.exists(env_path) and os.path.exists(example_path):
        shutil.copyfile(example_path, env_path)
        print(f"--- Created {env_path} from .env.example ---")
    return env_path if os.path.exists(env_path) else None


def load_env_file(env_path):
    if not env_path or not os.path.exists(env_path):
        return {}
    env = {}
    with open(env_path, "r", encoding="utf-8") as file:
        for raw_line in file:
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("export "):
                line = line[len("export ") :]
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip()
            if (value.startswith('"') and value.endswith('"')) or (
                value.startswith("'") and value.endswith("'")
            ):
                value = value[1:-1]
            env[key] = value
    return env


def find_jar(service_path):
    libs_path = os.path.join(service_path, "build", "libs")
    if not os.path.isdir(libs_path):
        return None
    jars = glob.glob(os.path.join(libs_path, "*.jar"))
    if not jars:
        return None
    non_plain = [jar for jar in jars if not jar.endswith("-plain.jar")]
    candidates = non_plain or jars
    return max(candidates, key=os.path.getmtime)


def posix_env_prefix(env_vars):
    if not env_vars:
        return ""
    parts = [f"{key}={shlex.quote(value)}" for key, value in env_vars.items()]
    return " ".join(parts)


def windows_env_prefix(env_vars):
    if not env_vars:
        return ""
    parts = [f'set "{key}={value}"' for key, value in env_vars.items()]
    return " && ".join(parts) + " && "


def run_service(service):
    os_type = platform.system()
    path = os.path.abspath(service["path"])
    name = service["name"]

    env_path = ensure_env_file(path)
    env_vars = load_env_file(env_path)
    jar_path = find_jar(path)

    if not jar_path:
        print(f"!!! {name}: No JAR found in {os.path.join(path, 'build', 'libs')}.")
        print("    Run: python build_jars.py")
        return

    print(f"--- Launching {name} ---")

    if os_type == "Windows":
        env_prefix = windows_env_prefix(env_vars)
        cmd = f'cd /d "{path}" && {env_prefix}java -jar "{jar_path}"'
        subprocess.Popen(f'start cmd /k "title {name} && {cmd}"', shell=True)

    elif os_type == "Linux":
        env_prefix = posix_env_prefix(env_vars)
        cmd = f"cd {shlex.quote(path)} && "
        if env_prefix:
            cmd += f"{env_prefix} "
        cmd += f"java -jar {shlex.quote(jar_path)}"
        term_cmd = get_linux_terminal_command(name, cmd)
        subprocess.Popen(term_cmd)

    elif os_type == "Darwin":
        env_prefix = posix_env_prefix(env_vars)
        cmd = f"cd {shlex.quote(path)} && "
        if env_prefix:
            cmd += f"{env_prefix} "
        cmd += f"java -jar {shlex.quote(jar_path)}"
        script = f'tell application "Terminal" to do script "{cmd}"'
        subprocess.Popen(["osascript", "-e", script])


if __name__ == "__main__":
    for service in all_services:
        run_service(service)
        time.sleep(0.333)
