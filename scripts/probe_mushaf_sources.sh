#!/bin/sh
# Prints what is reachable for the Madinah-mushaf page fonts and line data, so the next step can use
# exact file names instead of guesses. Run it, then paste the whole output back.
probe() {
  printf '== %s\n' "$1"
  curl -sIL --max-time 25 "$1" | grep -iE '^(HTTP|content-length|content-type|location)' | head -8
  echo
}
probe "https://raw.githubusercontent.com/nuqayah/qpc-fonts/master/mushaf.txt"
probe "https://raw.githubusercontent.com/nuqayah/qpc-fonts/master/mushaf-v2.txt"
probe "http://qurancomplex.gov.sa/TTF/"
probe "https://verses.quran.foundation/fonts/quran/hafs/v1/ttf/p1.ttf"
probe "https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p1.ttf"
echo "== file names in the qpc-fonts repository (GitHub API)"
for d in mushaf mushaf-v2 mushaf-woff2; do
  echo "-- $d"
  curl -s --max-time 25 "https://api.github.com/repos/nuqayah/qpc-fonts/contents/$d" | grep '"name"' | head -8
done
