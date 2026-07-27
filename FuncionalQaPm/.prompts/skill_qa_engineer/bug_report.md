# Reporte de Bugs en Jira — extensión de `skill_qa_engineer`

> Capacidad **nueva** para este proyecto (antes `skill_qa_engineer` solo gestionaba Casos
> de Prueba en AIO Tests). Se activa cuando el usuario pide reportar un defecto/bug
> encontrado durante testing manual o exploratorio, en cualquier ambiente (Dev/Prod).
>
> Usa el **MCP de Atlassian/Jira** (`com.atlassian/atlassian-mcp-server`, mismo servidor que
> usa `skill_project_manager`) — **ya autorizado y verificado en vivo** para el sitio
> `tu-sitio.atlassian.net` (usuario conectado: `tu-email@ejemplo.com`, cloudId
> `<TU_CLOUD_ID>`). Si en una sesión futura el MCP no aparece
> autorizado, detente e indícale al usuario que debe autorizarlo (`/mcp` o conexión en
> claude.ai) — nunca inventes un `issue_key` ni asumas éxito.

## Campos reales confirmados para `TP` — verificado vía `getJiraIssueTypeMetaWithFields`

> **A diferencia de lo asumido inicialmente**: `TP` sí tiene un tipo de issue de Bug y todos
> estos campos custom configurados. Verificado en vivo el 2026-07-07 contra
> `cloudId=<TU_CLOUD_ID>`, `projectIdOrKey=TP`, `issueTypeId=10005`.
> Si el proyecto cambia su configuración de campos en el futuro, re-ejecutar el Paso 0 para
> refrescar esta tabla — no asumir que sigue vigente indefinidamente.
>
> **Actualización 2026-07-08**: se agregó el campo **`Device Type`** (`customfield_10113`,
> obligatorio, sin valor por defecto) a la configuración del tipo de issue "Error" en `TP` —
> re-verificado en vivo vía `getJiraIssueTypeMetaWithFields`. Confirmado sobre el ticket real
> `TP-13`.

**Importante — el tipo de issue no se llama literalmente "Bug"**: su nombre interno
(`untranslatedName`) es `Bug`, pero el nombre visible/localizado en este sitio es **`Error`**
(`issueTypeId: "10005"`). Al crear el issue, usa el **id** `10005` (o el nombre `"Error"` si
la herramienta MCP solo acepta nombre) — nunca asumas que el string `"Bug"` funciona como
`issueTypeName`.

| Campo | `fieldId` real | Tipo | Valores permitidos (`allowedValues`) | Obligatorio |
|---|---|---|---|---|
| Resumen (título) | `summary` | string nativo | — | Sí |
| Descripción | `description` | string nativo | — | No (pero esta skill nunca la deja vacía) |
| Prioridad (nativa Jira) | `priority` | priority nativo | `Highest`(id 1) / `High`(id 2) / `Medium`(id 3) / `Low`(id 4) / `Lowest`(id 5) | Sí |
| **Severity** | `customfield_10073` | select | `Critical`(10020) / `High`(10021) / `Medium`(10022) / `Low`(10023) | Sí |
| **Steps To Reproducible** | `customfield_10074` | textarea (ADF) | texto libre — usar `orderedList` en ADF | Sí |
| **Precondition** | `customfield_10075` | textarea (ADF) | texto libre — usar `bulletList` en ADF | Sí |
| **Environment** | `customfield_10076` | select | `Dev`(10024) / `Prod`(10025) | Sí |
| **Testing Stage** | `customfield_10077` | select | `Dev`(10026) / `Staging / Pre Release`(10027) / `Release product`(10028) / `Product`(10029) | Sí |
| **Actual Result** | `customfield_10078` | textarea (ADF) | texto libre | Sí |
| **Expected Result** | `customfield_10079` | textarea (ADF) | texto libre | Sí |
| **Bug Typification** | `customfield_10080` | select | `UI`(10030) / `UX`(10031) / `Integracion`(10032) / `Funcionalidad`(10033) / `Configuracion`(10034) / `Documentacion`(10035) | Sí |
| **Device Type** | `customfield_10113` | select | `PC-Web`(10068) / `iOS`(10069) / `Android`(10070) | Sí |
| Persona asignada | `assignee` | user nativo | — | No |
| Informador | `reporter` | user nativo | — | Sí (default: usuario conectado) |
| Etiquetas | `labels` | array nativo | — | No |
| Principal (parent) | `parent` | issuelink nativo | — | No |
| Incidencias enlazadas | `issuelinks` | array nativo | — | No |

**Campos que NO existen en `TP`** (no inventarlos): `Screen Component`, `Branch`,
`Environment Impact` (TuEmpresa-specific), `Found By`/`Testing Type`. Si el usuario los menciona,
inclúyelos como texto libre dentro de `description`, nunca como `customfield_XXXXX` inventado.

