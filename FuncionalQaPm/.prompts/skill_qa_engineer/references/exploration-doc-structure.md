# Estructura de Documentación Exploratoria (.md) — extensión de [exploration.md](../exploration.md)

> Define **dónde** y **con qué contenido** se persisten los hallazgos de una sesión de
> exploración manual (web o móvil) cuando el usuario pide documentarla para reutilización
> futura. Es el insumo que luego consume [audit_test_case.md](../audit_test_case.md) (Regla 11
> de [formatting-rules.md](formatting-rules.md)) para robustecer Casos de Prueba con nombres
> reales de módulos/componentes.

## Estructura de carpetas (obligatoria)

```
docs/QaExplorer/
└── {TipoDispositivo}/        # Android | iOS | Web
    └── {AppOPagina}/          # nombre de la app o del sitio (PascalCase, sin espacios)
        └── {Modulo}/          # módulo funcional (ej. Autenticacion, Carrito, Perfil)
            └── {SubModulo}/   # sub-módulo o pantalla concreta (ej. Login, RecuperarPassword)
                └── {SubModulo}.md
```

- **`{TipoDispositivo}`**: usa los mismos valores ya validados en `customFields["Testing
  Layers"]` (Regla 10 de [formatting-rules.md](formatting-rules.md)) que representan
  plataforma con UI explorable: `Android`, `iOS`, `Web`. No uses `Api`/`Data Base`/`Logs` aquí
  — esta estructura es solo para exploración de interfaz.
- **`{AppOPagina}`**: nombre de la app móvil o del sitio/portal web (ej. `MyDemoAppRN`,
  `PortalClientes`). Si el proyecto tiene una sola app/sitio, sigue usándolo igual (evita
  ambigüedad cuando en el futuro se agregue una segunda app).
- **`{Modulo}` / `{SubModulo}`**: en español, Title Case, sin espacios (usa guiones si el
  nombre lo requiere). Deben coincidir, cuando se conozcan, con los nombres reales usados en
  el código/UI — esto es lo que permite que la auditoría de TCs (skill de auditoría) los
  referencie con confianza.
- **Si un módulo no tiene sub-módulos naturales**, repite el nombre del módulo como
  sub-módulo (mismo nivel de profundidad siempre) — ej.
  `Web/PortalClientes/Dashboard/Dashboard/Dashboard.md`. Mantener la profundidad fija permite
  recorrer `docs/QaExplorer/**/*.md` con un patrón predecible.
- **Nunca dupliques un archivo para la misma pantalla**: si ya existe, edítalo (agrega una
  fila en "Historial de cambios", sección 8) en vez de crear uno nuevo con sufijo `_v2`, `_new`, etc.

## Cuándo se crea/actualiza

Se activa **solo si el usuario pide explícitamente documentar** una sesión de exploración para
reutilización futura (igual que hoy en [exploration.md](../exploration.md)) — no reemplaza el
resumen conversacional cuando el usuario solo pide "explora y créame los casos de prueba"
directamente. Regla práctica: si la exploración es lo suficientemente extensa como para cubrir
un módulo/pantalla completo (varios flujos, varios componentes), documentarla es la opción por
defecto a sugerir al usuario — no esperar a que lo pida explícitamente si el hallazgo es
sustancial.

## Plantilla obligatoria del archivo `.md`

```markdown
---
plataforma: Android | iOS | Web
app_o_pagina: "{nombre}"
modulo: "{nombre}"
submodulo: "{nombre}"
ticket_relacionado: "{ticket}" # o "sin_ticket"
explorado_con: mobile-mcp | appium-mcp | playwright-mcp | aisquare-playwright
ultima_actualizacion: "{YYYY-MM-DD}"
---

# {Módulo} / {Sub-módulo} — {Plataforma}

## 1. Objetivo del módulo/pantalla
Qué le permite hacer al usuario esta pantalla/módulo, en 2-3 líneas.

## 2. Funciones principales
- {Función 1}: qué hace, cuándo se usa.
- {Función 2}: ...

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id / testID | Notas |
|---|---|---|---|
| "Ingresar" | Botón | `id=btn_login` | Deshabilitado hasta llenar ambos campos |

## 4. Flujos documentados
### Flujo 1 — {nombre del flujo}
1. {Paso observado}
2. {Paso observado}

Resultado esperado: {resultado real observado}
Mensajes de error/validación (verbatim): "{texto exacto tal como aparece en pantalla}"

## 5. Datos de prueba / valores de frontera observados
- {dato}: {comportamiento observado}

## 6. Evidencia
- Screenshot: `{ruta guardada con mobile_save_screenshot / browser_take_screenshot}`
- Grabación: `{ruta si aplica}`

## 7. Hallazgos abiertos / posibles bugs
- {Observación sospechosa} — sugerido para [bug_report.md](../bug_report.md), pendiente de
  confirmación del usuario (nunca se reporta automáticamente desde aquí).

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| {YYYY-MM-DD} | {contexto} | Creación inicial |
```

### Reglas de contenido

- Todo el contenido en español (igual que Regla 5 de [formatting-rules.md](formatting-rules.md)).
- Nombres de componentes y mensajes de error: **verbatim**, tal como se observaron en la
  captura/snapshot real de la sesión — nunca inventados ni "normalizados".
- La sección 7 (hallazgos abiertos) **nunca** se convierte automáticamente en bug ni en TC —
  solo se sugiere; requiere confirmación explícita del usuario para pasar por
  [bug_report.md](../bug_report.md) o para diseñar un Caso de Prueba.
- Si la exploración fue guiada por un ticket, registra `ticket_relacionado`; si fue
  mantenimiento/exploración libre, usa `"sin_ticket"` (misma convención que Regla 8 de
  [formatting-rules.md](formatting-rules.md)).

## Restricciones

- No crear Casos de Prueba ni reportar bugs desde este flujo — esta convención es puramente
  de documentación (ver Restricciones de [exploration.md](../exploration.md)).
- No inventar `{Modulo}`/`{SubModulo}` que no correspondan a la navegación real observada.
- Antes de crear un archivo nuevo, revisa si ya existe uno para el mismo
  `{TipoDispositivo}/{AppOPagina}/{Modulo}/{SubModulo}` — si existe, edítalo en vez de duplicar.
