#!/bin/sh
# ROJAN Ecosystem Governance — drift check (ENFORCING, PASS G7).
#
# Exit status:
#   0  governance/ is present, pinned to a GOVERNANCE-v* version, and every
#      vendored rule document matches CHECKSUMS.sha256 (byte-identical).
#   1  any of: governance/ missing, ADOPTED_VERSION missing / not a GOVERNANCE-v*
#      pin, CHECKSUMS.sha256 missing, or a vendored document differs from the
#      adopted canonical set.
#
# Usage:  sh check-governance-drift.sh [governance-dir]   (default: ./governance)
#
# CHECKSUMS.sha256 covers only the canonical rule documents. The three adoption
# artifacts (ADOPTED_VERSION, CHECKSUMS.sha256, this script) are excluded by
# design, so upgrading this script or re-pinning a version is not "drift".

set -u

GOV_DIR="${1:-governance}"
FAIL=0

echo "=== ROJAN Ecosystem Governance — drift check (enforcing) ==="

if [ ! -d "$GOV_DIR" ]; then
  echo "::error::governance/ folder not found at '$GOV_DIR'"
  echo "RESULT: FAIL — this repository has no vendored governance/ folder."
  exit 1
fi

if [ -f "$GOV_DIR/ADOPTED_VERSION" ]; then
  ADOPTED="$(head -n1 "$GOV_DIR/ADOPTED_VERSION")"
  echo "Adopted version: $ADOPTED"
  case "$ADOPTED" in
    GOVERNANCE-v[0-9]*) : ;;
    *)
      echo "::error::governance/ADOPTED_VERSION first line is not a GOVERNANCE-v* pin: '$ADOPTED'"
      FAIL=1
      ;;
  esac
else
  echo "::error::$GOV_DIR/ADOPTED_VERSION is missing"
  FAIL=1
fi

if [ ! -f "$GOV_DIR/CHECKSUMS.sha256" ]; then
  echo "::error::$GOV_DIR/CHECKSUMS.sha256 is missing — cannot verify document integrity"
  echo "RESULT: FAIL — incomplete adoption."
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  CHECK_OUT="$(cd "$GOV_DIR" && sha256sum -c CHECKSUMS.sha256 2>&1)"
else
  CHECK_OUT="$(cd "$GOV_DIR" && while read -r sum name; do
      name="${name#\*}"
      actual="$(shasum -a 256 "$name" 2>/dev/null | awk '{print $1}')"
      if [ "$actual" = "$sum" ]; then echo "$name: OK"; else echo "$name: FAILED"; fi
    done < CHECKSUMS.sha256)"
fi

echo "$CHECK_OUT" | sed 's/^/  /'

if echo "$CHECK_OUT" | grep -q 'FAILED'; then
  echo "::error::one or more vendored governance documents differ from the adopted canonical set"
  FAIL=1
fi

if [ "$FAIL" -eq 0 ]; then
  echo "RESULT: OK — governance/ matches its adopted canonical version."
else
  echo "RESULT: FAIL — governance drift detected (see errors above)."
fi

exit "$FAIL"
