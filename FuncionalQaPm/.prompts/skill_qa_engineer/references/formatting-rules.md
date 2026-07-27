# Reglas de Formato y Calidad — Casos de Prueba (AIO Tests)

> Adaptado del estándar de diseño de TCs usado en otros proyectos del equipo QA, ajustado
> al esquema JSON "amigable" que consume `aio_tests_client.py` (ver [test_spec.md](../test_spec.md)).
> Aplica estas reglas **antes** de llamar a `create_test_case` / `update_test_case`.

## Regla 1 — Título con trazabilidad al ticket

Formato:

- Con `{ticket}` (HU/Bug de Jira relacionado): **`{Feature} - {Escenario concreto} [{ticket}]`**
- Sin `{ticket}` (caso exploratorio o de mantenimiento libre): **`{Feature} - {Escenario concreto}`**

```
OK:   "Login - Usuario válida login con credenciales correctas [TP-118]"
OK:   "Recordatorio de vacunas - No se reenvía si ya fue marcada como aplicada [TP-204]"
MAL:  "Validar login"                              <- no describe el escenario
MAL:  "TP-118 login test"                           <- ticket al inicio, sin estructura
```

El sufijo `[{ticket}]` es lo que permite luego buscar los TCs ya vinculados a un ticket con
`search_test_cases(title_contains="{ticket}")` (ver [triage.md](../triage.md)) — AIO Tests
no expone filtrado nativo por ticket en el cliente actual, así que el título es el mecanismo
de trazabilidad más simple y ya soportado sin tocar código.

> **Condición obligatoria — no opcional**: si el usuario entrega un `{ticket}` (HU o Bug de
> Jira, de `TP` o de cualquier otro proyecto), el `key` debe ir **siempre** en el título
> (Regla 1) y en `labels` (Regla 8) — nunca se omite "para simplificar". Solo se usa la forma
> "sin ticket" (`sin_ticket` en labels, sin sufijo en título) cuando el TC es genuinamente
> **exploratorio o de mantenimiento libre sin HU/guion de referencia** (ej. exploración manual
> en un sistema distinto a `TP` que no tiene ticket abierto todavía). No usar `jiraRequirementIDs`
> como sustituto de esta trazabilidad: se probó en vivo (2026-07-08) y la API de AIO Tests no
> persiste ese campo vía `update_test_case`/PUT (ver tabla de pendientes en [tools.md](../tools.md)).

## Regla 2 — Precondición: estado previo, nunca "usuario ya logueado" implícito

La `precondition` describe el estado **anterior** al primer paso. Si el caso requiere sesión
iniciada, el login debe aparecer como **Paso 1** explícito (o la precondición debe decir
explícitamente "Usuario ya autenticado" solo cuando el login fue cubierto por un TC previo
de la misma cadena).

```
OK:   precondition: "Usuario 'qa_user@ejemplo.com' existe y está activo en base de datos."
      Paso 1: step: "Ingresar usuario y contraseña válidos y presionar 'Ingresar'"
              expected_result: "El sistema redirige al dashboard principal"

MAL:  precondition: "Usuario logueado en la app"
      Paso 1: "Navegar a Mi Perfil"   <- el login nunca se ejecutó, quedó asumido
```

Si el flujo depende de datos previos (ej. una mascota ya registrada, una vacuna programada),
descríbelo como estado ya existente, nunca como comando a ejecutar:

```
OK:   "La mascota 'Firulais' tiene una vacuna programada para dentro de 7 días."
MAL:  "Ejecutar INSERT INTO vaccines ... "   <- una precondición no es un comando
```

## Regla 3 — Un paso = una acción atómica

Si puedes insertar "y luego" entre dos acciones de un mismo `step`, son dos pasos distintos.

```
MAL:  step: "Ingresar nombre de la mascota, seleccionar especie y presionar Guardar"
OK:   Paso 1: step: "Ingresar el nombre de la mascota"
      Paso 2: step: "Seleccionar la especie en el desplegable"
      Paso 3: step: "Presionar el botón 'Guardar'"
```

