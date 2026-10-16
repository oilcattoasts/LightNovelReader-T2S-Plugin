#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/converter/build/standalone-test"
mkdir -p "$OUT"
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/converter/src/main/java/org/lnr/t2s/converter/TraditionalToSimplified.java" \
  "$ROOT/converter/src/verification/java/org/lnr/t2s/converter/TraditionalToSimplifiedTest.java"
java -cp "$OUT" org.lnr.t2s.converter.TraditionalToSimplifiedTest \
  "$ROOT/plugin/src/main/assets/opencc"
