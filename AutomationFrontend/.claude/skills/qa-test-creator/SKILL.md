---
name: qa-test-creator
description: >
  Construye la automatización E2E completa (Object Repository, Page, Steps, Script, .tc)
  a partir de un plan Approved por qa-explorer, siguiendo la arquitectura Katalon POM
  3 capas para Android y Web. Ejecuta el runner headless como validación obligatoria
  antes de reportar éxito. Usar cuando el Orquestador entra al paso "3-Crear Tests",
  o cuando el usuario pide "con el plan aprobado de X, crea la automatización".
---

# qa-test-creator — Construcción de la automatización (POM 3 capas)

Skill consumida por el Agente Orquestador único (`qa-orchestrator`) en el paso "3-Crear Tests".

## Bootstrap obligatorio

1. Leer `.claude/skills/katalon-mobile-automation/SKILL.md` como fuente de verdad
   (formato `.rs`, plantillas Page/Steps/Script, POM 3 capas). Si hay conflicto, prevalece
   el SKILL.
2. **Puerta de aprobación (obligatoria):** verificar en el contexto de `qa-flow-planner`
   que `PlanStatus: Approved` y `ApprovedBy: qa-explorer`. Si `Draft` o `Rejected`,
   detener y reportar el bloqueo exacto (formato en `manifest.yaml → approval_gate`).

## Alcance de escritura

Rutas permitidas: `Object Repository/{android,ios,web}/**`, `Keywords/com/tuempresa/page/**`,
`Keywords/com/tuempresa/steps/**`, `Scripts/{android,ios,web}/**`, `Test Cases/{android,ios,web}/**`,
`Test Suites/**` (solo si el usuario lo pide explícitamente).

Rutas prohibidas: `settings/**`, `Include/config/**`, `Profiles/**`, `Drivers/**`,
`Libs/internal/**`, `*.prj`, `build.gradle`, `package.json`, `console.properties`,
`entityReference.index`.

## Reglas duras — no negociables

**Formato `.rs` (R-K1/R-K2/R-K3):** ver `.claude/skills/qa-explorer/references/rs-hard-rules.md`
(copia canónica, no duplicada aquí).

**Compatibilidad Katalon/Groovy (R-K4/R-K5/R-K6/R-K6.1):** ver
`references/groovy-compat-rules.md`. Cubre sintaxis Groovy conservadora, el gate de
validación dual runner+Katalon Studio, y el procedimiento de recuperación cuando una Page
nueva rompe la compilación del editor.

> **Nota de alcance del pipeline autónomo:** el paso "4-Correr Tests" del Orquestador usa
> exclusivamente el runner headless (`runner/run.sh`, multiplataforma Web+Android desde la
> Fase 2 del refactor). El gate R-K5 (Katalon Studio Problems panel = 0 errors) requiere un
> humano frente al IDE gráfico y **no es parte del loop autónomo 4↔5** — sigue documentado
> aquí como buena práctica si el usuario abre el proyecto en Katalon Studio Desktop, pero
> el Orquestador no bloquea ni espera por él. Ver auditoría de la Fase 4 para el
> razonamiento completo de esta decisión.

## Arquitectura POM 3 capas (recordatorio)

- **Page**: `Mobile.*`/`WebUI.*`, `findTestObject()`, `DeviceResolutionPage.scaleX/scaleY`
  (solo Android/coordenadas).
- **Steps**: `@Keyword`, instancia Page, maneja errores con `KeywordUtil`. Nunca `Mobile.*`
  ni `WebUI.*` directo.
- **Script**: solo `CustomKeywords`, `Mobile.startExistingApplication()`/`WebUI.openBrowser()`,
  `Mobile.comment()`/`WebUI.comment()`, lifecycle de cierre.

Utilidades a reutilizar siempre antes de reimplementar: `UtilsPage` (scroll/validación),
`DeviceResolutionPage` (escalado de coordenadas, solo Android), `SmartWaitPage` (waits
estandarizados), `LocatorHelper` (fallback de locators), `ScreenshotPage` (regresión
visual), `VisualLocatorPage` (último recurso, solo Android).

Estándares obligatorios post-creación (Smart Wait compliance, self-healing locators
dirigidos por `tap_validated`, visual baseline capture, LocatorHelper en ruta crítica):
ver `references/post-creation-standards.md`.

## Flujo obligatorio

**Fase 1 — Validar flujo en dispositivo/navegador real** (mismo protocolo de exploración
que `qa-explorer`, pero para confirmar antes de escribir código).

**Fase 2 — Crear archivos** en este orden: `.rs` → Page → Steps → Script → `.tc`.
Bloqueo preventivo: si cualquier `.rs` tiene `locatorStrategy` dentro de `locator`,
`locatorCollection` duplicado, o `locator` como bloque XML — detener y corregir antes de
continuar.

**Regla de path mirroring (crítica, nunca omitir):** `Test Cases/<plataforma>/<subfolder>/<TC>.tc`
→ `Scripts/<plataforma>/<subfolder>/<TC>/Script<timestamp>.groovy`. Usar el run-id del
pipeline como nombre de carpeta del script es un error — siempre el nombre del TC.

**Fase 3 — Runner headless (obligatorio, paso "4-Correr Tests" del Orquestador):**
```bash
bash runner/run.sh run --case <plataforma>/<TC_NAME>
```
Criterio de éxito: `✓ [PASSED ] <TC_NAME>` en el log. Si falla, no reportar completado —
el Orquestador invoca `qa-debugger` (paso "5-Validar" → debug → repetir desde "4").

## Checklist antes de cerrar

- [ ] Path mirroring TC↔Script verificado
- [ ] `PlanStatus: Approved` confirmado antes de crear archivos
- [ ] Separación Page/Steps/Script respetada
- [ ] `.rs` cumple R-K1/R-K2/R-K3
- [ ] Coordenadas (si las hay) escaladas con `DeviceResolutionPage`
- [ ] Cada Page/Steps nuevo cumple sintaxis Groovy conservadora (R-K4)
- [ ] Runner ejecutado y `PASSED`

## Referencias

- `references/groovy-compat-rules.md` — R-K4/R-K5/R-K6/R-K6.1 (compatibilidad Katalon Studio)
- `references/post-creation-standards.md` — Smart Wait, self-healing locators, visual
  baseline, LocatorHelper, VisualLocatorPage
- `.claude/skills/qa-explorer/references/rs-hard-rules.md` — R-K1/R-K2/R-K3 (formato `.rs`)
