# Herramientas de `skill_qa_engineer`

Esta skill integra con **AIO Tests** (Jira Cloud) mediante dos artefactos equivalentes en esta misma carpeta:

1. [aio_tests_client.py](aio_tests_client.py) — cliente CLI/librería Python, usa `requests` directo contra la API REST de AIO Tests.
2. [aio_tests_api.py](aio_tests_api.py) — servicio **FastAPI** que envuelve ese mismo cliente y lo expone como endpoints HTTP locales (útil si otro proceso/agente quiere invocar por HTTP en vez de por CLI).

Ambos comparten la misma configuración (`.env`) y la misma lógica de autenticación/errores.

> **Fix aplicado (2026-07-08)**: `aio_tests_client.py` ahora resuelve `customFields` en
> formato amigable (`{"Testing Layers": "Android"}`) al formato real que exige el OpenAPI de
> AIO Tests — un **array** de objetos `{"ID", "name", "value": {"ID", "value"}}` — vía la
> función `_resolve_custom_fields` (misma lógica que `_resolve_tags` para `labels`, consulta
> `GET /config/customfield` una vez y cachea en memoria). Antes de este fix, `customFields` se
> pasaba tal cual al payload (dict simple), lo cual la API real habría rechazado con 400.
> Verificado en vivo contra el proyecto `TP`: resuelve `Testing Layers`/`Test Type Cycle`
> correctamente, y lanza `AioTestsConfigError` (sin llegar a golpear la API) si el nombre del
> campo o del valor no existe — nunca inventa un ID.

## 0. Configuración previa (obligatoria)

1. Copia `.env.example` a `.env` en esta carpeta y completa los valores reales:

   ```bash
   cp .env.example .env
   ```

   | Variable | Descripción |
   |---|---|
   | `AIO_API_TOKEN` | Token de AIO Tests (sin el prefijo `AioAuth `; el script lo agrega automáticamente al armar la cabecera `Authorization`) |
   | `PROJECT_KEY` | Clave del proyecto Jira, por defecto `TP` |
   | `AIO_BASE_URL` | URL base de la API de AIO Tests (región EU o US según tu instancia) |

2. Instala dependencias. Este sistema tiene Python "externally-managed" (PEP 668): no se
   puede hacer `pip install` global sin `--break-system-packages`. Se usa un entorno virtual
   aislado dentro de esta misma carpeta (ya creado, no requiere permisos de administrador):

   ```bash
   cd .prompts/skill_qa_engineer
   python3 -m venv .venv                       # ya ejecutado — omitir si .venv ya existe
   ./.venv/bin/pip install -r requirements.txt  # ya ejecutado
   ```

   Para usar el CLI o levantar la API con estas dependencias:

   ```bash
   ./.venv/bin/python3 aio_tests_client.py list --max-results 10
   ./.venv/bin/uvicorn aio_tests_api:app --reload --port 8000
   ```

   El directorio `.venv/` está excluido de git ([.gitignore](.gitignore)).

