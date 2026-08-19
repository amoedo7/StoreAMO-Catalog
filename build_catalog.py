#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

OWNER_DEFAULT = "amoedo7"
APP_SCHEMA = "storeamo.app.v1"
CATALOG_SCHEMA = "storeamo.catalog.v1"
PLATFORMS = {"android", "windows", "macos", "linux", "web", "ios", "other"}
STATUSES = {"development", "candidate", "verified", "deprecated"}


def request_json(url: str, token: str | None = None) -> Any:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "StoreAMO-Catalog/1",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def request_text(url: str, token: str | None = None) -> str:
    headers = {"User-Agent": "StoreAMO-Catalog/1"}
    if token and url.startswith("https://api.github.com/"):
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=12) as response:
        return response.read().decode("utf-8")


def list_public_repos(owner: str, token: str | None) -> list[dict[str, Any]]:
    repos: list[dict[str, Any]] = []
    page = 1
    while True:
        url = f"https://api.github.com/users/{urllib.parse.quote(owner)}/repos?per_page=100&page={page}&type=owner&sort=full_name"
        batch = request_json(url, token)
        if not isinstance(batch, list) or not batch:
            break
        repos.extend(r for r in batch if not r.get("private") and not r.get("archived"))
        if len(batch) < 100:
            break
        page += 1
    return repos


def load_manifest(owner: str, repo: dict[str, Any], token: str | None) -> dict[str, Any] | None:
    branch = repo.get("default_branch") or "main"
    raw = f"https://raw.githubusercontent.com/{owner}/{repo['name']}/{urllib.parse.quote(branch, safe='')}/storeamo.json"
    try:
        data = json.loads(request_text(raw, token))
    except (urllib.error.HTTPError, urllib.error.URLError, json.JSONDecodeError, TimeoutError):
        return None
    return data if isinstance(data, dict) else None


def validate_manifest(m: dict[str, Any], expected_repo: str) -> list[str]:
    errors: list[str] = []
    if m.get("schema") != APP_SCHEMA:
        errors.append(f"schema debe ser {APP_SCHEMA}")
    for key in ("id", "name", "tagline", "description", "category"):
        if not isinstance(m.get(key), str) or not m[key].strip():
            errors.append(f"{key} requerido")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", str(m.get("id", ""))):
        errors.append("id inválido")
    if m.get("status") not in STATUSES:
        errors.append("status inválido")
    platforms = m.get("supported_platforms")
    if not isinstance(platforms, list) or not platforms or any(p not in PLATFORMS for p in platforms):
        errors.append("supported_platforms inválido")
    release = m.get("release")
    if release is not None:
        if not isinstance(release, dict) or release.get("provider") != "github":
            errors.append("release.provider debe ser github")
        elif release.get("repo") and release.get("repo") != expected_repo:
            errors.append("release.repo debe apuntar al mismo repositorio")
        for rule in (release.get("assets") or []) if isinstance(release, dict) else []:
            if not isinstance(rule, dict) or rule.get("platform") not in PLATFORMS or not rule.get("pattern"):
                errors.append("regla de asset inválida")
                continue
            try:
                re.compile(rule["pattern"])
            except re.error:
                errors.append(f"regex de asset inválida: {rule.get('pattern')}")
    return errors


def latest_release(full_repo: str, token: str | None, channel: str) -> dict[str, Any] | None:
    if channel == "stable":
        url = f"https://api.github.com/repos/{full_repo}/releases/latest"
        try:
            value = request_json(url, token)
            return value if isinstance(value, dict) else None
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                return None
            raise
    url = f"https://api.github.com/repos/{full_repo}/releases?per_page=20"
    try:
        releases = request_json(url, token)
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    if not isinstance(releases, list):
        return None
    return next((r for r in releases if not r.get("draft")), None)


def asset_digest(asset: dict[str, Any]) -> str | None:
    digest = asset.get("digest")
    if isinstance(digest, str) and digest.lower().startswith("sha256:"):
        value = digest.split(":", 1)[1].lower()
        if re.fullmatch(r"[0-9a-f]{64}", value):
            return value
    return None


