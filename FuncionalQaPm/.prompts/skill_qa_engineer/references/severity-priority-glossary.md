# Glosario de Severidad / Prioridad — Bugs (Jira, `TP`)

> **Confirmado en vivo** (2026-07-07, vía `getJiraIssueTypeMetaWithFields` contra
> `cloudId=<TU_CLOUD_ID>`, proyecto `TP`, issue type `Error`/id `10005`):
> `TP` **sí tiene** un campo custom dedicado `Severity` (`customfield_10073`), distinto del
> campo nativo `priority`. Ambos son obligatorios al crear un bug — se envían juntos, no son
> intercambiables.

## Severity (campo custom, `customfield_10073`)

| Severidad observada por QA | Valor / `id` en `customfield_10073` | Cuándo usarla |
|---|---|---|
| **Critical** | `Critical` / id `10020` | Bloquea un flujo core (login, pagos, agendar cita) sin workaround; afecta a todos los usuarios. |
| **High** | `High` / id `10021` | Rompe una funcionalidad importante; existe workaround incómodo o parcial. |
| **Medium** | `Medium` / id `10022` | Defecto funcional acotado a un caso de borde o a una parte no crítica del flujo. |
| **Low** | `Low` / id `10023` | Cosmético, texto, alineación, o comportamiento menor sin impacto funcional real. |

Formato al enviar: `"customfield_10073": {"id": "10020"}` (usar el `id`, no el string, salvo
que la herramienta MCP indique lo contrario).

## Priority (campo nativo `priority`, también obligatorio en `TP`)

`TP` exige tanto `Severity` como `priority` nativo. Usa el mapeo directo (mismo nivel de
severidad, nombre distinto por ser la escala estándar de 5 niveles de Jira):

| `Severity` | `priority.name` | `priority.id` |
|---|---|---|
| Critical | `Highest` | `1` |
| High | `High` | `2` |
| Medium | `Medium` | `3` |
| Low | `Low` | `4` |
| *(sin uso por defecto)* | `Lowest` | `5` |

Formato al enviar: `"priority": {"id": "1"}` (o `{"name": "Highest"}` si la herramienta lo
prefiere por nombre).

## Si estos IDs cambian en el futuro

Los `customfield_XXXXX` y sus opciones son específicos de la configuración actual de `TP`.
Si `TP` reconfigura estos campos, o si se reporta un bug en otro proyecto Jira, **no asumas
que los mismos IDs aplican** — re-ejecuta el Paso 0 de [bug_report.md](../bug_report.md)
(`getJiraProjectIssueTypesMetadata` + `getJiraIssueTypeMetaWithFields`) y actualiza esta tabla.

## Prioridad de Casos de Prueba (para `steps[].priority` en AIO Tests — no confundir con lo anterior)

Esto es un campo **distinto**, de AIO Tests (no de Jira), usado al crear/actualizar Casos de
Prueba. **Confirmado en vivo** contra `GET /api/v1/project/TP/config/testcase/priority`:

| Nivel | Valor exacto | ID real en AIO |
|---|---|---|
| Alta | `High` | `2` |
| Media | `Medium` | `3` |
| Baja | `Low` | `4` |

Coincide exactamente con la convención que ya usaba `aio_tests_client.py` (`priority.name`) —
no requirió ningún ajuste de código.
