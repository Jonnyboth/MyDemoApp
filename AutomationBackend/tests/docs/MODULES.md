# Resumen por Módulo

Referencia rápida de qué hace cada archivo y cuándo tocarlo.

## `config/`

| Archivo | Resumen |
|---|---|
| `environment.py` | Lee `ENV` (dev/qa/prod) y variables de `.env`; devuelve un `EnvironmentConfig` inmutable con `base_url`, credenciales y `timeout`. Único punto donde se decide "contra qué servidor corremos". Tocar solo si se agrega un nuevo ambiente o variable de configuración global. |
| `endpoints.py` | Constantes de rutas relativas agrupadas por dominio (`AuthEndpoints`, `UsersEndpoints`). Si el backend cambia una ruta, se corrige en un solo lugar. Tocar al agregar un endpoint nuevo. |

## `core/`

| Archivo | Resumen |
|---|---|
| `http_client.py` | Envoltorio sobre `requests.Session`. Expone `get/post/put/patch/delete` tipados, delega en `console_reporter` la impresión de cada request/response (consola + buffer HTML) y reintenta automáticamente los `GET` ante errores de conexión/timeout (backoff exponencial, 3 intentos). El cURL equivalente sigue logueándose en el archivo (`DEBUG`). |
| `session_manager.py` | Hace login una única vez por corrida y cachea el token en el `HttpClient` compartido. Fixture `authenticated_client` en `conftest.py` depende de esto. |
| `logger.py` | Configura logging dual: consola (`WARNING+`, para no interferir con `console_reporter`) + archivo `reports/test_execution.log` (`DEBUG`, incluye el cURL completo). Evita duplicar handlers si pytest reimporta el módulo. |
| `exceptions.py` | Excepciones propias: `ApiRequestError` (fallos de red), `ApiAssertionError` (fallos de aserción de negocio), `SchemaValidationError` (contrato de respuesta incumplido). |
| `console_reporter.py` | Fuente única de datos del reporte: imprime en consola (color ANSI) el request/response/aserciones de cada test y, en paralelo, acumula esos mismos eventos como datos estructurados (`reset_buffer`/`pop_buffer_items`) que `conftest.py` recolecta al final de cada test. También imprime el arte de bienvenida por archivo (`print_module_banner`). |
| `html_report.py` | Generador del reporte HTML propio (dashboard morado oscuro con tarjetas, dona filtrable y filas expandibles). `generate_report(path, title, generated_at, data)` escribe un único archivo HTML autocontenido (CSS + JS vanilla inline, sin dependencias externas ni build) a partir del diccionario `{"environment": ..., "tests": [...]}` armado en `conftest.py`. |

## `models/`

| Archivo | Resumen |
|---|---|
| `user_model.py` | DTO Pydantic `CreateUserRequest` (`firstName`/`lastName`/`age`) del dominio de usuarios — solo el request de `create_user` (`SIM-TC-12`), que es el que arma `UserBuilder`. `update_user`/`get_user`/`delete_user` (`SIM-TC-13/14/15`) no tienen modelo: `update_user` recibe un `dict` porque cada test manda solo el/los campo(s) a modificar (no un usuario completo), y `get_user`/`delete_user` no llevan body. |

Regla: un modelo aquí solo se justifica cuando hay un builder armando un
payload "completo" tipado para el camino feliz. El contrato de la
**respuesta** nunca se modela con Pydantic — se valida con un dict de JSON
Schema en el propio archivo de `component/` (`assert_json_schema`). Un
`dict`/escalar directo en la firma del `service` es correcto (no un atajo)
cuando el body es parcial o no existe.

## `services/`

| Archivo | Resumen |
|---|---|
| `users_service.py` | Operaciones sobre usuarios (`create_user`, `get_user`, `update_user`, `delete_user`) sobre `HttpClient`. Los tests llaman estos métodos, nunca `http_client.post(...)` directo. |

Regla: un `service` por dominio de negocio. Si un test necesita orquestar dos
servicios (ej. crear usuario y luego loguearse), esa orquestación vive en el
test, no dentro de un service.

## `builders/`

| Archivo | Resumen |
|---|---|
| `user_builder.py` | Builder encadenable para `CreateUserRequest`. Genera datos aleatorios por defecto (Faker) y permite sobreescribir solo el campo relevante al caso de prueba (`UserBuilder().with_last_name("QA-Automation").build()`). |

