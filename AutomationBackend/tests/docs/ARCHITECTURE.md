# Arquitectura del Framework

## 1. Objetivo

Framework de automatización **solo-API** (sin Selenium/Appium) construido sobre
`pytest` + `requests`, pensado para:

- Validar contratos de respuesta (schema), no solo status codes.
- Evitar duplicación de payloads/rutas entre tests.
- Permitir ejecución paralela y multi-ambiente (dev/qa/prod) sin tocar código.
- Dejar trazabilidad completa de cada petición (cURL + tiempos) en el reporte.

## 2. Capas y responsabilidad única

```
┌─────────────────────────────────────────────────────────────┐
│ tests/                  → QUÉ se prueba (casos de negocio)   │
├─────────────────────────────────────────────────────────────┤
│ services/                → CÓMO se ejecuta la operación      │
│  (Service Object Pattern: AuthService, UsersService)          │
├─────────────────────────────────────────────────────────────┤
│ builders/  │  models/     → CON QUÉ datos (payloads tipados) │
│ utils/data_generator                                          │
├─────────────────────────────────────────────────────────────┤
│ core/http_client          → CÓMO se transporta (HTTP crudo)  │
│ core/session_manager, core/logger, core/exceptions             │
├─────────────────────────────────────────────────────────────┤
│ config/                   → DÓNDE se ejecuta (ambiente/rutas)│
└─────────────────────────────────────────────────────────────┘
```

Cada capa solo conoce la inmediatamente inferior. Un test **nunca** importa
`requests` directamente; un `service` **nunca** decide contra qué ambiente
corre. Esto permite cambiar de proveedor HTTP, de ambiente o de esquema de
datos sin tocar los tests.

### 2.1. Niveles de prueba: 4 carpetas físicas, cero duplicación de lógica

`smoke`, `regression`, `component` y `e2e` son 4 carpetas físicas reales en
`tests/tests/`, pero solo 2 de ellas tienen lógica propia:

- **`component/`** — fuente única de verdad. Un archivo **por endpoint/TC**
  (`test_<KEY_SIN_GUIONES>_<funcion>.py`, ej. `test_SIM_TC_12_create_user.py`),
  con TODOS los escenarios de ese endpoint (camino feliz + alternos + bordes),
  cada uno marcado `@pytest.mark.component`.
- **`smoke/`** — un archivo **por microservicio/ecosistema**
  (`test_smoke_<ecosistema>.py`, ej. `test_smoke_users_manager.py`) que **no
  define tests nuevos**: solo importa (`from tests.component.<modulo> import
  <función>`) los escenarios camino-feliz-crítico de cada endpoint de ese
  microservicio. Si `component/` cambia firma/payload/schema, `smoke/` se
  actualiza solo.
- **`regression/`** — mismo mecanismo de import, un archivo por microservicio
  (`test_reg_<ecosistema>.py`), con el camino feliz + los alternos de negocio
  relevantes (campo obligatorio faltante, recurso inexistente...). Excluye los
  bordes puros sin valor de negocio (límites numéricos exactos, ids usados
  solo para forzar un 404 genérico) — esos quedan solo en `component/`.
- **`e2e/`** — única carpeta además de `component/` con código propio (no
  importa nada): flujos que encadenan más de un endpoint.

**`pytest.ini`: `testpaths = tests/component tests/e2e`** — la suite por
defecto (`pytest` a secas) es exhaustiva y no duplica llamadas reales a la
API. `smoke/` y `regression/` quedan fuera de `testpaths` a propósito: son
reexportaciones de tests que `component/` ya ejecuta, así que se invocan
explícitamente por carpeta (`pytest tests/smoke/`, `pytest tests/regression/`)
cuando se necesita ese subconjunto curado (ej. gate de deploy).

## 3. Flujo de una petición (ejemplo: crear usuario)

```
test_create_user
   │
   ├─> UserBuilder().with_last_name("QA-Automation").build()  # builders/ -> CreateUserRequest (Pydantic)
   │
   └─> users_service.create_user(payload)              # services/
          │
          └─> http_client.post(UsersEndpoints.BASE, ...)  # core/http_client.py
                 │
                 ├─ arma el PreparedRequest real
                 ├─ imprime el cURL exacto (core/http_client._log_as_curl)
                 ├─ envía con reintento si es GET (tenacity)
                 └─ loguea status + latencia (core/logger.py)
```

## 4. Patrones de diseño aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| **Service Object** | `services/auth_service.py`, `services/users_service.py` | Equivalente al Page Object Model pero para APIs: centraliza la lógica de negocio de cada dominio, los tests no conocen rutas ni payloads crudos. |
| **Builder** | `builders/user_builder.py` | Payloads complejos se arman de forma legible y encadenable (`with_first_name().with_last_name()`), con valores por defecto aleatorios (Faker) para no repetir datos fijos entre tests. |
| **Facade / Wrapper** | `core/http_client.py` | Oculta los detalles de `requests.Session`, retry y logging cURL detrás de una interfaz simple (`get/post/put/patch/delete`). |
| **Singleton por sesión de pytest** | `session_manager` (fixture `scope="session"`) | Un solo login por corrida completa de la suite, no por test. |
| **DTO tipado (Pydantic)** | `models/` | Valida tipos del **request** en tiempo de armado del payload (solo donde hay builder). El contrato de la **respuesta** se valida aparte, con JSON Schema (`utils.assertions.assert_json_schema`) — no hay parseo de la respuesta en un modelo Pydantic. |

