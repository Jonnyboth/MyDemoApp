# Protocolo de validación de plan + captura empírica

> Extraído sin cambios de lógica desde `BMO-Explorer.agent.md`.

## MODO VALIDACIÓN — protocolo paso a paso

**Paso 1 — Leer el plan.** Extraer objetivo, precondiciones, pasos, componentes sugeridos
del archivo de contexto (`PlanStatus: Draft`).

**Paso 2 — Verificar en dispositivo real + capturar dump simultáneamente.** Por cada paso
del plan: navegar (mobile-mcp o Selenium según plataforma) → screenshot → dump/inspección
DOM (**guardar el resultado** — esto elimina una pasada completa por las mismas pantallas
en MODO CAPTURA) → buscar elementos clave → marcar:
- ✅ ejecutable — elemento encontrado con locator estable
- ⚠️ ajustable — elemento existe pero con locator diferente (documentar ajuste)
- ❌ bloqueado — elemento no existe o pantalla inaccesible

**Paso 3 — Decisión de aprobación.**
```
Si todos los pasos son ✅ o ⚠️ (con ajuste documentado):
  → PlanStatus: Approved, ApprovedBy: qa-explorer, ApprovalDate: <hoy>
  → dumps_capturados: lista de dumps ya disponibles (reutilizar en MODO CAPTURA)

Si hay ≥1 paso ❌ bloqueado sin workaround claro:
  → Leer RetryCount (default 0).
  → Si RetryCount >= 3: escalar al usuario, NO reiniciar automáticamente.
  → Si RetryCount < 3: PlanStatus: Rejected, RetryCount+1, RejectionNotes detalladas.
```

## Regla de eficiencia — reutilizar dumps de validación

Antes de cualquier dump nuevo en MODO CAPTURA: leer `dumps_capturados` del contexto; para
cada pantalla que ya tiene dump disponible, usar ese XML directamente. Solo hacer dump
nuevo para pantallas no cubiertas en la validación. Si la validación fue 100% exitosa,
el resultado ideal es **cero navegaciones adicionales** al dispositivo.

## Protocolo de validación empírica (canal lateral, Android)

> Propósito: confirmar que el elemento no solo *existe* en el árbol XML, sino que es
> **realmente interactivo** (produce cambio de pantalla al tapearlo). Solo aplica a
> elementos con `clickable="true"`. Los `clickable="false"` se marcan automáticamente
> `tap_validated: false`.

**Paso A — Screenshot pre-tap.** `mobile_take_screenshot()` + `adb shell wm size` (anotar
resolución real).

**Paso B — Calcular centro desde bounds.** `bounds="[x1,y1][x2,y2]"` → `centro_x=(x1+x2)/2`,
`centro_y=(y1+y2)/2` (píxeles reales) → convertir a base 1080×2340:
`base_x = round(centro_x * 1080 / device_width)`, análogo para `base_y`.

**Paso C — Tap empírico.** `mobile_click_on_screen_at_coordinates(device, centro_x, centro_y)`
→ esperar 1.5s → `mobile_take_screenshot()`.

**Paso D — Evaluar cambio de pantalla.** Nuevo `uiautomator dump` → comparar raíz del árbol
XML pre/post: árbol cambió → `tap_validated: true` ✅; árbol idéntico → `tap_validated: false`
❌ (decorativo o Compose sin accesibilidad).

**Paso E — Retroceder.** `mobile_press_button(device, "back")` + screenshot de confirmación.

**Elementos Compose (`clickable=false` pero visualmente interactivos):** registrar
`tap_validated: COMPOSE` — usar coordenadas base escaladas con `DeviceResolutionPage` en
el Page class; documentar elemento objetivo y razón.

**Tabla de registro (formato enriquecido)** — guardar en el contexto bajo
`## Componentes validados empíricamente`:

| .rs sugerido | resource-id | content-desc | bounds reales | base_x (1080) | base_y (2340) | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|---|---|

## Estrategia de captura — prioridad de herramientas

1. **PRIMARIO — UIAutomator dump (Android, 1-2s):**
   ```bash
   adb -s <deviceId> shell uiautomator dump /sdcard/uidump.xml
   adb -s <deviceId> pull /sdcard/uidump.xml /tmp/uidump_<pantalla>.xml
   ```
   Extraer `content-desc` → ACCESSIBILITY; `resource-id` → ANDROID_UI_AUTOMATOR
   (`new UiSelector().resourceId("...")`); XPath relativo → ATTRIBUTES. Nunca detenerse
   en 1 sola estrategia.
2. **VALIDACIÓN VISUAL — ScreenshotPage** antes de cada dump, para confirmar pantalla
   correcta y detectar regresiones futuras.
3. **NAVEGACIÓN — mobile-mcp/Selenium**, solo para llegar a la pantalla (nunca para
   capturar elementos).
4. **RUNTIME FALLBACK — LocatorHelper**, para elementos en ruta crítica (carrito,
   checkout, pago, order tracking) generados en los Page classes.
5. **VISUAL AI LOCATOR — VisualLocatorPage**, último recurso cuando todas las estrategias
   XML fallan (`test-ai-classifier` v4.0.2, backend CPU).

### Matriz de decisión

| Escenario | Herramienta |
|---|---|
| Capturar elementos de pantalla nueva | UIAutomator dump (Android) / inspección DOM (Web) |
| Verificar pantalla correcta antes de capturar | `ScreenshotPage.captureAndCompare()` |
| Navegar a la siguiente pantalla | mobile-mcp / Selenium (tap/scroll únicamente) |
| Elemento cambia de ID en runtime | `LocatorHelper.findWithFallback()` |
| Elemento sin atributos accesibles | `VisualLocatorPage.findByVisual()` (solo Android) |
| Regresión: layout cambió | Diff de `ScreenshotPage` |

## Convención de nombres `.rs`

| Prefijo | Tipo | Ejemplos |
|---|---|---|
| `btn_` | Botón / tappable | `btn_continuar` |
| `lbl_` | Label / texto estático | `lbl_titulo` |
| `img_` | Imagen / logo | `img_logo` |
| `inp_` | Campo de texto | `inp_buscar` |
| `rv_` | RecyclerView / lista | `rv_productos` |
| `ctr_` | Contenedor | `ctr_scrollRoot` |
| `hdr_` | Header de pantalla | `hdr_tituloTienda` |
| `item_` | Item dentro de lista | `item_producto` |
| `chk_` | Checkbox | `chk_terminos` |
