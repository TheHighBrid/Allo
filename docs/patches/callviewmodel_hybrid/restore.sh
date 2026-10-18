#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/../../../app/src/main/java/com/kenza/callsim/call/CallViewModel.kt"
cat "$ROOT"/part*.b64 | base64 -d > "$OUT"
echo "Restored CallViewModel.kt ($(wc -c < "$OUT") bytes)"
