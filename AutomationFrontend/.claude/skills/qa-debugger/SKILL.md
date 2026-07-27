---
name: qa-debugger
description: >
  Diagnostica y corrige fallos de tests Katalon Mobile/Web del proyecto TuEmpresa con
  evidencia real de dispositivo/navegador, aplicando el fix mínimo sin romper la
  arquitectura POM 3 capas. Usar cuando el Orquestador entra al paso "5-Validar" tras
  un runner FAILED, o cuando el usuario reporta "este test falla en X, diagnostica y
  aplica fix mínimo".
---

# qa-debugger — Diagnóstico y corrección con evidencia real

Skill consumida por el Agente Orquestador único (`qa-orchestrator`) en el paso "5-Validar"
cuando el runner falla.

## Bootstrap obligatorio

1. Leer `.claude/skills/katalon-mobile-automation/SKILL.md` como fuente de verdad.

## Alcance de escritura

Puede editar cualquier archivo del proyecto necesario para corregir el error, respetando
la arquitectura 3 capas — el objetivo es siempre el **fix mínimo**, nunca refactor.

Rutas permitidas: `Object Repository/{android,ios,web}/**`, `Keywords/com/tuempresa/page/**`
(incluye `page/common/`), `Keywords/com/tuempresa/steps/**`, `Scripts/{android,ios,web}/**`,
`Test Cases/{android,ios,web}/**`.

Rutas prohibidas: `settings/**`, `Include/config/**`, `Profiles/**`, `Drivers/**`,
`Libs/internal/**`, `*.prj`, `build.gradle`, `package.json`.

## Guardrails no negociables

1. Corregir solo la causa raíz con evidencia (log, screenshot, dump/inspección DOM).
2. Nunca resolver un bug rompiendo la arquitectura Page/Steps/Script.
3. Si el error es de locator, validar en dispositivo/navegador real antes de editar.
4. Si hay coordenadas, verificar `DeviceResolutionPage.scaleX/scaleY` (Android) — nunca
   dejar coordenadas absolutas hardcodeadas.
5. Reglas duras R-K1/R-K2/R-K3 (formato `.rs`) y R-K4/R-K5/R-K6/R-K6.1 (compatibilidad
   Katalon/Groovy) son las mismas que aplica `qa-test-creator` — ver referencias.

## Catálogo de errores comunes

Ver `references/error-catalog.md` para el diagnóstico y fix completo de:
1. `Name is null at MobileLocatorStrategy.valueOf`
2. Element not found / Timeout
3. Violación POM 3 capas
4. Coordenadas absolutas sin escalado

Protocolo de triage completo para fallos de locator (`NoSuchElementException`, timeouts)
también en `references/error-catalog.md → Locator Failure Triage Protocol`.

## Handoff en el pipeline autónomo (paso "5-Validar")

Cuando el Orquestador invoca este skill tras un runner `FAILED`:
1. Diagnosticar causa raíz con el log + screenshot del error.
2. Aplicar fix mínimo (nunca refactor no relacionado).
3. Reportar el fix aplicado al Orquestador (archivo + descripción + regla R-K que aplicó
   si corresponde).
4. El Orquestador repite el paso "4-Correr Tests" con el fix aplicado — máximo 3 ciclos
   debug↔run antes de escalar al usuario (ver `.github/orchestrator/manifest.yaml`).

## Salida obligatoria al cerrar

```text
Diagnóstico completado para <flujo/test>

Causa raíz:
- ...

Archivos modificados:
- ...

Regla aplicada: R-K<N> (si corresponde)

Validaciones ejecutadas:
- Verificación de formato .rs contra el SKILL
- Validación en dispositivo/navegador real
```

## Referencias

- `references/error-catalog.md` — 4 errores documentados + triage protocol + utilidades
  reutilizables para debugging
- `.claude/skills/qa-explorer/references/rs-hard-rules.md` — R-K1/R-K2/R-K3
- `.claude/skills/qa-test-creator/references/groovy-compat-rules.md` — R-K4/R-K5/R-K6/R-K6.1
