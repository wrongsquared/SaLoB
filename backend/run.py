import os
import platform
import subprocess
import shutil
import time

# 1. Separate the Discovery Server from other services
discovery_service = {"name": "Discovery Server", "path": "./eureka-discovery-server"}

other_services = [
    {"name": "Food Service", "path": "./food-service"},
    {"name": "User Service", "path": "./user-service"},
    {"name": "API Gateway", "path": "./api-gateway"}, # Gateway last is best practice
]

def get_linux_terminal_command(name, cmd):
    """Detects available terminal and returns the command to open it."""
    # 1. Try Alacritty (with Zsh)
    if shutil.which("alacritty"):
        # -T is title, -e is the command
        # We wrap in zsh -c and then 'zsh' to keep the window open after gradle stops
        return ["alacritty", "-T", name, "-e", "zsh", "-c", f"{cmd}; zsh"]
    
    # 2. Try Gnome Terminal (Fallback)
    if shutil.which("gnome-terminal"):
        return ["gnome-terminal", "--title", name, "--", "zsh", "-c", f"{cmd}; zsh"]
    
    # 3. Last Resort
    return ["xterm", "-title", name, "-e", f"{cmd}; bash"]

def run_service(service):
    os_type = platform.system()
    path = os.path.abspath(service['path'])
    name = service['name']
    
    # Check for gradlew vs gradlew.bat
    script_name = "gradlew.bat" if os_type == "Windows" else "./gradlew"
    cmd = f"cd {path} && {script_name} bootRun"
    
    print(f"--- Launching {name} ---")

    if os_type == "Windows":
        subprocess.Popen(f'start cmd /k "title {name} && {cmd}"', shell=True)
    
    elif os_type == "Linux":
        term_cmd = get_linux_terminal_command(name, cmd)
        subprocess.Popen(term_cmd)
        
    elif os_type == "Darwin": # Mac
        script = f'tell application "Terminal" to do script "cd {path} && ./gradlew bootRun"'
        subprocess.Popen(['osascript', '-e', script])

if __name__ == "__main__":
    # Eureka server first
    run_service(discovery_service)
    print("Waiting for Discovery Server to initialize...")
    time.sleep(2)
    
    # Start everything else
    for service in other_services:
        run_service(service)
        time.sleep(1) # Small gap so the CPU doesn't spike to 100% instantly
