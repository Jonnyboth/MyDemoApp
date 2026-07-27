# Triage — Anti-duplicados antes de crear Casos de Prueba (AIO Tests)

> Adaptación del pre-flight de triage de 7 gates usado en proyectos con Zephyr Scale, a lo
> que realmente puede verificarse hoy en AIO Tests para el proyecto `TP`. **Verificado en
> vivo** vía la herramienta MCP `get_test_case_schema` (fuente autoritativa completa — más
> confiable que consultar el REST a mano campo por campo): `TP` tiene 2 gates con campo real
> determinista: **L** (`customFields["Testing Layers"]`) y **T** (campo nativo `type`, que en
> la UI de Jira se muestra como **"Tipo"** — con valores `Component`/`Integration`/`E2E`/
> `API`/`Performance`/`Security`/`Carga / Estrés`, el equivalente real del "TC Type" de
> Zephyr). El custom field `Test Type Cycle` (`Regresion`/`Smoke`/`N/A`) es una dimensión
> **distinta** (rol del TC en el ciclo de ejecución) — útil como señal complementaria, pero no
> sustituye a `type` para el Gate T. Las demás 5 dimensiones se evalúan por lectura de texto
> (juicio del agente), como se detalla abajo.

## Gates disponibles hoy en AIO Tests para `TP`

| Gate original (Zephyr) | Equivalente en AIO | Tipo de verificación |
|---|---|---|
| **L — Layer** | `customFields["Testing Layers"]` del TC candidato | ✅ Determinista — comparar valor exacto (`Web`/`Api`/`Data Base`/`Logs`/`iOS`/`Android`) contra la plataforma del ticket |
| **T — TC Type** | Campo nativo `type` ("Tipo" en la UI) del TC candidato | ✅ Determinista — comparar valor exacto (`Component`/`Integration`/`E2E`/`API`/`Performance`/`Security`/`Carga / Estrés`) contra la naturaleza del AC |
| **O — Operative System** | Sin campo dedicado confirmado en `TP` | ⚠️ Judgment — inferir de `Testing Layers` si el valor distingue Android/iOS, o del texto de la precondición/pasos |
| **D — Test Data** | Sin campo dedicado | ⚠️ Judgment — leer `steps[].test_data` del TC candidato y comparar contra los valores concretos del AC |
| **P — Precondición** | Sin campo dedicado (`precondition` es texto libre) | ⚠️ Judgment — leer el texto y evaluar compatibilidad con el estado que requiere el AC |
| **S — Scenarios** | Sin campo dedicado | ⚠️ Judgment — leer título/pasos del TC y comparar contra los `scenarios_required` del AC (Positivo/Negativo/Borde) |
| **C — Coverage** | `description` del TC (rol equivalente al `objective` de Zephyr) | ⚠️ Judgment — el texto debe declarar la validación del AC de forma reconocible, no genérica |

Señal complementaria (no es uno de los 7 gates, pero ayuda a decidir entre candidatos
empatados): `customFields["Test Type Cycle"]` — preferir un candidato `Smoke` cuando el AC
describe el happy path principal, o `Regresion` para flujos alternos/negativos.

Con **L y T** verificables de forma determinista, sube bastante la precisión del nivel
**REUSE** (dos gates estructurales ya no dependen de interpretación), pero **O, D, P, S, C
siguen siendo criterio del agente** — no inventes un "cálculo exacto" donde no lo hay; sé
transparente con el usuario sobre qué se verificó contra un campo real (`Testing Layers`,
`type`) y qué fue juicio de texto (los otros 5 factores).

## Cuándo se ejecuta

**Obligatorio** antes de crear TCs nuevos cuando el usuario entrega un `{ticket}` de Jira
(HU o Bug). **No aplica** si el usuario pide explícitamente un caso de mantenimiento suelto
sin ticket, o si ya indicó que revisó duplicados manualmente.

## Flujo

### Paso 1 — Buscar TCs ya vinculados al ticket

```bash
.prompts/skill_qa_engineer/.venv/bin/python3 .prompts/skill_qa_engineer/aio_tests_client.py search --title-contains "{ticket}"
```

Esto encuentra todo TC cuyo título siga la convención `[{ticket}]` de
[formatting-rules.md](references/formatting-rules.md) Regla 1. Guarda el resultado como
`{linked_tcs}`.

### Paso 2 — Buscar TCs por afinidad temática (candidate-set adicional)

Si `{linked_tcs}` está vacío o parece incompleto, complementa con una búsqueda por palabras
clave del feature/módulo (ej. `search --title-contains "Login"`), y evalúa manualmente si
alguno de esos resultados ya cubre el mismo escenario aunque no tenga el ticket en el título
(puede ser un caso creado antes de adoptar esta convención).

### Paso 3 — Clasificar cada criterio de aceptación (AC) del ticket

Para cada AC extraído del ticket (`getJiraIssue` vía Atlassian MCP), evalúa cada TC de
`{linked_tcs}` contra los 7 gates de la tabla de arriba (2 deterministas — L, T — y 5 por
juicio de texto — O, D, P, S, C) y decide un nivel:

| Nivel | Condición | Acción |
|---|---|---|
| **REUSE** | Los 7 gates resultan verdes (L y T verificados contra campos reales; O/D/P/S/C por lectura de texto) | No crear nada. Si el TC no tiene el ticket en `labels`, actualízalo (`update_test_case`) para añadirlo. |
| **REFRESCAR** | L y T verdes, pero al menos uno de O/D/P/S/C está desactualizado respecto al AC actual | `update_test_case` sobre el TC existente (nunca crear uno nuevo para el mismo escenario). |
| **CREAR** | L o T fallan (layer/tipo incompatible), o ningún TC candidato cubre el AC | Diseñar y crear un TC nuevo siguiendo [formatting-rules.md](references/formatting-rules.md). |

Sé explícito con el usuario sobre qué se verificó contra un campo real (`Testing Layers`,
`type`) y qué fue juicio del agente sobre texto (los otros 5 factores) — no presentes ambos
tipos de evidencia como si tuvieran el mismo nivel de certeza.

### Paso 4 — Mostrar resumen antes de crear

```
## Triage — {ticket}

| AC | Nivel | TC relacionado | Acción propuesta |
|---|---|---|---|
| AC1 | REUSE | Login - Usuario válida login... [TP-118] | Ninguna |
| AC2 | CREAR | — | Crear TC nuevo |

Total ACs: N | Reusar: X | Refrescar: Y | Crear: Z
```

Pausa aquí y espera confirmación del usuario antes de crear/actualizar nada, salvo que el
usuario haya pedido explícitamente ejecución directa sin revisión previa.

## Restricciones

- **No crear** TCs para ACs clasificados como REUSE o REFRESCAR.
- **No borrar ni archivar** TCs — esa es una operación de mantenimiento manual fuera del
  alcance de esta skill.
- Si `search_test_cases` falla (401/404/500), repórtalo tal cual al usuario y detente — no
  asumas que "no hay duplicados" solo porque la búsqueda falló.

## Limitación conocida (pendiente)

`aio_tests_client.list_test_cases` no soporta filtrar server-side por `labels`/tags ni por
ticket; solo pagina todos los TCs del proyecto. Para proyectos con muchos TCs, la búsqueda
por título (Paso 1) es la única vía eficiente hoy. Si el volumen de TCs crece mucho, considerar
como mejora futura (no incluida en este cambio para no ser invasivos): extender
`aio_tests_client.py` con un filtro client-side por tag, una vez se confirme la forma real del
JSON de respuesta de `list_test_cases` contra el proyecto real.
