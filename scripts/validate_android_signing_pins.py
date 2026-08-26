#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PINS_PATH = ROOT / "android-signing-pins.json"
REGISTRY = ROOT / "registry"
CATALOG = ROOT / "catalog.json"
HEX64 = re.compile(r"^[0-9a-f]{64}$")

pins_doc = json.loads(PINS_PATH.read_text(encoding="utf-8"))
assert pins_doc.get("schema") == "storeamo.android-signing-pins.v1"
pins = pins_doc.get("packages")
assert isinstance(pins, dict) and pins

used_app_ids: set[str] = set()

for package_name, pin in pins.items():
    assert isinstance(package_name, str) and package_name.count(".") >= 2
    assert isinstance(pin, dict)
    canonical = str(pin.get("canonical_cert_sha256") or "").lower()
    assert HEX64.fullmatch(canonical), f"{package_name}: invalid canonical cert"
    legacy = pin.get("legacy_certificates") or []
    legacy_certs = set()
    for row in legacy:
        cert = str((row or {}).get("cert_sha256") or "").lower()
        assert HEX64.fullmatch(cert), f"{package_name}: invalid legacy cert"
        assert cert != canonical, f"{package_name}: canonical cert listed as legacy"
        legacy_certs.add(cert)

    app_id = str(pin.get("app_id") or "").strip()
    assert app_id, f"{package_name}: missing app_id"
    assert app_id not in used_app_ids, f"{package_name}: duplicate app_id in signing pins: {app_id}"
    used_app_ids.add(app_id)

    registry_path = REGISTRY / f"{app_id}.json"
    assert registry_path.is_file(), f"{package_name}: registry entry missing: {registry_path.name}"
    manifest = json.loads(registry_path.read_text(encoding="utf-8"))
    artifacts = [
        a for a in manifest.get("artifacts", [])
        if isinstance(a, dict) and a.get("platform") == "android" and a.get("application_id") == package_name
    ]
    assert artifacts, f"{package_name}: no Android artifact in registry"
    for artifact in artifacts:
        cert = str(artifact.get("signing_cert_sha256") or "").lower()
        assert cert == canonical, (
            f"{package_name}: SIGNER_DRIFT in registry; expected {canonical}, got {cert or '<missing>'}"
        )
        assert cert not in legacy_certs

    if CATALOG.is_file():
        catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
        app = next((a for a in catalog.get("apps", []) if a.get("id") == app_id), None)
        assert app is not None, f"{package_name}: app missing from catalog"
        catalog_artifacts = [
            a for a in app.get("artifacts", [])
            if isinstance(a, dict) and a.get("platform") == "android" and a.get("application_id") == package_name
        ]
        assert catalog_artifacts, f"{package_name}: no Android artifact in catalog"
        for artifact in catalog_artifacts:
            cert = str(artifact.get("signing_cert_sha256") or "").lower()
            assert cert == canonical, (
                f"{package_name}: SIGNER_DRIFT in catalog; expected {canonical}, got {cert or '<missing>'}"
            )

print(f"ANDROID_SIGNING_PINS_OK packages={len(pins)} app_ids={len(used_app_ids)}")
