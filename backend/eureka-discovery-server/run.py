import os
import subprocess
import platform

title = "Eureka Discovery Server"

if platform.system() == "Windows":
    os.system(f'title {title}')
else:
    print(f"\033]0;{title}\a")

subprocess.run(["./gradlew", "bootRun"] if os.name != 'nt' else ["gradlew.bat", "bootRun"])
