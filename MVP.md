# MVP · StoreAMO Catalog

## Resultado que debe entregar
Un repo con `storeamo.json` válido puede aparecer automáticamente en un catálogo machine-readable sin editar StoreAMO.

## Flujo mínimo
1. Descubrir repos permitidos.
2. Leer `storeamo.json`.
3. Validar schema e identidad.
4. Resolver release/asset cuando aplique.
5. Obtener digest verificable si existe.
6. Generar `catalog.json`.
7. Generar reporte de descubrimiento.
8. Publicar sólo entradas válidas.

## Criterios obligatorios
- repo inválido no rompe todo el catálogo;
- discovery ≠ verified;
- duplicados de id se rechazan;
- URLs/artefactos inconsistentes se reportan;
- salida determinista y testeada;
- candidate/verified conservan frontera.

## Fuera del MVP
- decidir seguridad por popularidad;
- descargar/ejecutar apps para “probarlas”.
