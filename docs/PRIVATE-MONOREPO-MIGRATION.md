# Migración desde monorepo privado sin romper StoreAMO

RaizAMO puede alojar progresivamente código oficial en `apps/<id>/` sin obligar a StoreAMO a leer el repositorio privado.

## Frontera de seguridad

```text
RaizAMO (privado)
  apps/<id>/ código + tests
          │
          │ build/verificación
          ▼
artefacto público controlado
          │
          ├─ URL HTTPS
          ├─ SHA-256
          ├─ versión/version_code
          └─ firma cuando corresponda
          │
          ▼
StoreAMO-Catalog/registry/<id>.json
          │
          ▼
      catalog.json
          │
          ▼
 StoreAMO Android/Web
```

El catálogo **no necesita acceso al código privado**. `build_catalog.py` ya soporta manifests saneados en `registry/`, `source.visibility=private` y artefactos directos con URL HTTPS + SHA-256.

## Regla de compatibilidad

Durante la migración conviven:

1. apps legacy descubiertas desde `repo/storeamo.json`;
2. apps de fuente privada declaradas mediante `registry/*.json`.

No se cambia el contrato consumido por StoreAMO Android/Web y no existe un día de migración global.

## Promoción por aplicación

Una app sólo cambia de fuente cuando se ha comprobado:

```text
importación al monorepo
→ build reproducible
→ tests
→ artefacto
→ SHA-256/firma
→ registry candidate
→ catalog.json válido
→ descarga pública real
→ instalación/verificación
→ cambio de fuente
→ archive del repo legacy
```

Nunca borrar el repo antiguo automáticamente.

## Publicación

El repositorio privado no debe exponer tokens para que StoreAMO pueda leerlo. Si en el futuro existe un GitHub App/broker con permisos mínimos para promoción cross-repo, debe producir únicamente el manifest saneado y la evidencia necesaria; no debe publicar secretos ni abrir acceso general al monorepo.

## Invariante

> Migrar el lugar donde vive el código no modifica por sí solo la identidad, la firma, la URL funcional ni el estado `verified` de una app.
