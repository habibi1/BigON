#!/usr/bin/env bash
#
# Drive the installed debug build on a booted emulator and capture the screens
# that matter, so a layout change can be looked at rather than reasoned about.
#
# Taps are resolved by on-screen text through a UI Automator dump rather than by
# coordinate. Coordinates would encode one device's pixel grid into a script
# that is supposed to survive a change of emulator profile, and would break
# silently — the tap still lands, just on nothing.
#
# Usage: capture-screenshots.sh [output-dir]

set -euo pipefail

PKG=com.bigon.sinema
OUT="${1:-screenshots}"
DUMP_LOCAL=/tmp/window_dump.xml
DUMP_REMOTE=/sdcard/window_dump.xml

mkdir -p "$OUT"
adb wait-for-device

# Animations off: a screenshot taken mid-transition is a blurred half-frame, and
# the shared-element transition between grid and detail makes that likely.
for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb shell settings put global "$scale" 0 || true
done

# Locate a node by its visible text (or content description) and tap its centre.
tap_text() {
  local label="$1"
  adb shell uiautomator dump "$DUMP_REMOTE" >/dev/null 2>&1 || true
  adb pull "$DUMP_REMOTE" "$DUMP_LOCAL" >/dev/null 2>&1 || true

  local coords
  coords=$(python3 - "$label" "$DUMP_LOCAL" <<'PY' || true
import re, sys, xml.etree.ElementTree as ET
label, path = sys.argv[1], sys.argv[2]
try:
    tree = ET.parse(path)
except Exception:
    sys.exit(0)
for node in tree.iter('node'):
    if node.get('text') == label or node.get('content-desc') == label:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -z "$coords" ]; then
    echo "::warning::'$label' was not on screen — the shot after this one will be wrong"
    return 1
  fi
  # shellcheck disable=SC2086 -- two words, deliberately split into two args
  adb shell input tap $coords
  return 0
}

shot() {
  local name="$1" settle="${2:-2}"
  sleep "$settle"
  adb exec-out screencap -p > "$OUT/${name}.png"
  echo "captured ${name}.png"
}

echo "── launching $PKG ──"
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
# Home does a network round trip on a cold start; give it room before judging it.
shot 01-home 10

echo "── search ──"
tap_text Search || true
shot 02-search 6

echo "── the filter sheet ──"
tap_text Filters || true
shot 03-filters-sheet 3

echo "── search, dark ──"
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell cmd uimode night yes || true
shot 04-search-dark 4
adb shell cmd uimode night no || true

echo "── captured $(ls -1 "$OUT" | wc -l) screenshots ──"
ls -la "$OUT"
