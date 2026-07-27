---
name: qa-flow-planner
description: >
  Planifica pruebas automatizadas Katalon Mobile/Web para TuEmpresa ANTES de escribir código.
  Explora el flujo en dispositivo real (o navegador), identifica el punto de entrada óptimo
  reutilizando test cases ya existentes (openApp, OpenStoreGeant, UtilsPage), valida
  precondiciones y produce un plan accionable (PlanStatus: Draft) para que qa-explorer lo
  valide y apruebe. Usar cuando el Orquestador entra al paso "1-Planificar", o cuando el
  usuario pide "planifica el caso", "explora el flujo y arma el plan", "qué test cases
  existen que pueda reutilizar".
---

# qa-flow-planner — Planificación de pruebas antes de automatizar

Skill consumida por el Agente Orquestador único (`qa-orchestrator`) en el paso "1-Planificar".

## Rol

Explorar el flujo, decidir la estrategia óptima de automatización (incluyendo reuso de
infraestructura existente) y entregar un plan escrito — **nunca código ni `.rs`**.

## Bootstrap obligatorio (siempre, en este orden)

1. Verificar conectividad del dispositivo/navegador (ver "Fase 0" abajo).
2. Leer `.claude/skills/katalon-mobile-automation/SKILL.md` — fuente de verdad del formato
   `.rs`, POM 3 capas y convenciones. Si hay conflicto entre este skill y ese, **prevalece el SKILL**.
3. Escanear test cases reutilizables (ver `references/reusable-testcases.md`).

## Alcance (guardrail no negociable)

**No automatiza y no genera Page/Steps/Script/.tc/.rs.** Su único artefacto de escritura es
el archivo de contexto de plan.

- ✅ Puede crear archivos nuevos en: `.github/orchestrator/runs/<run-id>-<flujo>.md` (ruta
  nueva del orquestador; ver Fase 6). Nunca sobrescribe contexto existente.
- ❌ Rutas prohibidas: `settings/**`, `Include/config/**`, `Profiles/**`, `Drivers/**`,
  `Libs/internal/**`, `*.prj`, `build.gradle`, `package.json`.

## Fase 0 — Verificación de entorno (obligatoria, primero)

**Android** (mobile-mcp): `mobile_list_available_devices()` → si vacío, detener y pedir
dispositivo/emulador. Luego `mobile_take_screenshot()` y `adb devices` (más rápido que
`mobile_list_elements_on_screen`, nunca usarlo para el health check).

**Web** (desde Fase 2 del runner): confirmar que hay un navegador disponible para
Selenium Manager (`google-chrome --version` o equivalente) y que la URL base del flujo
es alcanzable.

## Fase 1 — Catálogo de reuso (obligatorio antes de proponer pasos)

Ver tabla completa en `references/reusable-testcases.md`. Regla de decisión: si el flujo
empieza en Home → reutilizar `openApp`; si empieza dentro de Geant → `OpenStoreGeant`;
si empieza en otra sección no cubierta → proponer un nuevo setUp para que `qa-test-creator`
lo cree. Nunca reimplementar scroll/validación — usar `UtilsPage`.

## Fase 2 — Exploración en dispositivo/navegador real

Por cada paso: screenshot antes → capturar elementos (UIAutomator dump en Android,
inspección DOM en Web) → interactuar → screenshot después. Registrar bloqueos, modales,
bifurcaciones. Capturar `class/text/resource-id (o CSS)/content-desc/bounds` sin crear
el `.rs` todavía.

## Fase 3 — Plan de automatización (salida obligatoria)

Estructura completa y plantilla exacta del archivo de contexto en
`references/plan-template.md`. Debe incluir: objetivo, punto de entrada, precondiciones,
datos de prueba, pasos validados, componentes exploratorios, riesgos, criterios de
aceptación, handoff técnico, compatibilidad multi-dispositivo (o multi-navegador),
anotaciones de Smart Wait por paso.

## Fase 4 — Persistencia y traspaso

**Regla crítica de `RetryCount` (no negociable):** este skill **nunca** decide el valor
de `RetryCount` — solo lo lee. La única skill autorizada a incrementarlo es `qa-explorer`
(ver `manifest.yaml → retry_policy → incremented_by: qa-explorer`).

- **Creación inicial del run** (no existe archivo de contexto todavía): crear
  `.github/orchestrator/runs/<run-id>-<flujo>.md` con `PlanStatus: Draft` y
  `RetryCount: 0`.
- **Revisión tras rechazo** (el archivo ya existe con `PlanStatus: Rejected` y
  `RetryCount: N`): **preservar el `RetryCount: N` tal cual** al reescribir el plan con
  `PlanStatus: Draft` — nunca resetearlo a 0. Resetearlo rompe el límite de 3 intentos y
  puede producir un ciclo Planificar↔Explorar sin fin.

**No esperar aprobación del usuario** — `qa-explorer` valida y aprueba/rechaza de forma
autónoma. Límite de reintentos: máximo 3 ciclos Planificar→Explorar antes de escalar al
usuario (ver manifest.yaml → retry_policy).

## Checklist antes de cerrar

- [ ] Entorno verificado (dispositivo Android o navegador Web disponible)
- [ ] Skill `katalon-mobile-automation` leído
- [ ] Catálogo de reuso escaneado y punto de entrada decidido
- [ ] Plan completo con las 11 secciones de `references/plan-template.md`
- [ ] `PlanStatus: Draft` establecido — sin auto-aprobar
- [ ] `RetryCount`: `0` si es creación inicial, o **preservado sin cambios** si es una
      revisión tras rechazo (nunca reseteado)
- [ ] Ruta exacta del archivo de contexto reportada al Orquestador

## Referencias

- `references/reusable-testcases.md` — catálogo de setUps y utilidades reutilizables
- `references/plan-template.md` — plantilla completa del plan + Smart Wait annotations
- Spec estructurada: `manifest.yaml` (este mismo directorio)
