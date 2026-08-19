#!/usr/bin/env python3
import json
import urllib.request
from pathlib import Path

CATALOG = Path('catalog.json')
ALLOWED = {'public', 'team', 'system'}

catalog = json.loads(CATALOG.read_text(encoding='utf-8'))
for app in catalog.get('apps', []):
    audience = 'public'
    source = app.get('source_manifest')
    if isinstance(source, str) and source.startswith('https://raw.githubusercontent.com/'):
        try:
            req = urllib.request.Request(source, headers={'User-Agent': 'StoreAMO-Catalog/1'})
            with urllib.request.urlopen(req, timeout=10) as response:
                manifest = json.loads(response.read().decode('utf-8'))
            value = manifest.get('audience', 'public')
            if value in ALLOWED:
                audience = value
        except Exception:
            pass
    app['audience'] = audience

CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('AUDIENCE_OK', sum(1 for a in catalog.get('apps', []) if a.get('audience') == 'team'))
