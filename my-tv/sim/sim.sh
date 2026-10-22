#!/bin/bash
# Drive the Android TV emulator (emulator-5580) like the real TV, for testing the launcher locally.
#   sim.sh theme <theme dir>    push config.json (+ wallpapers/) through the launcher's ThemeProvider
#   sim.sh start                restart the launcher in front
#   sim.sh keys UP RIGHT OK ... press remote keys (D-pad, OK, or KEYCODE_ names), 700 ms apart
#   sim.sh shot <name>          screenshot to ~/firetv-build/sim-shots/<name>.png
#   sim.sh focus                the launcher view that has focus (id and text)
set -euo pipefail
E=emulator-5580
U=content://com.gathin.tvhome.theme
P=/data/local/tmp/cyt-theme
SHOTS=~/firetv-build/sim-shots
mkdir -p "$SHOTS"

put() {   # put <local file> <theme path>
  adb -s $E push "$1" "$P-x" >/dev/null 2>&1
  adb -s $E shell "content write --uri $U/$2 < $P-x && rm -f $P-x"
}

case "${1:-}" in
  theme)
    T=$2
    # The emulator's own apps are not on the TV: hide them so the grid matches.
    python3 -I - "$T/config.json" "$SHOTS/.theme.json" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1]))
d["hidden_apps"] = sorted(set(d.get("hidden_apps", [])) | {"com.android.vending", "com.google.android.youtube.tv", "com.android.tv.settings"})
json.dump(d, open(sys.argv[2], "w"), indent=2)
EOF
    put "$SHOTS/.theme.json" config.json
    if [ -d "$T/wallpapers" ]; then for f in "$T"/wallpapers/*.jpg; do put "$f" "wallpapers/$(basename "$f")"; done; fi
    date +%s > "$SHOTS/.rev" && put "$SHOTS/.rev" theme_revision
    echo "theme pushed: $T" ;;
  start)
    adb -s $E shell "am force-stop com.gathin.tvhome; am start -n com.gathin.tvhome/.MainActivity" >/dev/null
    sleep 3; echo "launcher started" ;;
  keys)
    # UP DOWN LEFT RIGHT OK are the remote's D-pad; anything else is passed as KEYCODE_<name>.
    shift; for k in "$@"; do
      case "$k" in UP|DOWN|LEFT|RIGHT) k="DPAD_$k" ;; OK) k="DPAD_CENTER" ;; esac
      adb -s $E shell input keyevent "KEYCODE_$k"; sleep 0.7
    done ;;
  shot)
    adb -s $E exec-out screencap -p > "$SHOTS/$2.png"; echo "$SHOTS/$2.png" ;;
  focus)
    adb -s $E shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb -s $E shell cat /sdcard/ui.xml | perl -pe 's/></>\n</g' | grep 'focused="true"' \
      | grep -o -E '(resource-id|text|content-desc|bounds)="[^"]*"' | sed 's/com.gathin.tvhome:id\///' | tr '\n' ' '; echo ;;
  *) sed -n '2,8p' "$0"; exit 1 ;;
esac
