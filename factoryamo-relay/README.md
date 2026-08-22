# FactoryAMO Relay v1

Canal público de estado entre chats de ChatGPT y FactoryAMO Android.

## Dirección

FactoryAMO Android → portapapeles/Intent → ChatGPT.

ChatGPT → GitHub → `factoryamo-relay/llegada/<JOB_ID>/status.json` → FactoryAMO Android.

El teléfono no contiene tokens de OpenAI ni de GitHub. ChatGPT escribe usando la conexión GitHub autorizada del usuario. FactoryAMO sólo lee por HTTPS el estado público.

## Seguridad

`llegada/` es pública. Nunca debe contener tokens, claves, credenciales, código fuente, datos personales, prompts completos ni secretos. Sólo estado operativo no sensible.

## Estados

`NEW`, `DESIGNING`, `BUILDING`, `TESTING`, `COMPILING`, `VERIFYING`, `PUBLISHING`, `WAITING_USER`, `BLOCKED`, `FAILED`, `PUBLISHED`.

`PUBLISHED` sólo puede emitirse cuando la app está realmente visible en StoreAMO.

## Esquema mínimo

```json
{
  "schema": "factoryamo.relay.v1",
  "job_id": "FAMO-YYYYMMDD-HHMMSS-ABCDE",
  "app_name": "ClimaAMO",
  "stage": "DESIGNING",
  "progress": 12,
  "message": "Definiendo MVP y arquitectura.",
  "question": "",
  "updated_at": "2026-08-22T16:00:00+02:00",
  "storeamo_url": ""
}
```

FactoryAMO valida `schema` y `job_id` antes de aceptar un estado.