**Excepción**: iniciar sesión (usuario + contraseña + tap "Ingresar") cuenta como **un solo
paso convencional**, porque se entiende universalmente como "autenticarse".

## Regla 4 — Una acción, todas sus aserciones en el mismo `expected_result`

No crees pasos adicionales solo para "verificar" algo que ya ocurrió en el paso anterior;
consolida todos los resultados observables de una acción en su `expected_result`.

```
MAL:  Paso 1: step: "Presionar 'Confirmar cita'" -> expected_result: "Pantalla de confirmación visible"
      Paso 2: step: "Verificar fecha de la cita"  -> expected_result: "Fecha correcta"
      Paso 3: step: "Verificar veterinario asignado" -> expected_result: "Veterinario correcto"

OK:   Paso 1: step: "Presionar 'Confirmar cita'"
      expected_result: "Pantalla de confirmación visible con fecha correcta, veterinario
                         asignado y botón 'Ver detalle' habilitado"
```

## Regla 5 — Español obligatorio en todo el contenido

`title`, `description`, `precondition`, `steps[].step`, `steps[].test_data` y
`steps[].expected_result` **siempre** en español, sin importar el idioma del ticket de origen.

## Regla 6 — `test_data` nunca vacío

Cada paso incluye `test_data`. Si el paso no requiere datos concretos, usa el string `"N/A"`
(nunca dejar el campo vacío `""` ni omitirlo).

## Regla 7 — Límite de pasos: mínimo 2, máximo 18

Ningún caso puede tener un solo paso. Si un flujo natural supera los 18 pasos, divídelo en
varios TCs encadenados (la `precondition` del segundo TC referencia la finalización del
primero). Nunca rellenes con pasos ficticios solo para llegar al mínimo.

## Regla 8 — Etiquetas (`labels`) obligatorias para trazabilidad

- Con `{ticket}`: `["created_by_ai", "{ticket}"]`
- Sin `{ticket}` (mantenimiento/exploratorio): `["created_by_ai", "sin_ticket"]`

`aio_tests_client.py` ya resuelve estos nombres a tags reales vía `POST /tag` (función
`_resolve_tags`) — no requiere ningún paso manual adicional. Nunca omitas `labels` para
"simplificar" un caso: es el único mecanismo de trazabilidad y de detección de duplicados
disponible hoy en AIO Tests desde este cliente.

## Regla 9 — Balance de tipos de escenario

Al diseñar un conjunto de TCs para una funcionalidad, cubre como mínimo:

| Tipo | Cuándo incluirlo |
|---|---|
| Positivo (happy path) | Siempre — al menos 1 por flujo principal |
| Negativo | Siempre que exista una validación de negocio o de formato |
| Borde | Cuando el AC menciona límites, cantidades o formatos específicos |
| E2E / Journey | Cuando el AC describe un flujo punta a punta con múltiples pantallas |

No generes solo casos positivos: un conjunto de TCs sin casos negativos/borde se considera
incompleto y debe señalarse en el resumen final al usuario.

## Regla 10 — Campos nativos y `customFields` confirmados para Test Cases en `TP` (AIO Tests)

> **Verificado en vivo** vía la herramienta MCP `get_test_case_schema` (`aio-tests-mcp`,
> fuente autoritativa — más completa que consultar el REST a mano) el 2026-07-08. Corrige una
> versión anterior de esta regla que descartaba por error `Tipo`/`Esfuerzo Estimado`: **sí
> existen**, pero como campos **nativos** de AIO (no `customFields`).

### Campos nativos relevantes para el mapeo pedido originalmente

