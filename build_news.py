#!/usr/bin/env python3
"""Build the public StoreAMO "Buenas Nuevas" feed.

Public application repositories contribute recent commit activity. Private-source
applications never expose private commit text: their public registry entry is used
as the sanitized activity boundary instead.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

API = "https://api.github.com"
SCHEMA = "storeamo.news.v1"
MAX_ITEMS = 120
MAX_PER_APP = 4
LOOKBACK_DAYS = 45


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def request_json(url: str, token: str | None) -> object:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "StoreAMO-Catalog-News/1",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


def github_repo_name(repository: str | None, owner: str) -> str | None:
    if not repository:
        return None
    prefix = f"https://github.com/{owner}/"
    if not repository.startswith(prefix):
        return None
    name = repository[len(prefix):].strip("/")
    if not name or "/" in name:
        return None
    return name


def clean_subject(message: str) -> tuple[str, str]:
    first = (message or "").splitlines()[0].strip()
    first = re.sub(r"\s+", " ", first)
    first = re.sub(r"^\[[^]]+\]\s*", "", first)
    match = re.match(r"^(feat|fix|perf|test|docs|ci|build|refactor|chore|release)(?:\([^)]*\))?!?:\s*(.+)$", first, re.I)
    kind = "activity"
    text = first
    if match:
        prefix, text = match.group(1).lower(), match.group(2).strip()
        kind = {
            "feat": "feature",
            "fix": "fix",
            "perf": "improvement",
            "test": "tests",
            "docs": "docs",
            "ci": "build",
            "build": "build",
            "refactor": "improvement",
            "chore": "activity",
            "release": "release",
        }[prefix]
    lowered = text.lower()
    if "release" in lowered or re.search(r"\bv?\d+\.\d+\.\d+", lowered):
        kind = "release"
    return kind, text[:180]


def friendly_title(app_name: str, kind: str) -> str:
    return {
        "feature": f"Nueva mejora en {app_name}",
        "fix": f"Corrección en {app_name}",
        "improvement": f"{app_name} sigue mejorando",
        "tests": f"Pruebas en {app_name}",
        "docs": f"Novedades de {app_name}",
        "build": f"{app_name} prepara cambios",
        "release": f"Nueva versión de {app_name}",
    }.get(kind, f"Actividad reciente en {app_name}")


def public_items(app: dict, owner: str, token: str | None, cutoff: datetime) -> list[dict]:
    repo = github_repo_name(app.get("repository"), owner)
    if not repo:
        return []
    url = f"{API}/repos/{owner}/{urllib.parse.quote(repo)}/commits?per_page={MAX_PER_APP}"
    try:
        commits = request_json(url, token)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, ValueError):
        return []
    if not isinstance(commits, list):
        return []

    items: list[dict] = []
    for commit in commits:
        meta = (commit or {}).get("commit") or {}
        author = meta.get("author") or meta.get("committer") or {}
        published = parse_time(author.get("date"))
        if not published or published < cutoff:
            continue
        message = str(meta.get("message") or "")
        first = message.splitlines()[0].strip()
        if not first or first.startswith("Merge "):
            continue
        kind, summary = clean_subject(message)
        if not summary:
            continue
        sha = str((commit or {}).get("sha") or "")[:12]
        items.append({
            "id": f"{app['id']}-{sha}",
            "app_id": app["id"],
            "app_name": app.get("name") or app["id"],
            "type": kind,
            "title": friendly_title(app.get("name") or app["id"], kind),
            "summary": summary,
            "published_at": published.isoformat(),
            "status": app.get("status", "development"),
            "source_visibility": "public",
        })
    return items


def registry_path(app: dict) -> str | None:
    source = str(app.get("source_manifest") or "")
    marker = "/StoreAMO-Catalog/main/"
    if marker not in source:
        return None
    path = source.split(marker, 1)[1]
    return path if path.startswith("registry/") and path.endswith(".json") else None


def private_item(app: dict, owner: str, token: str | None, cutoff: datetime) -> dict | None:
    path = registry_path(app)
    if not path:
        return None
    query = urllib.parse.urlencode({"path": path, "per_page": 1})
    url = f"{API}/repos/{owner}/StoreAMO-Catalog/commits?{query}"
    try:
        commits = request_json(url, token)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, ValueError):
        return None
    if not isinstance(commits, list) or not commits:
        return None
    commit = commits[0]
    meta = (commit or {}).get("commit") or {}
    author = meta.get("author") or meta.get("committer") or {}
    published = parse_time(author.get("date"))
    if not published or published < cutoff:
        return None

    status = app.get("status", "development")
    name = app.get("name") or app["id"]
    if status == "development":
        title = f"{name} está en desarrollo"
        summary = "Hubo actividad reciente. StoreAMO mostrará sus avances públicos sin revelar el código privado."
        kind = "development"
    elif status == "candidate":
        title = f"{name} avanza como candidate"
        summary = "Se actualizó su información pública de distribución o verificación."
        kind = "candidate"
    else:
        title = f"Novedades en {name}"
        summary = "Se actualizó la información pública de esta aplicación."
        kind = "activity"
    sha = str((commit or {}).get("sha") or "")[:12]
    return {
        "id": f"{app['id']}-registry-{sha}",
        "app_id": app["id"],
        "app_name": name,
        "type": kind,
        "title": title,
        "summary": summary,
        "published_at": published.isoformat(),
        "status": status,
        "source_visibility": "private",
    }


def build(owner: str, catalog_path: Path, out_path: Path) -> dict:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    token = os.environ.get("STOREAMO_NEWS_TOKEN") or os.environ.get("GITHUB_TOKEN")
    cutoff = now_utc() - timedelta(days=LOOKBACK_DAYS)
    items: list[dict] = []
    for app in catalog.get("apps", []):
        if app.get("source_visibility", "public") == "public":
            items.extend(public_items(app, owner, token, cutoff))
        else:
            item = private_item(app, owner, token, cutoff)
            if item:
                items.append(item)

    # Keep the feed deterministic for equal timestamps and avoid duplicates.
    unique = {item["id"]: item for item in items}
    ordered = sorted(unique.values(), key=lambda item: (item["published_at"], item["id"]), reverse=True)[:MAX_ITEMS]
    payload = {
        "schema": SCHEMA,
        "generated_at": now_utc().isoformat(),
        "privacy": {
            "public_repositories": "recent public commit summaries",
            "private_repositories": "sanitized registry activity only",
        },
        "items": ordered,
    }
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--owner", default="amoedo7")
    parser.add_argument("--catalog", default="catalog.json")
    parser.add_argument("--out", default="news.json")
    args = parser.parse_args()
    payload = build(args.owner, Path(args.catalog), Path(args.out))
    print("NEWS_OK", len(payload["items"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