**Endpoints confirmados.** La base URL (`https://tcms.aiojiraapps.com/aio-tcms`) y los paths usados en `aio_tests_client.py` fueron verificados contra el OpenAPI oficial que AIO Tests publica en `https://tcms.aiojiraapps.com/aio-tcms/aiotcms-static/api-docs/` (spec en `.../api/v1/openapi.json`), y contra la documentación pública en [aiosupport.atlassian.net/wiki/spaces/AioTests/pages/2025619567](https://aiosupport.atlassian.net/wiki/spaces/AioTests/pages/2025619567). El parámetro `jiraProjectId` de la API acepta tanto la key del proyecto (`TP`) como su ID numérico, así que no hace falta resolver el ID manualmente.

## 1. Vía CLI — `aio_tests_client.py`

> Todos los comandos usan el intérprete del entorno virtual (`.venv/bin/python3`) creado en
> el Paso 0 — **no** el `python3` del sistema, que no tiene `requests`/`python-dotenv`
> instalados. Rutas dadas desde la **raíz del repositorio** (no requieren `cd` previo).

### Crear un Caso de Prueba
```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py create --json-file caso.json
```
donde `caso.json` sigue la estructura descrita en [test_spec.md](test_spec.md).

### Actualizar un Caso de Prueba existente
```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py update --id 1234 --json '{"description": "Nueva descripción del caso"}'
```

### Obtener un Caso de Prueba puntual
```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py get --id 1234
```

### Listar Casos de Prueba del proyecto (para mantenimiento)
```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py list --max-results 100
```

### Buscar por título (evitar duplicados antes de crear)
```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py search --title-contains "login"
```

También puede importarse como módulo (usando el intérprete del `.venv`):
```python
from aio_tests_client import create_test_case, update_test_case, get_test_case, list_test_cases, search_test_cases
```

## 2. Vía HTTP — `aio_tests_api.py` (FastAPI)

Levantar el servicio local (también con el intérprete del `.venv`; `uvicorn` necesita ejecutarse
desde dentro de la carpeta para resolver el import `aio_tests_api`):
```bash
cd .prompts/skill_qa_engineer
./.venv/bin/uvicorn aio_tests_api:app --reload --port 8000
```

Documentación interactiva (Swagger UI) generada automáticamente: `http://127.0.0.1:8000/docs`.

### Endpoints expuestos

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/health` | Verifica que el servicio está arriba y qué `project_key`/`base_url` usa por defecto |
| `POST` | `/test-cases` | Crea un nuevo Caso de Prueba |
| `PUT` | `/test-cases/{test_case_id}` | Actualiza (parcial o totalmente) un Caso de Prueba existente |
| `GET` | `/test-cases/{test_case_id}` | Obtiene el detalle de un Caso de Prueba puntual |
| `GET` | `/test-cases?max_results=&start_at=` | Lista/pagina los Casos de Prueba del proyecto |

### Ejemplo — crear vía HTTP
```bash
curl -X POST http://127.0.0.1:8000/test-cases \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Login exitoso con credenciales válidas",
    "description": "Valida que un usuario registrado pueda iniciar sesión correctamente.",
    "precondition": "El usuario existe previamente y está activo.",
    "priority": "High",
    "labels": ["login", "smoke"],
    "steps": [
      {"step": "Navegar a la pantalla de login", "test_data": "URL: /login", "expected_result": "Se muestra el formulario con usuario y contraseña"},
      {"step": "Ingresar credenciales válidas y presionar Ingresar", "test_data": "usuario: qa_user@ejemplo.com", "expected_result": "El sistema redirige al dashboard principal"}
    ]
  }'
```

### Ejemplo — actualizar vía HTTP
```bash
curl -X PUT http://127.0.0.1:8000/test-cases/1234 \
  -H "Content-Type: application/json" \
  -d '{"priority": "Low"}'