| Nombre "amigable" pedido | Campo real en AIO | Tipo | Valores permitidos | Obligatorio |
|---|---|---|---|---|
| Estado (implícito) | `status` | nativo, SINGLE_SELECT | `Draft` / `Ready` / `Under Review` / `Refactor` / `Deprecated` | **Sí** — `create_test_case` lo defaultea a `Draft` si no se especifica; `update_test_case` nunca lo toca a menos que se pida explícitamente |
| TC Type (lo más cercano a "Tipos de Caso") | `type` (se muestra como **"Tipo"** en la UI de Jira) | nativo, SINGLE_SELECT | `Component` / `Integration` / `E2E` / `API` / `Performance` / `Security` / `Carga / Estrés` | No |
| Estimated Time Range (lo más cercano a "Esfuerzo Estimado") | `estimatedEffort` (se muestra como **"Esfuerzo Estimado"** en la UI de Jira) | nativo, entero en **segundos** (no un dropdown de rangos como en Zephyr) | Cualquier entero ≥ 0 | **Sí** (política de esta skill — ver nota abajo; AIO Tests en sí no lo exige) |

> **`estimatedEffort` es obligatorio en la práctica de esta skill**, aunque AIO Tests no lo
> rechace si falta: un TC sin esfuerzo estimado no permite a un humano planear cuánto tardará
> en ejecutarlo manualmente. Estímalo en segundos según la complejidad real de los pasos —
> nunca lo dejes en `null`/omitido:
> - 1-2 pasos simples (ej. validación de campo, mensaje de error): `60`-`90`
> - 3-6 pasos con datos a completar: `120`-`180`
> - Flujos E2E multi-pantalla o que requieren precondiciones complejas de verificar: `180`-`300`
> - Si el flujo natural se acerca al límite de 18 pasos (Regla 7), considera si no debería
>   dividirse en varios TCs antes de estimarlo como uno solo muy largo.
>
> No confundir con `jiraRequirementIDs`: ese campo no persiste vía `update_test_case` (ver
> [tools.md](../tools.md)) y no debe usarse; `estimatedEffort` sí persiste normalmente vía
> `create_test_case`/`update_test_case` (confirmado en vivo el 2026-07-08).

### Custom fields reales (`customFields`) — solo 2, ambos obligatorios

| Nombre real del campo en AIO | `fieldId` interno | Tipo | Valores permitidos | Obligatorio |
|---|---|---|---|---|
| `Testing Layers` | `2` | SINGLE_SELECT_LIST | `Web`(4) / `Api`(5) / `Data Base`(6) / `Logs`(7) / `iOS`(8) / `Android`(9) | Sí |
| `Test Type Cycle` | `1` | SINGLE_SELECT_LIST | `Regresion`(1) / `Smoke`(2) / `N/A`(3) | Sí |

**No existen como `customFields`** (nunca los envíes dentro de `customFields`, aunque sí
existen como campos nativos con otro nombre — ver tabla de arriba): `Tipo`/`type` no es un
customField, es nativo. Y **no existe en absoluto**, ni nativo ni custom, ningún campo llamado
literalmente `"Tipos de Caso"`.

Ejemplo de payload completo para `create_test_case` (formato amigable — `aio_tests_client.py`
traduce automáticamente):

```json
{
  "title": "Login - Usuario válida login con credenciales correctas [TP-118]",
  "description": "...",
  "precondition": "...",
  "priority": "High",
  "type": "E2E",
  "estimatedEffort": 300,
  "labels": ["created_by_ai", "TP-118"],
  "customFields": {
    "Testing Layers": "Android",
    "Test Type Cycle": "Smoke"
  },
  "steps": [ ... ]
}
```

`status` no hace falta incluirlo explícitamente al crear (se defaultea a `"Draft"`), pero sí
si se quiere otro valor inicial (ej. `"Ready"`).

**`Testing Layers` y `Test Type Cycle` son obligatorios** — nunca los omitas al crear un TC.
Si el valor no coincide exactamente con una opción permitida (ej. `"Móvil"` en vez de
`"Android"`), `aio_tests_client.py` lanza `AioTestsConfigError` **antes** de llamar a la API,
listando los valores válidos — nunca lo adivines ni lo dejes pasar silenciado.