## `utils/`

| Archivo | Resumen |
|---|---|
| `assertions.py` | Aserciones reutilizables: `assert_status_code`, `assert_response_time`, `assert_json_schema` (valida contra JSON Schema con `jsonschema`), `assert_body_contains`, `assert_header_present`. Todas lanzan `ApiAssertionError`/`SchemaValidationError` con mensaje descriptivo (incluye el body real recibido). |
| `data_generator.py` | Wrappers sobre `Faker` (`random_first_name`, `random_last_name`, `random_email`, `random_age`, `random_password`). Punto único de generación de datos dinámicos, usado por los `builders/`. |

## `tests/` (casos de prueba)

4 carpetas físicas, pero solo `component/` y `e2e/` tienen lógica propia (ver
[`tests/docs/ARCHITECTURE.md`](ARCHITECTURE.md) §2.1); `smoke/` y `regression/`
son reexportaciones por import de tests que ya viven en `component/`. `pytest.ini`
excluye `smoke/`/`regression/` de `testpaths` para que la suite por defecto no
duplique llamadas reales a la API. Cada carpeta hereda las fixtures de
`tests/conftest.py` (no se duplican). Ver [`tests/README.md`](../tests/README.md)
para los comandos de ejecución.

| Carpeta | Archivo | Resumen |
|---|---|---|
| `conftest.py` (raíz de `tests/tests/`) | — | Fixture de dominio: `users_service`. |
| `component/` | `test_SIM_TC_12_create_user.py` | POST /users/add: camino feliz + campo obligatorio faltante (`xfail`, borde de negocio) + límites de longitud de `lastName` (borde puro) + edad fuera de rango (`xfail`, borde puro). |
| `component/` | `test_SIM_TC_13_get_user.py` | GET /users/{id}: camino feliz + ids inválidos 0/-1/9999999 (borde puro) + id vacío → lista completa (alterno de negocio). |
| `component/` | `test_SIM_TC_14_update_user.py` | PUT /users/{id}: camino feliz + ids inválidos (borde puro) + id vacío (`xfail`, alterno) + límites de longitud de `lastName` (borde puro) + `lastName` vacío (`xfail`, alterno). |
| `component/` | `test_SIM_TC_15_delete_user.py` | DELETE /users/{id}: solo camino feliz — el TC no define casos de regresión. |
| `smoke/` | `test_smoke_users_manager.py` | Import de los 4 caminos felices (`SIM-TC-12/13/14/15`) — 0 líneas de lógica propia. |
| `regression/` | `test_reg_users_manager.py` | Import de los 4 caminos felices + 4 alternos de negocio (campo faltante, id vacío en GET/PUT, `lastName` vacío) — excluye los bordes puros (ids/longitudes numéricas), que quedan solo en `component/`. |
| `e2e/` | — | Sin casos hoy — ninguno de los TCs actuales pide un flujo encadenado. |

## Raíz del proyecto

| Archivo | Resumen |
|---|---|
| `conftest.py` | Fixtures de infraestructura compartidas por toda la suite: `environment`, `http_client` (sin auth), `session_manager`, `authenticated_client`. Todas con `scope="session"`. También define: `--no-html-report` (boolean para activar/desactivar el HTML), `pytest_runtest_logreport` (recolecta resultado + items de `console_reporter` por test) y `pytest_sessionfinish`, que agrupa los tests por carpeta física (`component`/`smoke`/`regression`/`e2e`, deducida del nodeid) y llama a `html_report.generate_report` una vez por carpeta, guardando en `tests/<carpeta>/reports/` con fecha y hora en el nombre (y el nombre del archivo de test si la corrida tocó uno solo) — así cada corrida queda en el historial en vez de sobrescribir. |
| `pytest.ini` | Config de ejecución: `-v -s` (stdout sin capturar), marcadores `component`/`e2e` (aplicados siempre) y `smoke`/`regression` (documentales, la selección real es por carpeta), `testpaths = tests/component tests/e2e` (suite por defecto sin duplicados), convención de nombres de archivos/funciones de test. |
| `requirements.txt` | Dependencias fijadas por versión exacta para builds reproducibles. |
| `.env.example` | Plantilla de variables de entorno (URLs por ambiente, credenciales, timeout). Se copia a `.env` (git-ignorado) y se completa con valores reales. |
