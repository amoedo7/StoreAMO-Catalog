# StoreAMO Registry

Este directorio es la frontera pública entre StoreAMO y aplicaciones cuyo código fuente puede ser privado.

Cada archivo `registry/<app-id>.json` contiene únicamente metadatos sanitizados necesarios para mostrar y distribuir una app. Nunca debe contener código fuente privado, rutas internas, tokens, contraseñas, claves de firma, keystores, seeds, credenciales ni secretos de infraestructura.

## Modelo

```text
repo fuente privado
      ↓ CI privada
build + tests + firma
      ↓
artefacto público de distribución
      ↓
registry/<app-id>.json sanitizado
      ↓
StoreAMO-Catalog
      ↓
StoreAMO
```

El catálogo acepta dos fuentes compatibles:

1. `storeamo.json` en repositorios públicos existentes, para mantener compatibilidad.
2. manifests sanitizados en `registry/`, para apps con fuente privada.

Cuando un ID existe en ambos lugares, `registry/` tiene prioridad. Esto permite migrar una app de pública a privada sin hacerla desaparecer de StoreAMO durante la transición.

## Identidad Android

Para Android, el manifest público debe incluir como mínimo `application_id`, `version`, `version_code`, `sha256` y, una vez fijada la firma segura, `signing_cert_sha256`.

El certificado público/fingerprint puede publicarse. La clave privada que lo genera, jamás.