Si en el futuro cambia la configuración del proyecto, no inventes campos nuevos: vuelve a
consultar `get_test_case_schema` (sección `fields`) y actualiza esta tabla.

## Regla 11 — Consulta obligatoria de documentación exploratoria (.md) cuando exista

Antes de crear un TC nuevo (o de editar uno existente para robustecerlo), **es obligatorio**
buscar si ya existe documentación exploratoria bajo `docs/QaExplorer/` (estructura y contenido
definidos en [exploration-doc-structure.md](exploration-doc-structure.md)) para el
`{TipoDispositivo}/{AppOPagina}/{Modulo}/{SubModulo}` que corresponde al TC:

- **Si existe** un archivo `.md` que coincide con el módulo/página del TC: léelo completo y
  úsalo para escribir pasos y resultados esperados con **nombres reales de componentes**
  (sección "Componentes identificados") y **flujos reales observados** (sección "Flujos
  documentados"), en vez de descripciones genéricas. Anota en `description` o `precondition`
  del TC la ruta del módulo documentado, ej.:
  `Módulo: Android/MyDemoApp/Autenticacion/Login`.
  Si el TC tiene `{ticket}` con ACs explícitos que contradicen lo observado en la exploración,
  el AC manda (igual que en [exploration.md](../exploration.md)), pero señala la discrepancia
  al usuario — puede indicar documentación desactualizada.
- **Si no existe** ningún archivo para ese módulo/página: continúa el flujo normal sin
  bloquear la creación, pero decláraselo explícitamente al usuario y sugiere ejecutar primero
  [exploration.md](../exploration.md) si el requerimiento es ambiguo. Nunca inventes rutas de
  módulo, nombres de componentes ni flujos "como si" existiera documentación.

```
OK:   step: "Presionar el botón 'Ingresar' (id=btn_login)"
      expected_result: "Se muestra el mensaje 'Usuario o contraseña incorrectos' (verbatim,
                         documentado en Android/MyDemoApp/Autenticacion/Login.md)"
MAL:  step: "Presionar el botón de login"   <- genérico, ignora el nombre/id real ya documentado
```

Detalle del flujo de verificación (búsqueda, criterios de coincidencia, manejo de ambigüedad
entre varios candidatos) en [audit_test_case.md](../audit_test_case.md) — esta skill también
audita el cumplimiento de esta regla (y de las 10 anteriores) sobre TCs ya existentes.

## Checklist previo a crear/actualizar un TC

- [ ] Título sigue Regla 1 (con o sin `[{ticket}]` según corresponda)
- [ ] Precondición no implica sesión iniciada salvo que un TC previo la cubra explícitamente
- [ ] Cada paso es atómico (Regla 3) — sin conectores "y", "luego" entre acciones distintas
- [ ] Un solo `expected_result` consolidado por acción (Regla 4)
- [ ] Todo el contenido en español (Regla 5)
- [ ] `test_data` nunca vacío — usa `"N/A"` si no aplica (Regla 6)
- [ ] Entre 2 y 18 pasos reales (Regla 7)
- [ ] `labels` incluye `created_by_ai` + ticket o `sin_ticket` (Regla 8)
- [ ] `customFields` incluye siempre `Testing Layers` y `Test Type Cycle` (ambos obligatorios) con un valor de la lista permitida (Regla 10)
- [ ] `type` ("Tipo") completado cuando aplique — es campo nativo, no `customField` (Regla 10)
- [ ] `estimatedEffort` ("Esfuerzo Estimado") siempre completado en segundos, nunca `null`/omitido (Regla 10 — obligatorio por política de esta skill)
- [ ] Antes de crear, se ejecutó `search_test_cases` para descartar duplicados (ver [triage.md](../triage.md))
- [ ] Se buscó documentación exploratoria (`docs/QaExplorer/`) para el módulo/página del TC; si existe, se usaron sus nombres reales de componentes/flujos, y si no existe, se declaró explícitamente al usuario (Regla 11)