```

## Manejo de errores (ambas vías)

| Código HTTP | Significado en este contexto |
|---|---|
| `200` / `201` | Operación exitosa (lectura / creación) |
| `400` | JSON inválido o payload sin campos para actualizar |
| `401` | `AIO_API_TOKEN` inválido, expirado o cabecera `Authorization` ausente |
| `403` | El token no tiene permisos sobre el proyecto/recurso |
| `404` | Proyecto o Caso de Prueba (ID) no existe |
| `429` | Rate limiting de AIO Tests alcanzado |
| `500` / `502` / `503` | Error del servidor de AIO Tests o de red al contactarlo |

El cliente CLI imprime el error por consola (`logger.error`) y termina con `sys.exit(1)`. El servicio FastAPI traduce esos mismos errores a `HTTPException` con el código correspondiente.

## 3. Dependencias y campos pendientes de configuración

Tabla viva de lo que **no** se pudo instalar/confirmar automáticamente en esta sesión, con el
motivo exacto. Actualízala cuando se resuelva alguno de estos puntos.

### Dependencias

| Elemento | Estado | Motivo |
|---|---|---|
| `requests`, `python-dotenv`, `fastapi`, `uvicorn`, `pydantic` | ✅ Instalado | Instalado en `.venv/` local (sin privilegios de administrador; el Python del sistema es "externally-managed" por PEP 668). |
| MCP `smartbear-zephyr`, `appium-mcp`, `playwright-mcp`, `aisquare-playwright` | ✅ Ya configurados en `.mcp.json` | Gestionados por el harness (`npx`), no requieren instalación manual de este agente. |
| MCP `mobile-mcp` (`@mobilenext/mobile-mcp@latest`, https://github.com/mobile-next/mobile-mcp) | ✅ Configurado **y verificado en vivo** | Añadido a `.mcp.json` con confirmación explícita del usuario. Probado con `mobile_list_available_devices` → respondió `{"devices":[]}` (conexión OK; simplemente no hay emulador/dispositivo activo ahora mismo). Es la herramienta preferida de [exploration.md](exploration.md) en modo móvil; `appium-mcp` queda como alternativa para control de sesión más fino. |
| `AIO_API_TOKEN` real | ✅ Confirmado presente | Verificado en `.env` (esta carpeta) — valor real cargado, no placeholder. No se imprime aquí por ser secreto. |
| `PROJECT_KEY` (AIO Tests / Jira) | ✅ Confirmado: `TP` | Verificado en `.env` — coincide con la convención documentada en todo el skill. |
| MCP `aio-tests-mcp` (hosted, `https://tcms-prod-us.aiojiraapps.com/aiotcms-mcp/v1/...`) | ✅ Configurado **y verificado en vivo** | Expone `get_project`, `get_test_case_schema`, `search_test_cases`, `create_test_case`, `update_test_case`, `get_test_case`, `get_test_case_versions`, `get_folder_hierarchy`, `create_folder`, `update_folder`, `get_tags`. Probado: `get_project(TP)` → `{"isEnabled": true}`; `get_test_case_schema(TP, "fields")` → schema completo de campos, la fuente más confiable (más completa que armar llamadas REST a mano). Rol: **fallback / tareas no cubiertas por la API REST directa** (ej. `get_folder_hierarchy`) — `aio_tests_client.py` sigue siendo la vía primaria para create/update/search por ya estar probado y integrado. |
| MCP `com.atlassian/atlassian-mcp-server` | ✅ **Autorizado y verificado en vivo** | El panel "MCP SERVERS" de VS Code mostraba la instancia de este workspace como "Disabled" (una instancia "Global" separada ya estaba autenticada); tras habilitarla, se confirmó con `ToolSearch` + llamadas reales: `getAccessibleAtlassianResources` → sitio `tu-sitio.atlassian.net` (cloudId `<TU_CLOUD_ID>`); `atlassianUserInfo` → usuario `tu-email@ejemplo.com`. Ya no bloquea nada. |
| Android SDK / `ANDROID_HOME` para `appium-mcp` | ⚠️ Sin verificar | Solo relevante si se usa [exploration.md](exploration.md) en modo móvil vía `appium-mcp` (alternativa); `mobile-mcp` no depende de esto. |
| Emulador Android sobre Windows 11 (host separado del entorno donde corre este agente) | ⚠️ Sin verificar | Si el emulador Android (Android Studio / AVD) corre en un host Windows 11 distinto al proceso que ejecuta `mobile-mcp`/`appium-mcp` (ej. este agente en WSL2/Linux), ambos deben poder alcanzar el mismo `adb` (mismo host, o `adb connect {ip-windows}:5555` / puerto ADB expuesto). No asumas conectividad automática entre WSL2 y el emulador del host Windows — verifica con `mobile_list_available_devices` antes de iniciar una exploración; si devuelve `{"devices":[]}` con el emulador ya abierto en Windows, es señal de que falta este puente ADB, no un error de la MCP. |
| `jiraRequirementIDs` (vínculo nativo TC↔Jira issue "Requirement" en AIO Tests) | ❌ **No persiste vía `update_test_case`/PUT** — probado en vivo (2026-07-08) sobre `TP-TC-6`: `PUT` con `jiraRequirementIDs: [10109]` (ID numérico real del issue `TP-14`) devolvió HTTP 200, pero el `GET` posterior mostró el campo vacío (`[]`) — la API lo ignora silenciosamente. No usar este campo como mecanismo de trazabilidad hasta confirmar el formato/endpoint correcto (posiblemente requiera un endpoint de vínculo dedicado, no el PUT del caso). El mecanismo de trazabilidad válido y ya verificado sigue siendo título `[{ticket}]` + `labels` (Regla 1 y Regla 8 de [references/formatting-rules.md](references/formatting-rules.md)). |

### Campos de Jira / AIO Tests — estado de confirmación

