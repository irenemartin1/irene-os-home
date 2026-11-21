#!/usr/bin/env bash
# Acts like a person using Irene OS as their real home screen, saving screenshots + logs.
set -x
OUT=qa-out
mkdir -p "$OUT"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
top() { adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -2 > "$OUT/$1.txt"; }

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity app.irene.pastel/.MainActivity
adb logcat -c

SIZE=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
W=${SIZE%x*}; H=${SIZE#*x}
CX=$((W / 2))

# 1. press Home -> should be Irene OS
adb shell input keyevent KEYCODE_HOME
sleep 10; shot 01-home; top 01-top

# 2. swipe up -> app drawer
adb shell input swipe $CX $((H * 7 / 10)) $CX $((H * 2 / 10)) 250
sleep 4; shot 02-drawer

# 3. back -> home
adb shell input keyevent KEYCODE_BACK
sleep 3; shot 03-back-home

# 4. open another app, press Home -> back on Irene OS
adb shell am start -a android.settings.SETTINGS
sleep 5
adb shell input keyevent KEYCODE_HOME
sleep 5; shot 04-home-after-settings; top 04-top

# 5. the Settings > Home app screen (where you pick the default) then pick home again
adb shell am start -a android.settings.HOME_SETTINGS
sleep 5; shot 05-home-settings
adb shell input keyevent KEYCODE_HOME
sleep 5; shot 06-home-again; top 06-top

# 6. shake 3x -> underwater mode
for i in 1 2 3; do
  adb emu sensor set acceleration 40:0:9.8; sleep 0.35
  adb emu sensor set acceleration 0:9.8:0; sleep 0.45
done
sleep 5; shot 07-underwater
sleep 4; shot 08-underwater-later
adb shell input swipe $CX $((H * 7 / 10)) $CX $((H * 2 / 10)) 250
sleep 4; shot 09-underwater-drawer
adb shell input keyevent KEYCODE_BACK
sleep 2

# 7. recents then home
adb shell input keyevent KEYCODE_APP_SWITCH
sleep 4; shot 10-recents
adb shell input keyevent KEYCODE_HOME
sleep 4; shot 11-final; top 11-top

# 8. let it run a while, then check it is still alive + smooth
sleep 20
adb shell pidof app.irene.pastel > "$OUT/pid.txt"
adb shell dumpsys gfxinfo app.irene.pastel > "$OUT/gfxinfo.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
adb logcat -d | grep -iE "irene|pastel|AndroidRuntime|FATAL|ANR|Choreographer" > "$OUT/log-filtered.txt"
echo "no crash" >> "$OUT/crash.txt"
find "$OUT" -type f -empty -delete
exit 0
