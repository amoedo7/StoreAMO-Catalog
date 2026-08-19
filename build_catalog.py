#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
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
AUDIENCES = {"public", "team", "system"}
SOURCE_VISIBILITIES = {"public", "private"}
REGISTRY_DIR = Path("registry")
CATALOG_REPO = "amoedo7/StoreAMO-Catalog"

FORBIDDEN_PUBLIC_KEY_PARTS = {
    "secret",
    "token",
    "password",
    "passwd",
    "privatekey",
    "private_key",
    "keystore",
    "seed",
    "mnemonic",
    "apikey",
    "api_key",
    "credential",
    "credentials",
}


def request_json(url: str, token: str | None = None) -> Any:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "StoreAMO-Catalog/2",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def request_text(url: str, token: str | None = None) -> str:
    headers = {"User-Agent": "StoreAMO-Catalog/2"}
    if token and url.startswith("https://api.github.com/"):
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=12) as response:
        return response.read().decode("utf-8")


def list_public_repos(owner: str, token: str | None) -> list[dict[str, Any]]:
    repos: list[dict[str, Any]] = []
    page = 1
    while True:
        url = (
            f"https://api.github.com/users/{urllib.parse.quote(owner)}/repos"
            f"?per_page=100&page={page}&type=owner&sort=full_name"
        )
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
    raw = (
        f"https://raw.githubusercontent.com/{owner}/{repo['name']}/"
        f"{urllib.parse.quote(branch, safe='')}/storeamo.json"
    )
    try:
        data = json.loads(request_text(raw, token))
    except (urllib.error.HTTPError, urllib.error.URLError, json.JSONDecodeError, TimeoutError):
        return None
    return data if isinstance(data, dict) else None


def load_registry_manifests() -> list[tuple[Path, dict[str, Any]]]:
    if not REGISTRY_DIR.exists():
        return []
    result: list[tuple[Path, dict[str, Any]]] = []
    for path in sorted(REGISTRY_DIR.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ValueError(f"{path}: JSON inválido: {exc}") from exc
        if not isinstance(data, dict):
            raise ValueError(f"{path}: el manifest debe ser un objeto JSON")
        result.append((path, data))
    return result


def normalize_key(key: str) -> str:
    return re.sub(r"[^a-z0-9_]", "", key.lower())


def find_forbidden_public_fields(value: Any, path: str = "$") -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            normalized = normalize_key(str(key))
            if normalized in FORBIDDEN_PUBLIC_KEY_PARTS or any(
                normalized.endswith(part) for part in FORBIDDEN_PUBLIC_KEY_PARTS
            ):
                found.append(f"{path}.{key}")
            found.extend(find_forbidden_public_fields(child, f"{path}.{key}"))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            found.extend(find_forbidden_public_fields(child, f"{path}[{index}]"))
    return found


def validate_sha256(value: Any) -> bool:
    return isinstance(value, str) and re.fullmatch(r"[0-9a-fA-F]{64}", value) is not None


def validate_manifest(m: dict[str, Any], expected_repo: str | None = None) -> list[str]:
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
    if m.get("audience", "public") not in AUDIENCES:
        errors.append("audience inválido")
    platforms = m.get("supported_platforms")
    if not isinstance(platforms, list) or not platforms or any(p not in PLATFORMS for p in platforms):
        errors.append("supported_platforms inválido")

    source = m.get("source")
    if source is not None:
        if not isinstance(source, dict):
            errors.append("source debe ser objeto")
        elif source.get("visibility", "public") not in SOURCE_VISIBILITIES:
            errors.append("source.visibility inválido")

    release = m.get("release")
    if release is not None:
        if not isinstance(release, dict) or release.get("provider") != "github":
            errors.append("release.provider debe ser github")
        elif release.get("repo") and not re.fullmatch(
            r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", str(release.get("repo"))
        ):
            errors.append("release.repo inválido")
        if isinstance(release, dict) and release.get("tag_pattern"):
            try:
                re.compile(str(release["tag_pattern"]))
            except re.error:
                errors.append("release.tag_pattern inválido")
        for rule in (release.get("assets") or []) if isinstance(release, dict) else []:
            if not isinstance(rule, dict) or rule.get("platform") not in PLATFORMS or not rule.get("pattern"):
                errors.append("regla de asset inválida")
                continue
            try:
                re.compile(rule["pattern"])
            except re.error:
                errors.append(f"regex de asset inválida: {rule.get('pattern')}")
            cert = rule.get("signing_cert_sha256")
            if cert is not None and not validate_sha256(cert):
                errors.append("signing_cert_sha256 inválido")

    direct_artifacts = m.get("artifacts")
    if direct_artifacts is not None:
        if not isinstance(direct_artifacts, list):
            errors.append("artifacts debe ser una lista")
        else:
            for artifact in direct_artifacts:
                if not isinstance(artifact, dict):
                    errors.append("artifact inválido")
                    continue
                if artifact.get("platform") not in PLATFORMS:
                    errors.append("artifact.platform inválido")
                if not isinstance(artifact.get("url"), str) or not artifact["url"].startswith("https://"):
                    errors.append("artifact.url debe ser HTTPS")
                if not validate_sha256(artifact.get("sha256")):
                    errors.append("artifact.sha256 inválido")
                if not str(artifact.get("version") or "").strip():
                    errors.append("artifact.version requerido")
                if not str(artifact.get("version_code") or "").strip():
                    errors.append("artifact.version_code requerido")
                cert = artifact.get("signing_cert_sha256")
                if cert is not None and not validate_sha256(cert):
                    errors.append("artifact.signing_cert_sha256 inválido")

    forbidden = find_forbidden_public_fields(m)
    if forbidden:
        errors.append("campos sensibles prohibidos en manifest público: " + ", ".join(sorted(forbidden)))

    if expected_repo and isinstance(release, dict):
        release_repo = release.get("repo")
        source_visibility = (source or {}).get("visibility", "public") if isinstance(source, dict) else "public"
        distribution_declared = bool(m.get("distribution"))
        if release_repo and release_repo != expected_repo and source_visibility == "public" and not distribution_declared:
            errors.append("release.repo externo requiere distribution o source.visibility=private")
    return errors


def latest_release(
    full_repo: str,
    token: str | None,
    channel: str,
    tag_pattern: str | None = None,
) -> dict[str, Any] | None:
    if channel == "stable" and not tag_pattern:
        url = f"https://api.github.com/repos/{full_repo}/releases/latest"
        try:
            value = request_json(url, token)
            return value if isinstance(value, dict) else None
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                return None
            raise

    url = f"https://api.github.com/repos/{full_repo}/releases?per_page=100"
    try:
        releases = request_json(url, token)
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    if not isinstance(releases, list):
        return None
    matcher = re.compile(tag_pattern) if tag_pattern else None
    for release in releases:
        if release.get("draft"):
            continue
        if channel == "stable" and release.get("prerelease"):
            continue
        tag = str(release.get("tag_name") or "")
        if matcher and not matcher.search(tag):
            continue
        return release
    return None


def asset_digest(asset: dict[str, Any]) -> str | None:
    digest = asset.get("digest")
    if isinstance(digest, str) and digest.lower().startswith("sha256:"):
        value = digest.split(":", 1)[1].lower()
        if re.fullmatch(r"[0-9a-f]{64}", value):
            return value
    return None


def checksum_from_release_assets(release_assets: list[dict[str, Any]], target_name: str) -> str | None:
    sums_asset = next(
        (a for a in release_assets if str(a.get("name", "")).lower() in {"sha256sums.txt", "sha256sum.txt"}),
        None,
    )
    if not sums_asset:
        return None
    url = sums_asset.get("browser_download_url")
    if not isinstance(url, str) or not url.startswith("https://"):
        return None
    try:
        text = request_text(url)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, UnicodeDecodeError):
        return None
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        match = re.match(r"^([0-9a-fA-F]{64})\s+[* ]?(.+)$", line)
        if match and Path(match.group(2).strip()).name == target_name:
            return match.group(1).lower()
    return None


