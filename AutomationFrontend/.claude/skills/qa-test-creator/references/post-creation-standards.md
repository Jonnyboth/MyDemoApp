# Estándares post-creación (obligatorios en todo test generado)

> Extraído sin cambios de lógica desde `BMO-TestCreator.agent.md`.

## 1. Smart Wait compliance

Todo método de Page Object generado debe:
- Importar `tuempresa.utils.SmartWaitPage`
- Usar `SmartWaitPage.waitVisible(element, SmartWaitPage.CONSTANTE)` en vez de
  `Mobile.delay(N)`
- `SmartWaitPage.tapPause()` solo en loops de contador/incremento
- `SmartWaitPage.floorPause()` solo cuando no hay target de `waitVisible` disponible

## 2. Self-healing locators (dirigido por `tap_validated` del contexto)

Antes de poblar las estrategias de un `.rs`, leer el campo `tap_validated` del componente
en el archivo de contexto de `qa-flow-planner`/`qa-explorer`:

| `tap_validated` | Estrategias a poblar | `findWithFallback` | Comentario |
|---|---|---|---|
| `✅ true` | ACCESSIBILITY + ANDROID_UI_AUTOMATOR + ATTRIBUTES | ✅ usar si es ruta crítica | Interactivo confirmado en dispositivo |
| `❌ false` | Solo ATTRIBUTES | ❌ no aplica | No interactivo (label, decorativo) |
| `COMPOSE` | Solo coordenadas base + ATTRIBUTES de referencia | ❌ no aplica | `tapAtPosition` escalado con `DeviceResolutionPage` |
| *(ausente, contexto legacy)* | Poblar las 3 por defecto | ✅ si es ruta crítica | Prioridad ACCESSIBILITY > ANDROID_UI_AUTOMATOR > ATTRIBUTES |

Todo `.rs` con `tap_validated: true` debe tener ≥ 2 estrategias pobladas. Si `resource-id`
existe en los datos de contexto, es **obligatorio** usarlo en `ANDROID_UI_AUTOMATOR` como
`new UiSelector().resourceId("...")` — usar solo XPath cuando hay `resource-id` disponible
es un error de calidad.

## 3. Visual Baseline Capture (obligatorio)

Después de crear un test case, siempre agregar snapshots en 1–3 pantallas críticas:
```groovy
// BASELINE: primer run la establece; runs siguientes comparan automáticamente
CustomKeywords.'tuempresa.utils.ScreenshotPage.captureAndCompare'('test_name_screen_state')
```
Identificar puntos de snapshot en: pantallas de confirmación final (éxito), cambios de
estado de carrito/orden, pantallas de pago. Si no hay un punto natural, agregar uno en la
pantalla de aserción final.

## 4. LocatorHelper para elementos críticos

Para cualquier elemento en la ruta crítica de compra (carrito, checkout, pago, order
tracking), preferir:
```groovy
TestObject el = CustomKeywords.'tuempresa.utils.LocatorHelper.findWithFallback'(
    'content-desc-value',                     // ACCESSIBILITY
    'new UiSelector().resourceId("...")',      // ANDROID_UI_AUTOMATOR
    '//*[@content-desc="content-desc-value"]' // ATTRIBUTES
)
```
sobre una llamada `findTestObject()` sin fallback.

## 5.1 Documentación Javadoc (obligatoria en toda clase/método Keyword generado)

Katalon Studio parsea comentarios Javadoc de los Keywords para mostrarlos en el panel de
autocompletado del IDE (ver `.cache/Keywords/*` → `"javadocParsed": true`). Un método sin
documentar es invisible para quien lo reutiliza desde ahí, y para cualquiera que abra el
`.groovy` directamente el nombre del método no siempre basta para saber qué hace, cuándo
usarlo o qué asume.

Toda clase Page/Steps/Utils **nueva** debe llevar:
- Un bloque `/** ... */` a nivel de clase explicando qué pantalla o dominio encapsula.
- Un bloque `/** ... */` en cada método público explicando QUÉ hace (no reformular el
  nombre) y, si no es obvio desde la firma, efectos colaterales o precondiciones (ej.:
  "asume que el drawer ya está abierto", "requiere sesión activa").
