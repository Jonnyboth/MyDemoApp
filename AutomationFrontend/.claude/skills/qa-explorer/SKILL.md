---
name: qa-explorer
description: >
  Valida planes Draft de qa-flow-planner contra el dispositivo/navegador real y los
  aprueba o rechaza de forma autónoma (nunca espera al usuario). Una vez Approved,
  mapea pantallas y genera archivos .rs del Object Repository usando UIAutomator adb
  dump (Android) o inspección DOM (Web) como método primario. Usar cuando el
  Orquestador entra al paso "2-Explorar", o cuando el usuario pide "valida este plan",
  "mapea la pantalla X y genera .rs faltantes", "captura los objetos del flujo".
---

# qa-explorer — Validación de plan + mapeo de pantallas

Skill consumida por el Agente Orquestador único (`qa-orchestrator`) en el paso "2-Explorar".
Tiene **dos modos**:

1. **MODO VALIDACIÓN** — plan en `PlanStatus: Draft` → aprueba o rechaza autónomamente.
2. **MODO CAPTURA** — plan en `PlanStatus: Approved` → genera `.rs` con dump real.

**Regla clave**: nunca espera aprobación del usuario — este skill ES quien aprueba/rechaza.

## Bootstrap obligatorio

1. Leer `.claude/skills/katalon-mobile-automation/SKILL.md` (fuente de verdad del formato
   `.rs` y POM 3 capas). Si hay conflicto con este skill, prevalece el SKILL.
2. Leer el archivo de contexto en `.github/orchestrator/runs/<run-id>-<flujo>.md`:
   - `PlanStatus: Draft` → entrar en MODO VALIDACIÓN.
   - `PlanStatus: Approved` → entrar en MODO CAPTURA.
   - Sin archivo → pedir que `qa-flow-planner` genere el plan primero.

## MODO VALIDACIÓN (autónomo)

Protocolo completo (screenshot + dump por paso, marcar ✅/⚠️/❌, guardar dumps para
reutilizarlos en captura) en `references/validation-protocol.md`.

Decisión:
- Todos los pasos ✅ o ⚠️ (con ajuste documentado) → `PlanStatus: Approved`,
  `ApprovedBy: qa-explorer`.
- ≥1 paso ❌ bloqueado → leer `RetryCount`; si `< 3`, `PlanStatus: Rejected` +
  `RejectionNotes` (vuelve a `qa-flow-planner`); si `>= 3`, escalar al usuario
  (ver `manifest.yaml → retry_policy`).

## MODO CAPTURA

Genera `.rs` en `Object Repository/android/<Pantalla>/`, `Object Repository/ios/<Pantalla>/`
o `Object Repository/web/<Pantalla>/` según la plataforma del plan. Reutiliza los dumps ya
guardados por MODO VALIDACIÓN — cero navegaciones adicionales si la validación fue exitosa.

Reglas duras de locators (R-K1/R-K2/R-K3, no negociables) y el protocolo de validación
empírica (tap real + comparación de árbol pre/post) están en `references/rs-hard-rules.md`
y `references/validation-protocol.md`. Toda violación bloquea la captura.

Prioridad de locator: `resource-id`/CSS > `content-desc` > `text`+`class` > XPath
contextual > coordenadas (solo si `clickable="false"` o Compose sin accesibilidad —
Android — o si el elemento Web no tiene selector estable).

## Alcance (guardrail no negociable)

- ✅ Rutas permitidas: `Object Repository/android/**`, `Object Repository/ios/**`,
  `Object Repository/web/**`.
- ❌ No crea/edita `Page`, `Steps`, `Scripts`, `Test Cases` (responsabilidad de
  `qa-test-creator`), ni `settings/**`, `Include/config/**`, `Profiles/**`, `Drivers/**`,
  `Libs/internal/**`, `*.prj`, `build.gradle`, `package.json`.
- Si necesita tocar algo fuera de scope, pedir autorización explícita antes de proceder
  (formato exacto en `manifest.yaml → out_of_scope_protocol`).

## Checklist antes de cerrar

- [ ] Todos los `.rs` nuevos usan `<MobileElementEntity>` (o el equivalente Web, ver
      `katalon-mobile-automation/SKILL.md`)
- [ ] `<locatorStrategy>` al nivel raíz, `<locatorCollection>` con las entradas estándar
- [ ] Sin duplicados por resource-id/CSS o nombre
- [ ] Solo rutas permitidas tocadas
- [ ] Plan actualizado a `Approved`/`Rejected` sin esperar al usuario
- [ ] UIAutomator adb dump usado como método primario (no `mobile_list_elements_on_screen`)
- [ ] Tabla de elementos generados/no mapeados entregada

## Referencias

- `references/validation-protocol.md` — protocolo completo MODO VALIDACIÓN + validación
  empírica de taps + estrategia de captura (decision matrix completa)
- `references/rs-hard-rules.md` — R-K1/R-K2/R-K3 (formato `.rs`, no negociables)
- Detalle exhaustivo de formato `.rs` y protocolo de captura: ya documentado en
  `.claude/skills/katalon-mobile-automation/SKILL.md` — este skill no lo duplica, solo
  añade la lógica de aprobación/rechazo autónoma que es específica de qa-explorer.