## 5. Gestión de ambientes

`config/environment.py` lee la variable `ENV` (`dev`/`qa`/`prod`, default `qa`)
y resuelve el `BASE_URL` correspondiente desde `.env`. No hay ramas `if env ==
"prod"` dispersas en el código: todo pasa por `get_environment()`, inyectado
como fixture `environment` en `conftest.py`.

## 6. Autenticación

`core/session_manager.py` hace login una sola vez (vía `AuthService`) y cachea
el token en el `HttpClient` compartido (`requests.Session` con header
`Authorization` seteado). La fixture `authenticated_client` expone ese cliente
ya autenticado a cualquier test que lo necesite.

**Estado actual: `services/auth_service.py` no existe** (framework reseteado
el 2026-07-26; solo el dominio `users` está reconstruido hoy). El mecanismo de
arriba es el diseño soportado por `core/`, pero `authenticated_client` fallará
con `ModuleNotFoundError` hasta que se construya el dominio `auth`
(`models/auth_model.py`, `services/auth_service.py` con un método `login`) por
el mismo pipeline de 3 capas. Si un escenario nuevo requiere autenticación,
ese dominio se construye primero — no asumas que ya está disponible.

## 7. Resiliencia de red

`http_client._request_with_retry` reintenta (backoff exponencial, máx. 3
intentos) únicamente en `GET`, por ser idempotente. `POST/PUT/PATCH/DELETE` no
se reintentan automáticamente: reintentar una operación no-idempotente ante un
timeout puede duplicar efectos secundarios (ej. crear el mismo usuario dos
veces) y enmascarar un bug real de latencia del backend.

## 8. Observabilidad / Reporte

- **Consola** (`core/console_reporter.py`, vía `-s` en `pytest.ini`): imprime en
  vivo cada request, response y aserción con color ANSI.
- **Nombre de escenario** (`@pytest.mark.scenario_name("<KEY-TC> <validación>")`,
  ej. `"SIM-TC-12 Create user age value Mayor 100"`): obligatorio en todo
  escenario de `component/` (uno por caso en un `parametrize`, vía
  `pytest.param(..., marks=pytest.mark.scenario_name(...))`). Un fixture
  autouse en `tests/tests/conftest.py` lo lee (`request.node.get_closest_marker`),
  lo imprime en consola (📌, antes del primer REQUEST) y lo adjunta como
  `user_properties` del test — de ahí lo recoge `pytest_runtest_logreport` en
  `conftest.py` raíz para mostrarlo como título de la fila en el reporte HTML
  (el nodeid técnico queda como subtítulo). Viaja gratis a `smoke/`/`regression/`
  porque el marcador vive en la función original importada, no hay que repetirlo.
- **Archivo** (`core/logger.py`): nivel `WARNING+` en consola (para no
  duplicar el output de `console_reporter`), `DEBUG` completo (incluye cURL)
  en `reports/test_execution.log`.
- **HTML** (`core/html_report.py`, dashboard propio sin dependencias externas):
  `conftest.py` recolecta resultado + detalle de cada test vía
  `pytest_runtest_logreport`, y al terminar la corrida (`pytest_sessionfinish`)
  agrupa los tests por carpeta física (deducida del nodeid) y escribe un
  reporte por carpeta en `tests/<carpeta>/reports/`, con fecha y hora en el
  nombre — cada corrida queda en el historial, no sobrescribe la anterior. Un
  test promovido a `smoke`/`regression` genera reporte en esa carpeta cuando
  se invoca por ahí, y otra vez en `component/reports/` cuando se corre la
  suite exhaustiva por defecto — es la misma llamada real, reportada desde 2
  vistas. Incluye tarjetas de resumen, gráfica de dona filtrable y filas
  expandibles con el mismo detalle de request/response/aserciones que la
  consola.
- El cURL (log de archivo) se imprime a partir del `PreparedRequest` real, no
  reconstruido a mano, así siempre refleja exactamente lo que salió por el socket.

## 9. Extender el framework

Para agregar un nuevo dominio (ej. `products`):

1. `config/endpoints.py` → agregar `ProductsEndpoints`.
2. `models/product_model.py` → DTO de request (solo si hay builder para el camino feliz; sin DTO de response — el contrato de respuesta se valida con JSON Schema en `component/`).
3. `builders/product_builder.py` → si el payload es complejo.
4. `services/products_service.py` → métodos de negocio (`create_product`, ...).
5. `tests/tests/component/test_<KEY>_<funcion>.py` (1 por endpoint) → TODOS los casos de ese endpoint, marcados `@pytest.mark.component`. Los que apliquen se promueven por import a `tests/tests/smoke/test_smoke_products.py` y/o `tests/tests/regression/test_reg_products.py` (ver §2.1) — nunca se reescriben.

No se toca `core/` ni `config/environment.py` para agregar un dominio nuevo:
esa es la señal de que la separación de capas está funcionando.
