#!/bin/sh
# Downloads Tanzil's Uthmani text and metadata (unmodified) into the app assets.
# Requires curl. Run from anywhere:  sh scripts/download_quran_data.sh
set -e
DIR="$(cd "$(dirname "$0")" && pwd)/../app/src/main/assets/quran"
mkdir -p "$DIR"

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
