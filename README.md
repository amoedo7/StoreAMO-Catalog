<div align="center">

# StoreAMO Catalog

**Catálogo oficial, portable y verificable del ecosistema DesarrollAMO.**

`storeamo.catalog.v1` · `GitHub Releases` · `Android` · `Windows` · `macOS` · `Linux` · `Web`

</div>

---

StoreAMO no decide qué descargar leyendo nombres de archivos al azar. Consume este catálogo versionado y muestra primero el artefacto adecuado para el dispositivo detectado.

```text
usuario abre StoreAMO
        ↓
detectar plataforma
        ↓
cargar catalog.json
        ↓
priorizar artefacto compatible
        ↓
mostrar OBTENER / ACTUALIZAR / ABRIR
        ↓
VER MÁS → otras plataformas disponibles
```

## Principios

- un único registro por aplicación;
- múltiples artefactos por plataforma cuando corresponda;
- ningún secreto en el catálogo;
- descargas oficiales por HTTPS;
- una versión no se marca `verified` sin evidencia de StoreAMO-Verify;
- las apps en desarrollo pueden aparecer sin inventar descargas;
- Android, Windows, macOS y Linux comparten el mismo contrato;
- StoreAMO puede ocultar artefactos incompatibles sin eliminarlos del catálogo.

## Estado inicial

El catálogo actual registra la suite pública de DesarrollAMO como `development` mientras preparamos releases verificadas. No se publican APK viejos como versiones oficiales.

## Archivos

- [`catalog.json`](catalog.json) — catálogo consumible por StoreAMO y StoreAMO-Web.
- [`schemas/storeamo.catalog.v1.schema.json`](schemas/storeamo.catalog.v1.schema.json) — contrato estructural.

## Estados

```text
development → candidate → verified → deprecated
```

`verified` significa que StoreAMO-Verify dispone de evidencia suficiente para ese artefacto. No significa “software sin bugs”.

---

**DesarrollAMO** · software, automatización y sistemas.