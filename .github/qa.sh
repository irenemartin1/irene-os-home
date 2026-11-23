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

# 7b. turn on gesture navigation (like the Pixel) and do the system swipe-home from the bottom edge
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural
sleep 4
adb shell am start -a android.settings.SETTINGS
sleep 4
adb shell input swipe $CX $((H - 3)) $CX $((H / 2)) 300
sleep 5; shot 12-after-system-swipe-home; top 12-top
adb shell input swipe $CX $((H - 3)) $CX $((H * 6 / 10)) 300
sleep 4; shot 13-system-swipe-on-home
adb shell input keyevent KEYCODE_HOME
sleep 3
# swipe up from the middle with gesture nav on -> app drawer
adb shell input swipe $CX $((H * 6 / 10)) $CX $((H * 25 / 100)) 250
sleep 3; shot 14-drawer-gesture-nav
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell dumpsys wallpaper | grep -iE "mWidth|mHeight|wallpaper" | head -8 > "$OUT/wallpaper.txt"

# 7c. organize: long-press the first home app and drag it to row 3, column 4
adb shell input keyevent KEYCODE_HOME
sleep 3; shot 15-before-drag
D=$(adb shell wm density | grep -o "[0-9]*" | tail -1)
X1=$(( (12 + 24) * D / 160 + W / 8 - 40 ))
Y1=$(( 180 * D / 160 ))
X2=$(( W - W / 8 ))
Y2=$(( Y1 + 2 * 92 * D / 160 ))
adb shell input draganddrop $X1 $Y1 $X2 $Y2 2500
sleep 3; shot 16-after-drag
# type into the note
adb shell input tap $CX $(( 60 * D / 160 ))
sleep 1
adb shell input text "buy%sfairy%slights"
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 2; shot 17-note

# 7d. every Irene OS theme: home screen (+ the app drawer for a few)
N=20
for T in forest underwater digital nature gallery productivity studio kawaii bull mushroom coquette celestial romantic luxury coastal zen y2k retro cyber academia princess circuit circus disco fitness pride tropical groovy glass wild dogs mediterranean leopard tiles preppy junk grunge indie vsco minimal christmas; do
  adb shell am start -n app.irene.pastel/.MainActivity --es irene_theme $T
  sleep 4; shot "$N-theme-$T"
  case $T in kawaii|luxury|celestial|cyber|dogs|christmas|forest)
    adb shell input swipe $CX $((H * 6 / 10)) $CX $((H * 25 / 100)) 250
    sleep 3; shot "$N-theme-$T-drawer"
    adb shell input keyevent KEYCODE_BACK; sleep 2;;
  esac
  N=$((N + 1))
done

# 8. let it run a while, then check it is still alive + smooth
sleep 20
adb shell pidof app.irene.pastel > "$OUT/pid.txt"
adb shell dumpsys gfxinfo app.irene.pastel > "$OUT/gfxinfo.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
adb logcat -d | grep -iE "irene|pastel|AndroidRuntime|FATAL|ANR|Choreographer" > "$OUT/log-filtered.txt"
echo "no crash" >> "$OUT/crash.txt"
find "$OUT" -type f -empty -delete
exit 0
