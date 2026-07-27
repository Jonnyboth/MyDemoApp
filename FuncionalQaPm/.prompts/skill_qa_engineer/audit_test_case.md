# Auditoría de Casos de Prueba — extensión de `skill_qa_engineer`

> Capacidad **nueva** que garantiza que todo TC creado, editado o ya existente en AIO Tests
> cumple las 11 reglas de [references/formatting-rules.md](references/formatting-rules.md) —
> incluida la Regla 11, que exige consultar la documentación exploratoria (.md) definida en
> [references/exploration-doc-structure.md](references/exploration-doc-structure.md) cuando
> existe para el módulo/página del TC. El objetivo es que los TCs queden más robustos y
> completos: con nombres reales de componentes, rutas de módulo trazables y flujos verificados,
> no solo descripciones genéricas.

Tiene dos modos de activación:

- **Modo A — Consulta obligatoria en creación/edición**: se ejecuta *siempre*, como parte del
  Paso 1 ("Recolección de insumos") del flujo normal de [system.md](system.md), para todo TC
  nuevo o TC existente que se vaya a editar de forma sustancial (pasos, precondición,
  descripción).
- **Modo B — Auditoría explícita**: se ejecuta cuando el usuario pide auditar/revisar TCs ya
  creados (ej. "audita los TCs del ticket TP-118", "revisa si este TC cumple el estándar",
  "audita todos los TCs del módulo Login").

## Modo A — Consulta obligatoria de documentación exploratoria

### Paso 1 — Identificar el módulo/página involucrado

A partir del ticket (`{ticket}` y sus ACs), de la descripción que dio el usuario, o del título
provisional del TC, identifica: `{TipoDispositivo}` (Android/iOS/Web), `{AppOPagina}`,
`{Modulo}` y, si aplica, `{SubModulo}`.

Si el usuario no da suficiente información para identificar el módulo (ej. pide un TC muy
genérico sin contexto de pantalla), pregunta antes de continuar — no adivines la ruta.

### Paso 2 — Buscar documentación exploratoria existente

Busca en el árbol de archivos del repositorio (con las herramientas de sistema de archivos
disponibles — `Glob`/`Grep`, no requiere ninguna MCP) bajo
`docs/QaExplorer/{TipoDispositivo}/{AppOPagina}/**/*.md`:

1. Coincidencia exacta de carpeta (`{Modulo}/{SubModulo}`) → candidato directo.
2. Si no hay coincidencia exacta, busca por nombre aproximado (sin tildes, insensible a
   mayúsculas) en las rutas y en el frontmatter (`modulo:`/`submodulo:`) de los `.md`
   encontrados bajo `{TipoDispositivo}/{AppOPagina}/`.
3. Si hay **más de un candidato razonable**, muéstraselos al usuario y pide cuál aplica (o usa
   el más específico si la coincidencia es evidente y única).
4. Si no hay ningún candidato bajo esa app/página, es válido asumir que no existe
   documentación — no la inventes.

### Paso 3 — Si se encontró un archivo: usarlo obligatoriamente

- Lee el archivo completo.
- Toma de la sección "3. Componentes identificados" los nombres/selectores reales a usar en
  `steps[].step` y `expected_result` (en vez de "el botón de login", usa el nombre/selector
  documentado).
- Toma de la sección "4. Flujos documentados" la secuencia real observada y contrástala contra
  los pasos que ibas a proponer — si difieren, prioriza lo documentado salvo que el `{ticket}`
  tenga un AC explícito que indique un comportamiento distinto (en ese caso, el AC manda, pero
  señala la discrepancia al usuario: puede ser documentación desactualizada, sugiere
  refrescarla con [exploration.md](exploration.md)).
- Agrega en `description` o `precondition` del TC la ruta del módulo documentado, ej.:
  `Módulo: Android/MyDemoApp/Autenticacion/Login`. Esto no reemplaza la trazabilidad de
  Regla 1/8 (título `[{ticket}]` + `labels`) — ambas coexisten.
- Si la sección "7. Hallazgos abiertos" del documento menciona algo sospechoso relevante al TC
  que estás creando, menciónaselo al usuario (sin convertirlo tú mismo en bug — eso requiere
  confirmación explícita, ver [bug_report.md](bug_report.md)).

### Paso 4 — Si NO se encontró ningún archivo

Continúa el flujo normal de creación del TC (no es un bloqueo), pero:

1. Decláraselo explícitamente al usuario: *"No se encontró documentación exploratoria para
   {Modulo}/{SubModulo}. El TC se creará solo con los datos entregados/observados en esta
   conversación."*
2. Si el requerimiento es ambiguo y no hay evidencia suficiente para pasos verificables,
   sugiere ejecutar primero [exploration.md](exploration.md) antes de diseñar el TC (mismo
   criterio que ya aplica el Paso 1 de [system.md](system.md)).
3. Nunca inventes nombres de componentes, selectores ni rutas de módulo "como si" existiera
   documentación — eso rompe la trazabilidad que esta regla busca garantizar.

