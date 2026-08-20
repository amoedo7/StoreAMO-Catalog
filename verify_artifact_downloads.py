#!/usr/bin/env python3
"""Verify that every installable Android artifact in catalog.json is really downloadable.

This intentionally performs anonymous HTTPS GETs, because that is what StoreAMO users need.
It catches the class of failure where metadata looks valid but the public asset is missing,
private, truncated, or different from the declared SHA-256/size.
"""
from __future__ import annotations

import hashlib
import io
import json
import sys
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

CATALOG = Path(sys.argv[1] if len(sys.argv) > 1 else "catalog.json")
TIMEOUT = 45
USER_AGENT = "StoreAMO-Catalog-CI/1.0"


def fail(message: str) -> None:
    print(f"::error::{message}")


def download(url: str) -> bytes:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": USER_AGENT, "Accept": "application/octet-stream"},
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status}")
        final_url = response.geturl()
        if not final_url.lower().startswith("https://"):
            raise RuntimeError(f"redirected to non-HTTPS URL: {final_url}")
        return response.read()


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    checked = 0
    errors = 0

    for app in catalog.get("apps", []):
        app_id = app.get("id", "<unknown>")
        for artifact in app.get("artifacts", []):
            if artifact.get("platform") != "android" or artifact.get("format") != "apk":
                continue
            checked += 1
            url = artifact.get("url", "")
            expected_sha = str(artifact.get("sha256", "")).lower()
            expected_size = artifact.get("size_bytes")
            label = f"{app_id} {artifact.get('version', '?')}"

            try:
                if not url.startswith("https://"):
                    raise RuntimeError("artifact URL is not HTTPS")
                body = download(url)
                actual_sha = hashlib.sha256(body).hexdigest()
                if actual_sha != expected_sha:
                    raise RuntimeError(f"SHA-256 mismatch: expected {expected_sha}, got {actual_sha}")
                if expected_size is not None and len(body) != int(expected_size):
                    raise RuntimeError(f"size mismatch: expected {expected_size}, got {len(body)}")
                with zipfile.ZipFile(io.BytesIO(body)) as apk:
                    bad = apk.testzip()
                    if bad is not None:
                        raise RuntimeError(f"corrupt APK ZIP member: {bad}")
                print(f"OK {label}: {len(body)} bytes · {actual_sha}")
            except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, OSError, ValueError, zipfile.BadZipFile, RuntimeError) as exc:
                errors += 1
                fail(f"{label}: public download verification failed: {exc}")

    if checked == 0:
        fail("catalog contains no Android APK artifacts to verify")
        return 1
    print(f"Checked {checked} public Android artifact(s); failures: {errors}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
