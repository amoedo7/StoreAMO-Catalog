#!/usr/bin/env bash
set -euo pipefail

VERIFY_SHA="bc52d320373a8bcb0991c9465cce515dcc33308a"
TMP_DIR="${TMPDIR:-/tmp}/storeamo-verify-${VERIFY_SHA}"

python scripts/validate_android_signing_pins.py
rm -rf "$TMP_DIR"
git clone --quiet --no-checkout https://github.com/amoedo7/StoreAMO-Verify.git "$TMP_DIR"
git -C "$TMP_DIR" checkout --quiet --detach "$VERIFY_SHA"
test "$(git -C "$TMP_DIR" rev-parse HEAD)" = "$VERIFY_SHA"
python "$TMP_DIR/verify_catalog.py" catalog.json
rm -rf "$TMP_DIR"

echo "StoreAMO-Catalog AutoCheck PASS"