## Modo B — Auditoría explícita de TCs existentes

### Paso 1 — Resolver el conjunto de TCs a auditar

Según lo que pida el usuario:

- Por `{ticket}`: `search_test_cases(title_contains="{ticket}")`.
- Por ID puntual: `get_test_case(id)`.
- Por módulo/tema: búsqueda por palabra clave del título (`search --title-contains`) o, si el
  usuario da un rango, `list_test_cases` paginado.

Si la búsqueda falla (401/404/500), repórtalo tal cual — no asumas que "no hay TCs" solo
porque la búsqueda falló (mismo criterio que [triage.md](triage.md)).

### Paso 2 — Verificar cada TC contra las 11 reglas

Para cada TC del conjunto, obtén el detalle completo (`get_test_case`) y evalúa cada regla de
[references/formatting-rules.md](references/formatting-rules.md):

| Regla | Qué se verifica |
|---|---|
| 1 | Título con `{Feature} - {Escenario} [{ticket}]` o sin ticket si es exploratorio/mantenimiento |
| 2 | Precondición no implica sesión iniciada salvo login explícito en Paso 1 |
| 3 | Cada `step` es una acción atómica (sin "y luego") |
| 4 | Un solo `expected_result` consolidado por acción |
| 5 | Todo el contenido en español |
| 6 | `test_data` nunca vacío (`"N/A"` si no aplica) |
| 7 | Entre 2 y 18 pasos reales |
| 8 | `labels` incluye `created_by_ai` + ticket o `sin_ticket` |
| 9 | Balance de tipos de escenario a nivel de conjunto (no de un TC aislado) |
| 10 | `customFields` (`Testing Layers`, `Test Type Cycle`) y campos nativos (`type`, `estimatedEffort`) completos y válidos |
| 11 | Si existe doc exploratoria para el módulo del TC, ¿el TC referencia nombres reales de componentes/ruta de módulo, o usa descripciones genéricas? |

Para la Regla 11 específicamente, repite el Paso 2 del Modo A (buscar en `docs/QaExplorer/`)
usando el módulo que se pueda inferir del título/labels/customFields del TC auditado.

Marca cada regla como:
- ✅ Cumple (con la evidencia puntual: qué campo, qué valor).
- ❌ No cumple (con el detalle exacto de qué falta o está mal).
- ⚠️ Sin evidencia suficiente para verificar (ej. no se encontró doc exploratoria para
  confirmar/descartar Regla 11, o el campo no vino en la respuesta de `get_test_case`) — nunca
  marques ⚠️ como ✅.

### Paso 3 — Presentar el resumen de auditoría

```
## Auditoría de TCs — {alcance: ticket/módulo/ID}

| TC | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | Veredicto |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TP-TC-6 | ✅ | ✅ | ❌ (paso 2 no atómico) | ✅ | ✅ | ✅ | ✅ | ✅ | — | ✅ | ⚠️ sin doc | Requiere ajuste |

Total TCs auditados: N | Conformes: X | Requieren ajuste: Y | Sin evidencia suficiente: Z
```

Pausa aquí y espera confirmación del usuario antes de modificar nada — salvo que el usuario
haya pedido explícitamente aplicar las correcciones sin revisión previa.

### Paso 4 — Aplicar correcciones (si el usuario aprueba)

Usa `update_test_case` solo con los campos que cambian (mismo criterio que el Paso 4 del flujo
normal en [system.md](system.md)). Nunca elimines `created_by_ai` ni el ticket de `labels` al
corregir otros campos. Una operación = una llamada a la API por TC corregido.

## Restricciones

- Modo A nunca bloquea la creación de un TC por falta de documentación exploratoria — solo la
  usa cuando existe y lo declara cuando no existe.
- Modo B **no crea TCs nuevos** — solo señala no conformidades sobre TCs existentes y, si se
  aprueba, los actualiza. Si un AC no tiene ningún TC que lo cubra, eso es un caso de
  [triage.md](triage.md) (nivel CREAR), no de esta auditoría.
- No inventes cumplimiento de una regla que no se pudo verificar — repórtala como ⚠️, nunca
  como ✅.
- No inventes rutas de módulo, nombres de componentes ni contenido de un `.md` exploratorio
  que no exista — si no lo encuentras, dilo explícitamente.

## Referencias

- [references/formatting-rules.md](references/formatting-rules.md) — las 11 reglas auditadas.
- [references/exploration-doc-structure.md](references/exploration-doc-structure.md) —
  estructura y contenido de la documentación exploratoria que audita la Regla 11.
- [exploration.md](exploration.md) — cómo generar documentación exploratoria cuando no existe.
- [triage.md](triage.md) — anti-duplicados; no confundir con esta auditoría (triage decide
  REUSE/REFRESCAR/CREAR contra ACs de un ticket, esta auditoría verifica calidad de formato).
- [test_spec.md](test_spec.md) — estructura JSON de un TC.
- [tools.md](tools.md) — `get_test_case`, `search_test_cases`, `update_test_case`.