def normalize_direct_artifacts(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for item in items:
        result.append({
            "platform": item["platform"],
            "arch": item.get("arch"),
            "format": item.get("format"),
            "version": str(item["version"]),
            "version_code": str(item["version_code"]),
            "url": item["url"],
            "sha256": str(item["sha256"]).lower(),
            "size_bytes": item.get("size_bytes"),
            "min_os": item.get("min_os"),
            "application_id": item.get("application_id"),
            "signing_cert_sha256": str(item["signing_cert_sha256"]).lower() if item.get("signing_cert_sha256") else None,
            "verified": bool(item.get("verified", False)),
            "verification_report": item.get("verification_report"),
            "release_url": item.get("release_url"),
            "source": item.get("source", "distribution-registry"),
        })
    return result


def build_artifacts(m: dict[str, Any], token: str | None, warnings: list[str]) -> list[dict[str, Any]]:
    direct = m.get("artifacts")
    if isinstance(direct, list):
        return normalize_direct_artifacts(direct)

    release_cfg = m.get("release")
    if not isinstance(release_cfg, dict) or release_cfg.get("provider") != "github":
        return []
    full_repo = release_cfg.get("repo")
    rules = release_cfg.get("assets") or []
    if not full_repo or not rules:
        return []
    release = latest_release(full_repo, token, release_cfg.get("channel", "stable"), release_cfg.get("tag_pattern"))
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
        asset_name = str(match.get("name") or "")
        sha = asset_digest(match) or checksum_from_release_assets(assets, asset_name)
        if not sha:
            warnings.append(f"{m['id']}: asset {asset_name} sin SHA-256 verificable; no se publica en catálogo")
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
            "signing_cert_sha256": str(rule["signing_cert_sha256"]).lower() if rule.get("signing_cert_sha256") else None,
            "verified": False,
            "verification_report": None,
            "release_url": release.get("html_url"),
            "source": "github-release",
        })
    return result