### Paso 0 — Refrescar campos si ha pasado mucho tiempo (opcional, ya resuelto para hoy)

Si esta tabla tiene más de unas semanas o el usuario reporta un error 400 en un campo
custom, re-ejecuta:

```
getJiraProjectIssueTypesMetadata(cloudId, projectIdOrKey="TP")   → confirmar issueTypeId de Bug/Error
getJiraIssueTypeMetaWithFields(cloudId, projectIdOrKey="TP", issueTypeId, requiredFieldsOnly=false)
```

y actualiza la tabla de arriba con lo que cambie.

## Inputs

### Obligatorios
- **`{summary_context}`**: qué falló y dónde (el usuario lo describe en su propio texto).
- **`{version}`**: versión/build donde se encontró el defecto (se anota en `description`; `TP` no tiene un campo dedicado `fixVersions` obligatorio confirmado — verificar si el usuario lo pide explícitamente).
- **`{project}`**: project key de Jira. Default: `TP` (mismo `PROJECT_KEY` que usa AIO Tests). Si el usuario reporta un bug de otro proyecto, debe indicarlo explícitamente y se debe repetir el Paso 0 para ese proyecto (los `customfield_XXXXX` no son necesariamente los mismos en otro proyecto).
- **`{device_type}`**: `PC-Web` / `iOS` / `Android` → mapea a `customfield_10113`. **Sin valor por defecto** (`hasDefaultValue: false` en Jira) — a diferencia de `{severity}`/`{environment}`/etc., este campo **nunca se infiere ni se asume**: si el usuario no lo indica, pregúntaselo explícitamente antes de crear el issue. Omitirlo provoca un 400 de Jira ("Device Type es obligatorio").

### Opcionales (con default)
- **`{severity}`**: `Critical` / `High` / `Medium` / `Low` → mapea directo a `customfield_10073`. Default: `Medium`.
- **`{environment}`**: `Dev` / `Prod` → mapea a `customfield_10076`. Default: `Dev`.
- **`{testing_stage}`**: `Dev` / `Staging / Pre Release` / `Release product` / `Product` → mapea a `customfield_10077`. Default: `Dev`.
- **`{typification}`**: `UI` / `UX` / `Integracion` / `Funcionalidad` / `Configuracion` / `Documentacion` → mapea a `customfield_10080`. Default: inferir del contexto del defecto (ej. problema visual → `UI`; dato incorrecto en llamada a otro servicio → `Integracion`).
- **`{reporter}`**: quién reporta. Default: usuario de la cuenta Atlassian conectada (`atlassianUserInfo`).
- **`{owner}`**: a quién asignar. Default: sin asignar.
- **`{ticket}`**: HU/ticket relacionado, para dar contexto adicional y enlazar el bug (`parent` o `issuelinks`).
- **`{image}`**: evidencia visual — se agrega como comentario descriptivo (Paso 5).

## Flujo de ejecución

### Paso 1 — Construir el título (`summary`)

Debe describir **QUÉ falla y DÓNDE**, nunca un título genérico.

```
MAL:  "Error en login"
BIEN: "Login con credenciales válidas se queda cargando indefinidamente sin redirigir al home"
```

### Paso 2 — Construir la descripción (`description`)

`description` contiene **SOLO**:
1. Descripción detallada del defecto (qué pasa, dónde, cuándo).
2. Riesgo/impacto para el usuario final.

**NUNCA dupliques aquí** precondición, pasos, resultado actual o resultado esperado — esos
campos ya existen dedicados en `TP` (ver tabla de arriba) y van directo a sus
`customfield_XXXXX` correspondientes, no a `description`.

```
**Descripción del defecto**
{qué pasa, dónde, cuándo}

**Riesgo / Impacto para el usuario**
{consecuencias potenciales si no se corrige}
```

### Paso 3 — Mapear campos obligatorios de `TP`

| Input del usuario | Campo destino | Formato a enviar |
|---|---|---|
| `{severity}` | `customfield_10073` | `{"id": "10020"}` (Critical) / `"10021"` (High) / `"10022"` (Medium) / `"10023"` (Low) |
| Derivado de `{severity}` (ver mapeo abajo) | `priority` | `{"id": "1"}`…`{"id": "5"}` |
| Precondición (analizar contexto) | `customfield_10075` | ADF `bulletList` (objeto real, no string) |
| Pasos para reproducir | `customfield_10074` | ADF `orderedList` (objeto real, no string) |
| Resultado actual | `customfield_10078` | ADF `paragraph` (objeto real, no string) |
| Resultado esperado | `customfield_10079` | ADF `paragraph` (objeto real, no string) |