| Campo | Estado en `TP` | Detalle |
|---|---|---|
| `Testing Layers` (AIO Tests, customField) | ✅ **Confirmado en vivo** vía `get_test_case_schema` — `fieldId: 2`, obligatorio | Valores: `Web`(4)/`Api`(5)/`Data Base`(6)/`Logs`(7)/`iOS`(8)/`Android`(9). Ver [references/formatting-rules.md](references/formatting-rules.md) Regla 10. |
| `Test Type Cycle` (AIO Tests, customField) | ✅ **Confirmado en vivo** — `fieldId: 1`, obligatorio | Valores: `Regresion`(1)/`Smoke`(2)/`N/A`(3). Dimensión de rol en el ciclo, no de naturaleza del TC. |
| `TC Type` / "Tipos de Caso" | ⚠️ **Corregido dos veces** — sí existe, pero como campo **nativo** `type` (se ve como **"Tipo"** en la UI de Jira, confirmado en tu captura de pantalla), no como `customField` ni con el nombre "Tipos de Caso" | Valores reales: `Component`/`Integration`/`E2E`/`API`/`Performance`/`Security`/`Carga / Estrés`. Se envía como `{"name": "..."}` en el campo `type`, nunca dentro de `customFields`. |
| `Estimated Time Range` / "Esfuerzo Estimado" | ⚠️ **Corregido dos veces** — sí existe, como campo **nativo** `estimatedEffort` (entero en segundos, no un dropdown de rangos) | Confirmado en tu captura de pantalla (aparece junto a Carpeta/Propietario, fuera de "Campos personalizados"). `aio_tests_client.py` ya lo soporta como passthrough directo. |
| `status` (Estado) | ✅ **Confirmado en vivo** — obligatorio, no estaba cubierto antes | Valores: `Draft`/`Ready`/`Under Review`/`Refactor`/`Deprecated`. **Bug corregido**: `aio_tests_client.py` no lo fijaba ni lo envolvía en `{"name": ...}`; ahora `create_test_case` lo defaultea a `"Draft"` y `update_test_case` nunca lo pisa a menos que se pida explícitamente. |
| `Severity` (Jira, Bug/`Error`) | ✅ **Confirmado en vivo** — `customfield_10073` | Verificado vía `getJiraIssueTypeMetaWithFields` (proyecto `TP`, issueTypeId `10005`). Valores: `Critical`(10020)/`High`(10021)/`Medium`(10022)/`Low`(10023). Detalle completo en [references/severity-priority-glossary.md](references/severity-priority-glossary.md). |
| `Steps To Reproducible` | ✅ Confirmado — `customfield_10074` (textarea/ADF, obligatorio) | Ver [bug_report.md](bug_report.md). |
| `Precondition` (para Bug) | ✅ Confirmado — `customfield_10075` (textarea/ADF, obligatorio) | Ver [bug_report.md](bug_report.md). |
| `Environment` | ✅ Confirmado — `customfield_10076` | Valores: `Dev`(10024)/`Prod`(10025). Nótese: solo 2 valores, no 3 como se asumía inicialmente. |
| `Testing Stage` | ✅ Confirmado — `customfield_10077` | Valores: `Dev`(10026)/`Staging / Pre Release`(10027)/`Release product`(10028)/`Product`(10029). |
| `Actual Result` | ✅ Confirmado — `customfield_10078` (textarea/ADF, obligatorio) | Ver [bug_report.md](bug_report.md). |
| `Expected Result` | ✅ Confirmado — `customfield_10079` (textarea/ADF, obligatorio) | Ver [bug_report.md](bug_report.md). |
| `Bug Typification` | ✅ Confirmado — `customfield_10080` | Valores: `UI`(10030)/`UX`(10031)/`Integracion`(10032)/`Funcionalidad`(10033)/`Configuracion`(10034)/`Documentacion`(10035). Set distinto al de otros proyectos del equipo (no tiene Performance/Security/Deeplinks/Analytics). |
| `priority` nativo (Bug) | ✅ Confirmado | `Highest`(1)/`High`(2)/`Medium`(3)/`Low`(4)/`Lowest`(5). Obligatorio junto con `Severity` (son campos distintos). |
| Tipo de issue "Bug" | ✅ Confirmado — `issueTypeId: "10005"`, nombre visible **`Error`** | El nombre interno (`untranslatedName`) es `Bug`, pero el nombre mostrado en Jira es `Error`. Usar el id al crear el issue. |
| `Componente BBR` (custom field de Zephyr Scale) | ❌ No aplica | Es un campo de Zephyr Scale, herramienta que `TP` no usa para TCs (usa AIO Tests). |
| `Screen Component`, `Branch`, `Environment Impact` (de otros proyectos del equipo) | ❌ No existen en `TP` | Confirmado en la respuesta de `getJiraIssueTypeMetaWithFields` — no aparecen entre los 27 campos del tipo `Error`. No inventarlos. |
| Prioridades válidas de AIO Tests para `TP` | ✅ **Confirmado en vivo** | Verificado con `GET /api/v1/project/TP/config/testcase/priority`: `High`(ID 2) / `Medium`(ID 3) / `Low`(ID 4). Coincide exactamente con la convención que ya usaba `aio_tests_client.py` — no requiere cambios de código. |

> **Cierre de la Tarea 7 del usuario**: se pidió "usar el MCP de Zephyr para mapear los IDs de
> estos campos en Jira" — eso seguía siendo incorrecto (Zephyr no ve campos de issues de Jira),
> pero con el MCP de Atlassian ya autorizado, `bug_report.md` Paso 0 obtuvo el mapeo real y
> completo directamente de Jira. Todos los campos pedidos quedaron confirmados con IDs reales.

**Único pendiente restante**: `aio-tests-mcp` ya fue añadido a `.mcp.json` con confirmación
explícita del usuario — solo falta que el harness reconecte los servidores MCP (una nueva
sesión, o el mismo mecanismo que activó `mobile-mcp` en esta conversación) para que sus tools
queden invocables. `mobile-mcp` y Atlassian ya quedaron resueltos y verificados en vivo, y
todos los campos de Jira/AIO Tests están confirmados con datos reales.
