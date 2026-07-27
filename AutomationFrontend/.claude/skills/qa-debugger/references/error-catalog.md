# Catálogo de errores comunes + triage protocol

> Extraído sin cambios de lógica desde `BMO-Debugger.agent.md`.

## Error 1 — `Name is null at MobileLocatorStrategy.valueOf`

**Causa típica:** `.rs` con estructura incompatible (`locatorStrategy` dentro de
`locator`, `locator` como bloque XML, o `locatorCollection` duplicado en dos niveles).

**Diagnóstico:** leer el `.rs` afectado → comparar estructura contra el SKILL oficial →
confirmar que `<locator>` es texto plano XPath, `<locatorStrategy>` está al nivel raíz,
`<locatorCollection>` tiene las entradas estándar.

**Fix correcto:** reescribir el `.rs` al formato del SKILL. No dejar estructuras
mezcladas ni duplicadas. Anti-pattern prohibido: `locatorStrategy` dentro de `locator`;
`locatorCollection` interno y externo al mismo tiempo.

## Error 2 — Element not found / Timeout

**Diagnóstico mínimo:**
1. Confirmar dispositivo/navegador activo.
2. Navegar a la pantalla.
3. Screenshot.
4. **UIAutomator dump** (Android, primario — más rápido que `mobile_list_elements_on_screen`):
   ```bash
   adb -s <deviceId> shell uiautomator dump /sdcard/debug.xml
   adb -s <deviceId> pull /sdcard/debug.xml /tmp/debug.xml
   ```
   Para Web: inspección DOM vía Selenium (`driver.pageSource` o DevTools).
5. Comparar `resource-id/CSS/text/content-desc/bounds` con el `.rs`.

**Fix:** ajustar locator con evidencia real de dispositivo/navegador.

## Error 3 — Violación POM 3 capas

**Reglas:** Page usa `Mobile.*`/`WebUI.*` y `findTestObject()`; Steps usa `@Keyword` y
llamadas a Page; Script usa `CustomKeywords` y lifecycle.

**Fix:** reubicar la lógica en su capa correcta.

## Error 4 — Coordenadas absolutas sin escalado (Android)

**Señales:** `tapAtPosition` con valores enteros fijos sin llamada a
`DeviceResolutionPage`; test pasa en un dispositivo y falla en otro.

**Diagnóstico:**
1. Leer el Page class afectado, buscar `tapAtPosition`/`swipe` con valores literales.
2. Verificar si el elemento tiene locator UIAutomator disponible (dump en el dispositivo
   actual).
3. Si hay locator → reemplazar `tapAtPosition` por `Mobile.tap(findTestObject(...))`.
4. Si no hay locator (Compose UI, `clickable=false`) → aplicar escalado con
   `DeviceResolutionPage`.

**Fix con escalado:**
```groovy
// Antes (incorrecto — absoluto):
Mobile.tapAtPosition(540, 2080)

// Después (correcto — escalado):
import com.tuempresa.page.common.DeviceResolutionPage
private static final int BTN_X = 540   // base 1080px
private static final int BTN_Y = 2080  // base 2340px

int x = DeviceResolutionPage.scaleX(BTN_X)
int y = DeviceResolutionPage.scaleY(BTN_Y)
Mobile.tapAtPosition(x, y)
```

---

## Locator Failure Triage Protocol

Cuando se reporta `NoSuchElementException`, timeout de `waitForElementPresent`/
`waitForElementVisible`, o "element not found":

**Paso 1 — Identificar la estrategia que falla.** Revisar el error y el `.rs` para saber
qué `selectorMethod` estaba activo: `ACCESSIBILITY` (content-desc cambió),
`ANDROID_UI_AUTOMATOR` (resource-id o jerarquía de clase cambió), `ATTRIBUTES`/`CSS`
(estructura XPath/CSS cambió — el más frágil).

**Paso 2 — Capturar dump/inspección fresca.**
```bash
adb shell uiautomator dump && adb pull /sdcard/window_dump.xml /tmp/window_dump.xml
```
Buscar el elemento por las 3 estrategias: content-desc anterior, resource-id, clase+posición.

**Paso 3 — Aplicar fix mínimo.**

| Hallazgo | Fix |
|---|---|
| Estrategia primaria rota pero el fallback funciona | Cambiar `<selectorMethod>` en el `.rs` a la estrategia que funciona |
| Todas las estrategias XML rotas | Probar `VisualLocatorPage.findByVisual("label")` como último recurso (solo Android) |
| El elemento ya no existe en la app | Escalar al usuario — la pantalla fue rediseñada |
| El elemento existe pero con nuevos atributos | Actualizar las entradas del `.rs` con los nuevos valores (las 3 estrategias) |

**Paso 4 — Prevenir recurrencia.** Confirmar que el `.rs` reparado tiene las 3
estrategias pobladas; si solo tenía 1 (causa raíz del fallo), agregar las que faltan;
registrar la estrategia rota como referencia futura.

## Utilidades reutilizables para debugging

| Utilidad | Ruta | Cuándo usarla en un fix |
|---|---|---|
| `UtilsPage` | `Keywords/com/tuempresa/page/common/UtilsPage.groovy` | El error ocurre en scroll o validación de múltiples elementos — reemplazar implementaciones ad-hoc por `scrollToElement()`/`validateElements()` |
| `DeviceResolutionPage` | `Keywords/com/tuempresa/page/common/DeviceResolutionPage.groovy` | El error ocurre por coordenadas absolutas sin escalar — usar `scaleX/scaleY/scalePoint()`; llamar `invalidateCache()` si cambió el dispositivo mid-sesión |
| `SmartWaitPage` | `Keywords/tuempresa/utils/SmartWaitPage.groovy` | Timeout de elemento: si falla con `SHORT` pero pasa con `MEDIUM` → problema de red, no de locator |
| `LocatorHelper` | `Keywords/tuempresa/utils/LocatorHelper.groovy` | `NoSuchElementException` — usar `findWithFallback()` para identificar qué estrategia sigue funcionando |
| `ScreenshotPage` | `Keywords/tuempresa/utils/ScreenshotPage.groovy` | Estado de UI inesperado — comparar pantalla actual vs baseline (`captureAndCompare`, `updateBaseline`) |
| `VisualLocatorPage` | `Keywords/tuempresa/utils/VisualLocatorPage.groovy` | Último recurso cuando todas las estrategias XML fallan (solo Android) |

## Recaptura tras update de la app

Cuando un `.rs` necesita recapturarse tras un cambio en la app: `adb shell uiautomator
dump` (XML fresco) → comparar atributos viejos vs nuevos → actualizar `.rs` con los
nuevos valores (las 3 estrategias) → si el elemento ya no tiene atributos, agregar
imagen de muestra a `classifier-labels/` y usar `VisualLocatorPage`.
