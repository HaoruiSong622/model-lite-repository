#!/bin/bash
set -e

SOURCE_PATH="${SOURCE_PATH}"
TARGET_PATH="${TARGET_PATH}"
ALLOWED_SUFFIXES="${ALLOWED_SUFFIXES}"

if [ "${SOURCE_TYPE}" = "CIFS" ]; then
  mkdir -p /source
  mount -t cifs "${SOURCE_PATH}" /source -o username="${CIFS_USERNAME}",password="${CIFS_PASSWORD}" || {
    echo "ERROR: CIFS mount failed for ${SOURCE_PATH}"; exit 1; }
  SOURCE_PATH="/source"
fi

[ -d "${SOURCE_PATH}" ] || { echo "ERROR: source ${SOURCE_PATH} is not a directory"; exit 1; }

IFS=',' read -ra SUFFIXES <<< "${ALLOWED_SUFFIXES}"
for f in "${SOURCE_PATH}"/*; do
  [ -e "$f" ] || continue
  ok=false
  for s in "${SUFFIXES[@]}"; do case "$f" in *"$s") ok=true; break;; esac; done
  [ "$ok" = true ] || { echo "ERROR: $(basename "$f") suffix not in whitelist [${ALLOWED_SUFFIXES}]"; exit 1; }
done
echo "validated"

mkdir -p "${TARGET_PATH}"
total=$(find "${SOURCE_PATH}" -type f | wc -l)
[ "${total}" -gt 0 ] || { echo "ERROR: no files under ${SOURCE_PATH}"; exit 1; }
copied=0
for f in "${SOURCE_PATH}"/*; do
  [ -e "$f" ] || continue
  rsync -a "$f" "${TARGET_PATH}/"
  copied=$((copied + 1))
  echo "PROGRESS:$((copied * 100 / total))"
done
echo "PROGRESS:100"
echo "copy completed"
