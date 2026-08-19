<div align="center">

# StoreAMO Catalog

**El catálogo se construye desde las propias aplicaciones, no desde StoreAMO.**

`storeamo.app.v1` · `GitHub Releases` · `Android` · `Windows` · `macOS` · `Linux` · `Web`

</div>

---

Cada aplicación que quiera vivir en StoreAMO declara en la raíz de su repositorio:

```text
storeamo.json
```

`StoreAMO-Catalog` descubre automáticamente los repositorios públicos de `amoedo7` que tengan ese manifiesto, valida su identidad y reconstruye `catalog.json`. StoreAMO Android y StoreAMO Web sólo consumen el catálogo: **no necesitan una actualización de código cuando aparece una app nueva**.

```text
REPO DE LA APP
   │
   ├── código
   ├── tests
   ├── GitHub Releases
   └── storeamo.json
            ↓
StoreAMO-Catalog · discovery automático
            ↓
      catalog.json
            ↓
     StoreAMO / Web
```

## Alta de una aplicación

Para entrar al ecosistema, un repo debe tener un `storeamo.json` válido con:

- identidad y textos de la ficha;
- plataformas soportadas;
- estado (`development`, `candidate`, `verified`, `deprecated`);
- configuración opcional de GitHub Release y patrones de artefactos;
- política de verificación requerida.

El schema vive en [`schemas/storeamo.app.v1.schema.json`](schemas/storeamo.app.v1.schema.json).

## Actualizaciones automáticas

Si el manifiesto declara una Release GitHub y patrones de assets, el generador consulta la última Release estable. Cuando GitHub proporciona un digest SHA-256 para el asset, puede incluir el artefacto en el catálogo sin editar StoreAMO.

Una nueva versión puede entonces seguir este camino:

```text
git tag / release nueva
        ↓
asset nuevo
        ↓
discovery horario
        ↓
catalog.json actualizado
        ↓
StoreAMO detecta la versión
```

## Verificación

Descubrir un repo **no equivale a declararlo seguro**. `StoreAMO-Verify` mantiene la frontera entre presencia en catálogo y `StoreAMO Verified`.

```text
development → candidate → verified → deprecated
```

Una release sin evidencia suficiente puede aparecer como proyecto, pero no recibe el sello `Verified`. StoreAMO puede ocultar descargas no verificadas por defecto.

## Automatización

[`build_catalog.py`](build_catalog.py) recorre repos públicos, lee `storeamo.json`, valida identidad y genera:

- `catalog.json`
- `discovery-report.json`

El workflow `discover-apps.yml` lo ejecuta cada hora y también manualmente.

---

**DesarrollAMO** · la app declara quién es; StoreAMO verifica y distribuye.
