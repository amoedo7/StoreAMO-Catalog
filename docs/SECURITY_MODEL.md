# StoreAMO · Security model v2

StoreAMO separa **fuente** de **distribución**. El código de una aplicación no necesita ser público para que la aplicación pueda aparecer, descargarse y actualizarse desde StoreAMO.

## Fronteras de confianza

```text
FUENTE PRIVADA
  código, tests, arquitectura, CI
        ↓
GITHUB ACTIONS PRIVADA
  usa secretos sólo durante el job
        ↓
BUILD FIRMADO
        ↓
DISTRIBUCIÓN PÚBLICA
  APK/artefacto + SHA-256 + certificado público + manifest sanitizado
        ↓
STOREAMO-CATALOG
        ↓
STOREAMO
```

## Regla de visibilidad

- Repositorio fuente: **privado por defecto**.
- IdeAMO/prototipos/ideas: **privado**.
- Catálogo y metadatos necesarios para instalar: pueden ser públicos.
- APKs destinados a usuarios públicos: pueden ser públicos aunque el código fuente sea privado.
- Documentación pública: sólo información deliberadamente publicable.

La oscuridad del código no reemplaza controles de seguridad, pero evita exponer gratuitamente propiedad intelectual, prototipos y detalles internos.

## Secretos

Nunca se versionan:

- keystores o claves privadas;
- contraseñas de firma;
- tokens de GitHub;
- API keys;
- service-role keys;
- seeds/mnemonics;
- credenciales de bases de datos;
- secretos de sesión;
- archivos `.env` reales.

Los workflows consumen secretos desde GitHub Actions Secrets o un gestor equivalente y deben usar permisos mínimos. Un secreto que estuvo en un repositorio público se considera comprometido aunque luego se borre del último commit.

## Firma Android

Cada aplicación tiene una identidad de firma estable. La clave privada no vive en Git, StoreAMO ni el APK de StoreAMO. El catálogo puede publicar el fingerprint SHA-256 del certificado porque es información pública usada para verificar identidad.

StoreAMO debe verificar antes de actualizar:

1. package/application ID;
2. versionCode superior;
3. SHA-256 del artefacto;
4. certificado de firma esperado;
5. compatibilidad del dispositivo.

Cambiar una clave de firma es una operación de seguridad/migración, no un cambio normal de versión.

## Publicación desde fuente privada

Una app privada publica sólo:

- nombre y copy de tienda;
- plataformas;
- versión/versionCode;
- application ID;
- URL pública del artefacto;
- SHA-256;
- fingerprint público del certificado;
- estado candidate/verified;
- información de compatibilidad y privacidad que corresponda.

El manifest público no debe revelar el nombre o URL del repositorio privado si no hace falta.

## Credenciales cross-repository

Si un workflow privado necesita publicar en un repositorio de distribución, usar preferentemente una GitHub App o un token fine-grained limitado **únicamente** al repositorio de distribución y con los permisos mínimos requeridos. El token se almacena como Secret; nunca en código, Gradle, JSON, README o logs.

## Incidente

Ante una credencial o clave expuesta:

1. asumir compromiso;
2. detener nuevas publicaciones afectadas;
3. revocar/rotar la credencial;
4. retirar el secreto del HEAD e historial cuando corresponda;
5. revisar logs y alcance;
6. migrar identidades si la clave expuesta era una clave de firma;
7. reanudar distribución sólo después de verificar la nueva cadena de confianza.