- `@param`/`@return` cuando el parámetro o el valor de retorno no se explican solos.

No aplica a getters/setters triviales de una línea sin lógica.

Ejemplo (antes/después, sobre `SmartWaitPage.waitVisible`):
```groovy
// Antes -- el nombre no basta para saber CUANDO usarlo ni que pasa si falla
static boolean waitVisible(TestObject obj, int timeoutSec = MEDIUM, ...) { ... }

// Despues
/**
 * Espera a que un elemento este presente en el arbol de accesibilidad.
 * A diferencia de Mobile.delay(), no duerme un tiempo fijo: retorna en cuanto
 * el elemento aparece, y solo espera hasta timeoutSec en el peor caso.
 *
 * @param timeoutSec segundos maximos de espera (default MEDIUM)
 * @param fh que hacer si nunca aparece: STOP_ON_FAILURE lanza AssertionError
 * @return true si el elemento aparecio dentro del timeout
 */
static boolean waitVisible(TestObject obj, int timeoutSec = MEDIUM, ...) { ... }
```

Nota R-K4: nada de em-dashes (`—`) ni comillas tipográficas dentro de estos bloques
Javadoc -- son código fuente igual que el resto del archivo, y les aplica la misma regla
de sintaxis conservadora.

## 5.2 Documentación de Scripts (capa 3)

Los Scripts ya se autodocumentan en ejecución vía `Mobile.comment(...)` (cada llamada
aparece como fila propia en el árbol de ejecución de Katalon) -- eso no cambia. Lo que se
agrega es un encabezado corto al inicio del archivo, antes de la PRECONDICIÓN, para
alguien que abre el `.groovy` sin correrlo:

```groovy
// Ticket: SIM-6 -- Login con usuario y contraseña
// Objetivo: valida que bod@example.com/10203040 inicie sesion y llegue al catalogo
// Datos: bod@example.com / 10203040 (cuenta activa)
```

## 5.3 VisualLocatorPage para elementos dinámicos (solo Android)

Para elementos marcados `VISUAL_ONLY: true` en el plan (banners promocionales, contenido
dinámico sin atributos estables):
```groovy
import tuempresa.utils.VisualLocatorPage

TestObject promoEl = VisualLocatorPage.findByVisual('promo_banner_label', SmartWaitPage.MEDIUM)
if (promoEl != null) {
    Mobile.tap(promoEl, SmartWaitPage.MEDIUM)
} else {
    KeywordUtil.logInfo('⚠️ Promo element not found visually — skipping (non-critical)')
}
```
Plugin: `test-ai-classifier` v4.0.2, backend CPU. Agregar labels nuevos en
`Include/resources/classifier-labels/<label>/sample_N.png`.

## 6. Formato de firmas de método (parámetros múltiples)

Cuando un método tiene 2 o más parámetros, cada parámetro va en su propia línea,
alineado verticalmente en la columna inmediatamente después del paréntesis de apertura.
Nunca mezclar varios parámetros en una línea y el resto en otra -- o todos en una sola
línea (métodos sin parámetros o con uno solo y corto), o uno por línea.

```groovy
// Incorrecto -- mezcla 2 parametros en la primera linea y 1 en la segunda
static boolean waitVisible(TestObject obj, int timeoutSec = MEDIUM,
                            FailureHandling fh = FailureHandling.STOP_ON_FAILURE) { ... }

// Correcto -- uno por linea, alineados bajo el primer parametro
static boolean waitVisible(TestObject obj,
                            int timeoutSec = MEDIUM,
                            FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
    ...
}
```

Indentación siempre con 4 espacios, nunca tabs, consistente en todo el archivo (ver
R-K4 en `groovy-compat-rules.md`). Cuando varias constantes o campos se declaran
seguidos, alinear el `=` en la misma columna para que el bloque se lea como tabla:

```groovy
private static final String MENU_ICON  = 'Object Repository/android/Menu/btn_menuIcon'
private static final String LOGIN_ITEM = 'Object Repository/android/Menu/btn_logInMenuItem'
```
