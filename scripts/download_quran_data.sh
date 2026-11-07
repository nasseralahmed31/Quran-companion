#!/bin/sh
# Downloads Tanzil's Uthmani text and metadata (unmodified) and the Amiri Quran font into the app assets.
# Requires curl. Run from anywhere:  sh scripts/download_quran_data.sh
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)/.."
DIR="$ROOT/app/src/main/assets/quran"
FONT_DIR="$ROOT/app/src/main/assets/fonts"
mkdir -p "$DIR" "$FONT_DIR"

curl -fL "https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&agree=true" \
  -o "$DIR/quran-uthmani.txt"
curl -fL "https://tanzil.net/res/text/metadata/quran-data.xml" \
  -o "$DIR/quran-data.xml"

VERSES=$(grep -c '^[0-9][0-9]*|[0-9][0-9]*|' "$DIR/quran-uthmani.txt" || true)
echo "Verse lines found: $VERSES (expected 6236)"
if [ "$VERSES" != "6236" ]; then
  echo "ERROR: unexpected verse count. Download the file manually from https://tanzil.net/download/" >&2
  echo "(Quran type: Uthmani, Output: Text with verse numbers) and save it as $DIR/quran-uthmani.txt" >&2
  exit 1
fi
echo "OK. The app also re-validates these files before importing them."

# Optional: Quran font (Amiri Quran, SIL OFL). The app falls back to the system Arabic font without it.
FONT_OK=""
for URL in \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/amiriquran/AmiriQuran-Regular.ttf" \
  "https://mirrors.ctan.org/fonts/amiri/AmiriQuran.ttf"; do
  if curl -fL "$URL" -o "$FONT_DIR/uthmani.ttf"; then
    SIZE=$(wc -c < "$FONT_DIR/uthmani.ttf")
    if [ "$SIZE" -gt 50000 ]; then
      echo "Font OK ($SIZE bytes)"
      FONT_OK=1
      break
    fi
  fi
done
if [ -z "$FONT_OK" ]; then
  rm -f "$FONT_DIR/uthmani.ttf"
  echo "WARNING: font not downloaded; the app will use the system Arabic font." >&2
fi