> **Confirmado en vivo (2026-07-08, creación de `TP-17`)**: estos 4 campos son `textarea` pero
> **exigen un documento ADF real** (`{"type": "doc", "version": 1, "content": [...]}`), no un
> string de texto plano. Un intento de enviarlos como string simple fue rechazado por Jira con
> HTTP 400: `"El valor del campo no es un contenido válido del formato de los documentos de
> Atlassian (ADF)"` para los 4 campos a la vez. Construye siempre el objeto ADF completo
> (`doc` → `orderedList`/`bulletList`/`paragraph` → `listItem`/`text`) antes de invocar
> `createJiraIssue` — nunca pases texto plano ni Markdown crudo en estos 4 campos.
| `{environment}` | `customfield_10076` | `{"id": "10024"}` (Dev) / `{"id": "10025"}` (Prod) |
| `{testing_stage}` | `customfield_10077` | `{"id": "10026"}`…`{"id": "10029"}` |
| `{typification}` | `customfield_10080` | `{"id": "10030"}`…`{"id": "10035"}` |
| `{device_type}` | `customfield_10113` | `{"id": "10068"}` (PC-Web) / `"10069"` (iOS) / `"10070"` (Android) |
| — | `labels` | `["created_by_ai"]` (+ `{ticket}` si aplica) |

**Mapeo Severity → Priority nativo** (ambos son obligatorios en `TP`, son campos distintos):

| `{severity}` | `customfield_10073` (Severity) | `priority` nativo |
|---|---|---|
| Critical | id `10020` | `Highest` (id `1`) |
| High | id `10021` | `High` (id `2`) |
| Medium | id `10022` | `Medium` (id `3`) |
| Low | id `10023` | `Low` (id `4`) |

Si `{owner}` fue provisto: resolver su `accountId` con `lookupJiraAccountId` antes de asignar.

### Paso 4 — Crear el issue

Invoca `createJiraIssue` con:
- `cloudId`: `<TU_CLOUD_ID>` (o resolver de nuevo con `getAccessibleAtlassianResources` si cambia de sitio).
- `projectKey`: `{project}` (default `TP`).
- `issueTypeName` o `issueTypeId`: `"10005"` (nombre visible `Error`).
- `summary`, `description` (Paso 1-2).
- Todos los `customfield_10073`–`10080` y `customfield_10113` (Device Type) mapeados en el Paso 3.
- `priority`, `labels`, `assignee` (si aplica).

Guarda `{bug_key}` y `{bug_url}` de la respuesta real — **nunca reportes éxito sin confirmar
la respuesta de la herramienta**. Si algún campo obligatorio falta o tiene un valor no
permitido, la API devolverá 400 con el detalle — corrige el campo señalado, nunca lo omitas.

### Paso 5 — Adjuntar evidencia (condicional)

Si `{image}` fue provisto, agrega un comentario al issue creado (`addCommentToJiraIssue`) con
una descripción técnica detallada de la imagen.

### Paso 6 — Confirmación al usuario

```
El bug se registró exitosamente en el proyecto {project} (tipo "Error") con severidad
{severity}, prioridad {priority_derivada}, ambiente {environment}, etapa {testing_stage},
device type {device_type}.

Issue: {bug_key}
Enlace: {bug_url}
```

## Reglas estrictas

- El título SIEMPRE debe describir qué falla y dónde, nunca un label genérico.
- `description` contiene SOLO defecto + riesgo — nunca precondición/pasos/resultados (esos van a sus custom fields dedicados).
- Si la herramienta MCP falla, reporta el error tal cual al usuario — **nunca imprimas el
  payload JSON como alternativa ni asumas éxito silencioso**.
- No inventes un `{owner}` ni un `{reporter}` — si no se puede resolver el `accountId`, crea
  el bug sin asignar y díselo al usuario.
- No inventes campos custom que no aparezcan en la tabla confirmada de arriba — si el usuario
  pide un campo no listado (ej. "Screen Component"), inclúyelo como texto dentro de `description`
  y anota que ese campo no existe configurado en `TP`.
- Todos los campos marcados "Obligatorio: Sí" en la tabla deben enviarse siempre — nunca
  omitir uno para "simplificar" la creación.
- **`{device_type}` (`customfield_10113`) es obligatorio y no tiene default en Jira** —
  a diferencia de `{severity}`/`{environment}`/`{testing_stage}`/`{typification}`, nunca lo
  infieras del contexto: si el usuario no lo mencionó al pedir el reporte, pregúntaselo
  explícitamente (`PC-Web` / `iOS` / `Android`) antes de invocar `createJiraIssue`. Omitirlo
  o adivinarlo se considera una violación de esta regla, igual que omitir Severity o Environment.

## Referencias

- [references/severity-priority-glossary.md](references/severity-priority-glossary.md)
- [skill_project_manager/tools.md](../skill_project_manager/tools.md) — mismo patrón de
  descubrimiento de herramientas MCP usado aquí, para mantener consistencia en el proyecto.
