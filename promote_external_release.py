#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import urllib.request
from pathlib import Path
from typing import Any

HEX64 = re.compile(r"^[0-9a-f]{64}$")


def get_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "StoreAMO-Promoter/1"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.read()


def get_text(url: str) -> str:
    return get_bytes(url).decode("utf-8")


def parse_sha256sums(text: str, filename: str) -> str:
    for line in text.splitlines():
        m = re.match(r"^([0-9a-fA-F]{64})\s+[* ]?(.+?)\s*$", line)
        if m and Path(m.group(2)).name == filename:
            return m.group(1).lower()
    raise SystemExit(f"No SHA-256 for {filename} in SHA256SUMS.txt")


def require_hex64(value: str, label: str) -> str:
    value = value.strip().lower()
    if not HEX64.fullmatch(value):
        raise SystemExit(f"Invalid {label}: {value!r}")
    return value


def load_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise SystemExit(f"{path}: expected a JSON object")
    return value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("request", type=Path)
    args = parser.parse_args()

    request = load_json(args.request)
    app_id = str(request["app_id"])
    version = str(request["version"])
    version_code = str(request["version_code"])
    application_id = str(request["application_id"])
    expected_signer = require_hex64(str(request["signing_cert_sha256"]), "expected signer")
    registry_path = Path(str(request.get("registry", f"registry/{app_id}.json")))
    base = str(request["release_base_url"]).rstrip("/") + f"/{app_id}/{version}"
    apk_name = str(request.get("apk_name", f"{request.get('name', app_id)}-{version}.apk"))
    apk_url = f"{base}/{apk_name}"
    release_url = f"{base}/release.json"

    signer = require_hex64(get_text(f"{base}/SIGNING_CERT_SHA256.txt"), "published signer")
    if signer != expected_signer:
        raise SystemExit(f"SIGNER_DRIFT: expected {expected_signer}, got {signer}")

    sums_sha = parse_sha256sums(get_text(f"{base}/SHA256SUMS.txt"), apk_name)
    apk = get_bytes(apk_url)
    actual_sha = hashlib.sha256(apk).hexdigest()
    if actual_sha != sums_sha:
        raise SystemExit(f"SHA256_MISMATCH: sums={sums_sha} actual={actual_sha}")

    release: dict[str, Any] = {}
    try:
        parsed = json.loads(get_text(release_url))
        if isinstance(parsed, dict):
            release = parsed
    except Exception as exc:
        raise SystemExit(f"release.json unavailable or invalid: {exc}") from exc

    # Validate whatever identity fields the secure publisher exposes, without
    # depending on one exact release.json layout.
    for key in ("version", "versionName", "version_name"):
        if key in release and str(release[key]) != version:
            raise SystemExit(f"release.json {key} mismatch: {release[key]!r}")
    for key in ("versionCode", "version_code"):
        if key in release and str(release[key]) != version_code:
            raise SystemExit(f"release.json {key} mismatch: {release[key]!r}")
    for key in ("applicationId", "application_id", "packageName", "package_name"):
        if key in release and str(release[key]) != application_id:
            raise SystemExit(f"release.json {key} mismatch: {release[key]!r}")

    manifest = load_json(registry_path)
    if str(manifest.get("id")) != app_id:
        raise SystemExit(f"Registry id mismatch: {manifest.get('id')!r}")

    artifact = {
        "platform": "android",
        "arch": "universal",
        "format": "apk",
        "version": version,
        "version_code": version_code,
        "url": apk_url,
        "sha256": actual_sha,
        "signing_cert_sha256": signer,
        "size_bytes": len(apk),
        "min_os": str(request.get("min_os", "Android 8.0 (API 26)")),
        "application_id": application_id,
        "verified": False,
        "verification_report": None,
        "release_url": release_url,
        "source": "distribution-registry",
    }
    manifest["artifacts"] = [artifact]

    for key in ("tagline", "description", "category", "audience", "status"):
        if key in request:
            manifest[key] = request[key]
    if "supported_platforms" in request:
        manifest["supported_platforms"] = request["supported_platforms"]
    if "store_notes" in request:
        store = manifest.setdefault("store", {})
        if not isinstance(store, dict):
            raise SystemExit("registry store must be an object")
        store["notes"] = request["store_notes"]

    registry_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "app_id": app_id,
        "version": version,
        "version_code": version_code,
        "sha256": actual_sha,
        "signing_cert_sha256": signer,
        "size_bytes": len(apk),
        "registry": str(registry_path),
    }, indent=2))


if __name__ == "__main__":
    main()