def base_catalog_entry(m: dict[str, Any], token: str | None, warnings: list[str]) -> dict[str, Any]:
    store = m.get("store") if isinstance(m.get("store"), dict) else {}
    source = m.get("source") if isinstance(m.get("source"), dict) else {}
    return {
        "id": m["id"],
        "name": m["name"],
        "tagline": m["tagline"],
        "description": m["description"],
        "category": m["category"],
        "audience": m.get("audience", "public"),
        "featured": bool(m.get("featured", False)),
        "status": m.get("status", "development"),
        "supported_platforms": list(dict.fromkeys(m.get("supported_platforms") or [])),
        "source_visibility": source.get("visibility", "public"),
        "store": store,
        "verification": m.get("verification") or {"policy": "storeamo-default-v1"},
        "artifacts": build_artifacts(m, token, warnings),
    }


def catalog_entry_public(m: dict[str, Any], repo: dict[str, Any], token: str | None, warnings: list[str]) -> dict[str, Any]:
    full_repo = repo["full_name"]
    entry = base_catalog_entry(m, token, warnings)
    entry["repository"] = repo.get("html_url") or f"https://github.com/{full_repo}"
    entry["source_manifest"] = f"https://raw.githubusercontent.com/{full_repo}/{repo.get('default_branch') or 'main'}/storeamo.json"
    return entry


def catalog_entry_registry(m: dict[str, Any], path: Path, token: str | None, warnings: list[str]) -> dict[str, Any]:
    entry = base_catalog_entry(m, token, warnings)
    source = m.get("source") if isinstance(m.get("source"), dict) else {}
    visibility = source.get("visibility", "private")
    entry["source_visibility"] = visibility
    if visibility == "public":
        entry["repository"] = source.get("repository") or (m.get("store") or {}).get("homepage")
    else:
        entry["repository"] = (m.get("store") or {}).get("homepage")
    entry["source_manifest"] = f"https://raw.githubusercontent.com/{CATALOG_REPO}/main/{path.as_posix()}"
    return entry


def main() -> int:
    parser = argparse.ArgumentParser(description="Descubre manifests públicos y registry sanitizado para construir catalog.json")
    parser.add_argument("--owner", default=OWNER_DEFAULT)
    parser.add_argument("--output", type=Path, default=Path("catalog.json"))
    parser.add_argument("--report", type=Path, default=Path("discovery-report.json"))
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN")
    warnings: list[str] = []
    errors: list[dict[str, Any]] = []
    discovered_by_id: dict[str, dict[str, Any]] = {}

    for repo in list_public_repos(args.owner, token):
        manifest = load_manifest(args.owner, repo, token)
        if manifest is None:
            continue
        manifest_errors = validate_manifest(manifest, repo["full_name"])
        if manifest_errors:
            errors.append({"source": repo["full_name"], "errors": manifest_errors})
            continue
        discovered_by_id[manifest["id"]] = catalog_entry_public(manifest, repo, token, warnings)

    try:
        registry_manifests = load_registry_manifests()
    except ValueError as exc:
        errors.append({"source": "registry", "errors": [str(exc)]})
        registry_manifests = []

    for path, manifest in registry_manifests:
        manifest_errors = validate_manifest(manifest, None)
        if manifest_errors:
            errors.append({"source": path.as_posix(), "errors": manifest_errors})
            continue
        discovered_by_id[manifest["id"]] = catalog_entry_registry(manifest, path, token, warnings)

    discovered = sorted(discovered_by_id.values(), key=lambda a: (not a["featured"], a["name"].lower()))

    catalog = {
        "schema": CATALOG_SCHEMA,
        "catalog_version": 3,
        "generated_at": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat(),
        "generation": {
            "mode": "public-repositories+sanitized-registry",
            "owner": args.owner,
            "app_schema": APP_SCHEMA,
            "private_sources_supported": True,
        },
        "brand": {
            "name": "DesarrollAMO",
            "store_name": "StoreAMO",
            "accent": "#67D2FF",
            "accent_secondary": "#F16AB5",
            "background": "#06101C",
        },
        "apps": discovered,
    }
    report = {
        "schema": "storeamo.discovery.report.v2",
        "generated_at": catalog["generated_at"],
        "owner": args.owner,
        "apps_discovered": len(discovered),
        "public_repository_entries": sum(1 for a in discovered if a.get("source_visibility") == "public"),
        "private_registry_entries": sum(1 for a in discovered if a.get("source_visibility") == "private"),
        "warnings": warnings,
        "errors": errors,
    }
    args.output.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"StoreAMO: {len(discovered)} apps · {report['private_registry_entries']} privadas vía registry · "
        f"{len(warnings)} warnings · {len(errors)} fuentes inválidas"
    )
    for warning in warnings:
        print("WARN", warning)
    for error in errors:
        print("FAIL", error)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