def build_artifacts(m: dict[str, Any], token: str | None, warnings: list[str]) -> list[dict[str, Any]]:
    release_cfg = m.get("release")
    if not isinstance(release_cfg, dict) or release_cfg.get("provider") != "github":
        return []
    full_repo = release_cfg.get("repo")
    rules = release_cfg.get("assets") or []
    if not full_repo or not rules:
        return []
    release = latest_release(full_repo, token, release_cfg.get("channel", "stable"))
    if not release:
        return []
    tag = str(release.get("tag_name") or "").lstrip("v")
    version = str(release_cfg.get("version") or tag)
    assets = release.get("assets") or []
    result: list[dict[str, Any]] = []
    for rule in rules:
        pattern = re.compile(rule["pattern"])
        match = next((a for a in assets if pattern.search(str(a.get("name", "")))), None)
        if not match:
            continue
        sha = asset_digest(match)
        if not sha:
            warnings.append(f"{m['id']}: asset {match.get('name')} sin digest SHA-256 de GitHub; no se publica en catálogo")
            continue
        result.append({
            "platform": rule["platform"],
            "arch": rule.get("arch"),
            "format": rule.get("format"),
            "version": version,
            "version_code": str(release_cfg.get("version_code") or version),
            "url": match.get("browser_download_url"),
            "sha256": sha,
            "size_bytes": match.get("size"),
            "min_os": rule.get("min_os"),
            "application_id": rule.get("application_id"),
            "verified": False,
            "verification_report": None,
            "release_url": release.get("html_url"),
            "source": "github-release",
        })
    return result


def catalog_entry(m: dict[str, Any], repo: dict[str, Any], token: str | None, warnings: list[str]) -> dict[str, Any]:
    store = m.get("store") if isinstance(m.get("store"), dict) else {}
    full_repo = repo["full_name"]
    return {
        "id": m["id"],
        "name": m["name"],
        "tagline": m["tagline"],
        "description": m["description"],
        "category": m["category"],
        "featured": bool(m.get("featured", False)),
        "status": m.get("status", "development"),
        "supported_platforms": list(dict.fromkeys(m.get("supported_platforms") or [])),
        "repository": repo.get("html_url") or f"https://github.com/{full_repo}",
        "store": store,
        "verification": m.get("verification") or {"policy": "storeamo-default-v1"},
        "artifacts": build_artifacts(m, token, warnings),
        "source_manifest": f"https://raw.githubusercontent.com/{full_repo}/{repo.get('default_branch') or 'main'}/storeamo.json",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Descubre storeamo.json y construye catalog.json")
    parser.add_argument("--owner", default=OWNER_DEFAULT)
    parser.add_argument("--output", type=Path, default=Path("catalog.json"))
    parser.add_argument("--report", type=Path, default=Path("discovery-report.json"))
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN")
    warnings: list[str] = []
    errors: list[dict[str, Any]] = []
    discovered: list[dict[str, Any]] = []

    for repo in list_public_repos(args.owner, token):
        manifest = load_manifest(args.owner, repo, token)
        if manifest is None:
            continue
        manifest_errors = validate_manifest(manifest, repo["full_name"])
        if manifest_errors:
            errors.append({"repository": repo["full_name"], "errors": manifest_errors})
            continue
        discovered.append(catalog_entry(manifest, repo, token, warnings))

    ids = [a["id"] for a in discovered]
    if len(ids) != len(set(ids)):
        duplicates = sorted({x for x in ids if ids.count(x) > 1})
        errors.append({"repository": None, "errors": [f"IDs duplicados: {', '.join(duplicates)}"]})

    discovered.sort(key=lambda a: (not a["featured"], a["name"].lower()))
    catalog = {
        "schema": CATALOG_SCHEMA,
        "catalog_version": 2,
        "generated_at": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat(),
        "generation": {"mode": "repository-manifests", "owner": args.owner, "app_schema": APP_SCHEMA},
        "brand": {
            "name": "DesarrollAMO",
            "store_name": "StoreAMO",
            "accent": "#67D2FF",
            "accent_secondary": "#F16AB5",
            "background": "#06101C"
        },
        "apps": discovered,
    }
    report = {
        "schema": "storeamo.discovery.report.v1",
        "generated_at": catalog["generated_at"],
        "owner": args.owner,
        "apps_discovered": len(discovered),
        "repositories": [a["repository"] for a in discovered],
        "warnings": warnings,
        "errors": errors,
    }
    args.output.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"StoreAMO: {len(discovered)} apps descubiertas · {len(warnings)} warnings · {len(errors)} repos inválidos")
    for warning in warnings:
        print("WARN", warning)
    for error in errors:
        print("FAIL", error)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
