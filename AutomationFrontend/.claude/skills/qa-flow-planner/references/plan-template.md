# Plantilla del plan de automatización

> Extraída sin cambios de contenido desde `BMO-FlowPlanner.agent.md` — Fase 5 y Fase 6.

## Secciones obligatorias del plan (salida al usuario/orquestador)

1. **Objetivo del caso**
2. **Punto de entrada (setUp)** — TC reutilizado (nombre + ruta) o nuevo setUp necesario
3. **Precondiciones**
4. **Datos de prueba**
5. **Pasos funcionales validados en dispositivo/navegador** (desde el punto de entrada)
6. **Componentes exploratorios capturados** (nombre `.rs` sugerido + estrategia locator)
7. **Riesgos y mitigaciones**
8. **Cobertura mínima recomendada**
9. **Criterios de aceptación**
10. **Handoff técnico** — qué reutilizar (CustomKeywords existentes) y qué crear nuevo
11. **Compatibilidad multi-dispositivo/multi-navegador** — perfiles objetivo, riesgos de
    variación UI, estado de pasos con coordenadas y plan de eliminación

## Archivo de contexto persistido

Ruta: `.github/orchestrator/runs/<run-id>-<flujo>.md`

```markdown
# Flow Context - <run-id> - <flujo>

Fecha:
Plataforma: android | ios | web
PlanStatus: Draft
RetryCount: 0
ApprovedBy:
ApprovalDate:
ApprovalNotes:
RejectionNotes:
DispositivoExplorado: <deviceId, o "N/A (web)">
ResolucionExplorada: <ancho>x<alto> px, o resolución de ventana del navegador

## Punto de entrada (setUp)
- TC reutilizado: <nombre o "nuevo needed">
- Motivo: <por qué se eligió ese punto>

## Objetivo
- ...

## Precondiciones
- ...

## Pasos validados en dispositivo/navegador
1. ...

## Componentes capturados (sin registrar .rs)
Nota: si el componente usa coordenadas, expresar bounds en la resolución explorada.
qa-explorer calculará base_x/base_y en 1080×2340 durante la validación empírica (solo Android).

| Paso | pantalla | class | text | identifier (resource-id/CSS) | label/content-desc | bounds | .rs sugerido | locator preferido | locator respaldo |
|------|----------|-------|------|-------------------------------|---------------------|--------|---------------|--------------------|--------------------|

## Componentes validados empíricamente
*(qa-explorer poblará esta sección durante MODO CAPTURA)*

| .rs sugerido | resource-id/CSS | content-desc | bounds reales | base_x (1080) | base_y (2340) | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|---|---|

## Riesgos y bifurcaciones
- ...

## Instrucciones para qa-test-creator
- setUp: ...
- Keywords a reutilizar: ...
- Nuevos a crear: ...

## Instrucciones para qa-explorer
- ...
```

## Smart Wait annotations (obligatorio por paso)

Todo paso que implique navegación, tap o cambio de estado debe incluir:

```
Step N: <descripción de la acción>
  → Pre-tap Wait: SmartWaitPage.waitVisible(<elemento>, SmartWaitPage.<CONSTANTE>)
  → Post-tap Wait: SmartWaitPage.waitVisible(<indicador_siguiente_pantalla>, SmartWaitPage.<CONSTANTE>)
  → Wait Constant: SHORT (5s) | MEDIUM (15s) | LONG (30s)
  → Rationale: <por qué se eligió este timeout>
```

| Escenario | Constante | Segundos | Cuándo usar |
|---|---|---|---|
| Elemento Compose ya en el DOM | `SHORT` | 5 | Botones/labels que renderizan inmediato |
| Pantalla que requiere llamada de red | `MEDIUM` | 15 | Landing de tienda, actualización de carrito, resultados de búsqueda |
| Procesamiento de pago/orden | `LONG` | 30 | Confirmación de checkout, inicio de order tracking |
| Buffer de animación (solo tap) | `floorPause` | 1 | Después de un tap, antes del siguiente wait — sin spinner |

Reglas: nunca emitir un paso sin anotación de wait; si no se conoce el elemento correcto,
marcar `⚠️ WAIT_UNKNOWN` y explicar por qué; desaparición de spinner →
`SmartWaitPage.waitGone(spinner, SmartWaitPage.MEDIUM)`; taps en loop (contador) →
`SmartWaitPage.tapPause()` entre taps.

## Anotaciones adicionales por componente

- **LocatorHelper** (riesgo alto): marcar elementos susceptibles a cambiar con updates de la
  app (banners promocionales, precios dinámicos, botones de checkout) para que
  `qa-test-creator` use `LocatorHelper.findWithFallback()`.
- **ScreenshotPage** (checkpoints visuales): identificar 1–3 pantallas críticas por flujo
  para `ScreenshotPage.captureAndCompare()`.
- **VisualLocatorPage** (elementos promocionales): marcar `VISUAL_ONLY: true` + label sugerido
  para elementos sin atributos UIAutomator/DOM estables.
