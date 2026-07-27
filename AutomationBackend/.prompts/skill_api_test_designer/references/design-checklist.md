# Plantilla de Matriz de Escenarios + Checklist de Cobertura

## Plantilla

Cada escenario de endpoint vive en su propio archivo físico de `component/`
(`tests/tests/component/test_<KEY_SIN_GUIONES>_<funcion>.py`, uno por
endpoint/TC). La columna "Promoción" es la que decide si, además, se
**importa** (nunca se copia) en `tests/tests/smoke/test_smoke_<dominio>.py`
y/o `tests/tests/regression/test_reg_<dominio>.py`. `e2e` es la excepción:
carpeta física propia con código propio, no reexporta nada.

| ID | Nombre de escenario | Promoción | Servicio.método | Insumos (builder/datos) | Auth | Status esperado | Aserciones (`utils/assertions.py`) | Capas nuevas a crear | Notas |
|---|---|---|---|---|---|---|---|---|---|
| PROD-01 | `PROD-TC-1 Create product happy path` | `component` + `smoke` | `products_service.create_product` | `ProductBuilder().build()` | pública | 201 | `assert_status_code`, `assert_response_time(max_ms=2000)` | endpoints, model, builder, service, fixture | camino feliz |
| ... | | | | | | | | | |

La columna **"Nombre de escenario"** es obligatoria: formato `"<KEY-TC-con-guiones> <validación principal>"` (ej. `"SIM-TC-12 Create user age value Mayor 100"`). El developer lo traduce 1:1 a `@pytest.mark.scenario_name("...")` — se ve en consola y en el reporte HTML de cada corrida.

## Checklist de cobertura mínima antes de aprobar el diseño

- [ ] Todo escenario de endpoint tiene su archivo en `component/` (1 archivo por endpoint/TC, nunca varios endpoints en el mismo archivo).
- [ ] Cada endpoint nuevo, o bien tiene su camino feliz promovido a `smoke`, o bien la fila trae una nota explícita de por qué NO es crítico para el gate de deploy (ej. "valida contrato aislado, sin valor de negocio adicional, ya cubierto en `e2e`") — nunca queda fuera de `smoke` en silencio, sin justificación.
- [ ] Los alternos de negocio importantes (campo obligatorio faltante, recurso inexistente, dato inválido con significado de negocio) están promovidos a `regression`.
- [ ] Los bordes puros (límites numéricos exactos, ids usados solo para forzar un 404 genérico, valores sin significado de negocio) están marcados explícitamente como **"solo component, no promover"** — nunca se promueven por default.
- [ ] Cada escenario de endpoint (promovido o no) trae el JSON Schema esperado escrito explícitamente (no "TBD").
- [ ] 1 escenario `e2e` (carpeta separada, código propio) si el requerimiento encadena más de un endpoint (crear → consultar → actualizar → eliminar, o similar).
- [ ] Cada fila referencia una función real de `utils/assertions.py`, o indica explícitamente "nueva aserción a crear en `utils/assertions.py`" con su firma propuesta.
- [ ] Cada fila indica si usa `http_client` (público) o `authenticated_client` (requiere login).
- [ ] Si el dominio es nuevo, la fila indica las variables `.env` (`{SERVICE}_{TIER}_BASE_URL`) y la entrada a agregar en `config/environment.py:_DEFAULT_BASE_URLS`.
- [ ] Ningún escenario duplica un test ya existente en `tests/tests/component/` (verificado contra `tests/docs/MODULES.md`).
- [ ] Cada fila trae su "Nombre de escenario" (`<KEY-TC> <validación principal>`, ej. `"SIM-TC-12 Create user age value Mayor 100"`) — es lo que el developer traduce a `@pytest.mark.scenario_name(...)`.

## Ejemplo completo — dominio nuevo `products`

Extiende el ejemplo de `docs/ARCHITECTURE.md` §9 ("Extender el framework"). Host nuevo: `PRODUCTS_QA_BASE_URL` (ej. `https://dummyjson.com/products`, público, sin auth).

| ID | Promoción | Servicio.método | Insumos | Auth | Status esperado | Aserciones | Capas nuevas | Notas |
|---|---|---|---|---|---|---|---|---|
| PROD-01 | `component` + `smoke` | `products_service.create_product` | `ProductBuilder().build()` | pública | 201 | `assert_status_code(201)`, `assert_json_schema(PRODUCT_SCHEMA)`, `assert_response_time(max_ms=2000)` | `ProductsEndpoints`, `ProductRequest`/`ProductResponse`, `ProductBuilder`, `ProductsService`, fixture `products_service` | camino feliz de creación |
| PROD-02 | `component` + `regression` | `products_service.get_product` | `product_id=999999` | pública | 404 | `assert_status_code(404)` | — (reutiliza capas de PROD-01) | recurso inexistente — alterno de negocio, sí se promueve |
| PROD-03 | `component` solamente | `products_service.create_product` | `ProductBuilder().with_price(-1).build()` | pública | 400 | `assert_status_code(400)`, `assert_body_contains("message", ...)` | — | precio negativo — **borde puro, no promover** a smoke/regression |
| PROD-04 | `component` solamente | `products_service.get_product` | `product_id=1` | pública | 200 | `assert_status_code(200)`, `assert_json_schema(PRODUCT_SCHEMA)` | — | valida contrato/schema aislado, sin valor de negocio adicional — no se promueve |
| PROD-05 | `e2e` | `products_service.create_product` → `get_product` → `update_product` → `delete_product` | `ProductBuilder().build()` | pública | 201/200/200/200 | `assert_status_code` en cada paso | — | flujo completo encadenado — única carpeta con código propio |

Archivos resultantes:

```
tests/tests/component/test_PROD_TC_1_create_product.py       # PROD-01 + PROD-03 (mismo endpoint)
tests/tests/component/test_PROD_TC_2_get_product.py          # PROD-02 + PROD-04 (mismo endpoint)
tests/tests/smoke/test_smoke_products.py                      # import de PROD-01 (nombre por dominio, sin sufijo — no hay archivo previo que reutilizar)
tests/tests/regression/test_reg_products.py                   # import de PROD-01 + PROD-02
tests/tests/e2e/test_product_lifecycle_e2e.py                 # PROD-05, código propio
```

Entregar esta matriz al usuario para validación antes de pasarla a `skill_api_automation_developer`.
