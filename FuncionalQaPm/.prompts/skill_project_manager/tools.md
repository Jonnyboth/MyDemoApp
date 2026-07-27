# Herramientas de `skill_project_manager`

Esta skill cuenta con **dos vías** para insertar una Historia de Usuario en el Backlog de Jira. La vía primaria es siempre el MCP; la vía secundaria (script Python) existe para ejecución fuera del agente (CI, cron, terminal) o como contingencia documentada cuando el MCP no responde.

## 1. Vía primaria — MCP de Atlassian/Jira (nativa)

Este entorno tiene configurado el **Atlassian Rovo MCP Server**, ya conectado a la cuenta Jira vinculada (`tu-email@ejemplo.com`). Al ejecutar el flujo de esta skill, el agente debe:

1. Listar/usar las herramientas MCP expuestas con prefijo `atlassian` / `jira` (por ejemplo, la operación de creación de issues del servidor MCP conectado — su nombre exacto depende de la versión del servidor listada en el entorno; búscala en las herramientas disponibles antes de invocarla si no la tienes ya cargada).
2. Mapear los campos de la HU redactada a los parámetros de esa herramienta:
   - `project_key` → clave del proyecto Jira (ej. `TP`).
   - `issue_type` → `Story`.
   - `summary` → el título corto de la HU.
   - `description` → narrativa Como/Quiero/Para + sección Contexto/Notas + Criterios de Aceptación en Gherkin (todo el bloque, en texto o formato enriquecido si la herramienta lo soporta).
3. No indicar sprint ni board — el issue debe quedar en el **Backlog** (comportamiento por defecto al crear un issue sin sprint asignado).
4. Leer la respuesta de la herramienta MCP para obtener la clave del issue creado (ej. `TP-45`) y, si está disponible, la URL directa. Reportar ambos al usuario.
5. Si la herramienta MCP retorna un error (proyecto inexistente, permisos, tipo de issue no válido en ese proyecto), mostrar el error tal cual al usuario — no reintentar con datos inventados.

## 2. Vía secundaria — Script de contingencia `create_jira_story.py`

Ubicado en esta misma carpeta: [create_jira_story.py](create_jira_story.py).

### Cuándo usarlo
- Cuando se necesita crear la HU **fuera** de una sesión de agente (por ejemplo, un pipeline de CI que refina el backlog automáticamente).
- Como respaldo documentado si, dentro de una sesión de agente, el MCP no está disponible: el agente puede ejecutar el script vía terminal; si el script tampoco logra crear el issue por REST, imprime una instrucción estructurada para que el propio agente complete la creación usando el MCP (ver lógica del script).

### Cómo funciona (resumen; detalle en el propio script)
1. Intenta crear el issue llamando directamente a la **API REST de Jira** (`POST /rest/api/3/issue`) usando las credenciales de entorno.
2. Si la llamada falla (credenciales ausentes/ inválidas, error de red, timeout, respuesta de error de Jira), captura la excepción y **no falla en silencio**: imprime en stdout un bloque `FALLBACK_MCP_INSTRUCTION` en JSON con todos los campos ya normalizados (`project_key`, `summary`, `description`, `issue_type`) listos para que el agente los use al invocar la herramienta MCP de creación de issues descrita en la sección 1.

### Variables de entorno requeridas para la vía REST
| Variable | Descripción |
|---|---|
| `JIRA_BASE_URL` | URL base del sitio Jira Cloud, ej. `https://tuempresa.atlassian.net` |
| `JIRA_EMAIL` | Correo de la cuenta con permisos de creación de issues |
| `JIRA_API_TOKEN` | API token de Atlassian (https://id.atlassian.com/manage-profile/security/api-tokens) |

### Invocación por CLI
```bash
python3 .prompts/skill_project_manager/create_jira_story.py \
  --project-key "TP" \
  --summary "Permitir recuperación de contraseña vía email" \
  --description "Como usuario registrado, quiero poder recuperar mi contraseña por correo, para volver a acceder a mi cuenta sin contactar soporte." \
  --acceptance-criteria "Escenario: Recuperación exitosa
  Dado que el usuario está en la pantalla de login
  Cuando ingresa un correo registrado y solicita recuperar contraseña
  Entonces recibe un correo con un enlace de restablecimiento válido por 30 minutos"
```

También puede importarse como módulo y llamar a `create_user_story(project_key, summary, description, acceptance_criteria, issue_type="Story")` directamente desde otro script Python.
