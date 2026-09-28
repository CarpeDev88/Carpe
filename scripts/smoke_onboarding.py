"""Exercise onboarding on an isolated CI emulator using accessible text."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/carpe-ui.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/carpe-ui.xml"))


def locate(label, tap=False):
    for attempt in range(8):
        for node in tree().iter("node"):
            if label in (node.get("text", "") + node.get("content-desc", "")):
                bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                    if tap:
                        adb("shell", "input", "tap", str((bounds[0]+bounds[2])//2),
                            str((bounds[1]+bounds[3])//2))
                        time.sleep(0.5)
                    return
        size = list(map(int, re.findall(r"\d+", adb("shell", "wm", "size"))))[-2:]
        width, height = size
        adb("shell", "input", "swipe", str(width//2), str(height*3//4),
            str(width//2), str(height//3), "300")
    raise AssertionError("Missing accessible UI: " + label)


def screenshot(name):
    adb("shell", "screencap", "-p", "/sdcard/carpe.png")
    adb("pull", "/sdcard/carpe.png", "carpe-" + name + ".png")


def launch():
    adb("shell", "am", "start", "-W", "-n", "app.carpe/.MainActivity")
    time.sleep(2)


adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
# This emulator is disposable; never run against a personal device.
adb("shell", "pm", "clear", "app.carpe")
launch()
locate("Welcome to CARPE")
screenshot("welcome")
locate("Continue", tap=True)
locate("Do you currently feel your relationship with technology is healthy?")
screenshot("check-in")
locate("Skip this question", tap=True)
locate("What would you like CARPE to help you with first?")
screenshot("needs")
locate("Just explore CARPE", tap=True)
locate("What matters now?")
adb("shell", "am", "force-stop", "app.carpe")
launch()
locate("What matters now?")
locate("Me", tap=True)
locate("Revisit welcome & check-in", tap=True)
locate("Welcome to CARPE")
locate("Continue", tap=True)
locate("Yes, it feels healthy", tap=True)
locate("Let’s support what is working.")
locate("Continue", tap=True)
locate("Protect my focus", tap=True)
locate("A timer that is successful when you stop looking at CARPE.")
screenshot("focus")
assert adb("shell", "pidof", "app.carpe").strip()
print("PASS: optional onboarding, completion persistence, replay, and focus routing")
