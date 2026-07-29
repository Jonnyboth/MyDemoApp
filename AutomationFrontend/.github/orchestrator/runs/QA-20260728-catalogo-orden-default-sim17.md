# Flow Context - QA-20260728-catalogo-orden-default-sim17 - Catálogo, orden alfabético por defecto (SIM-17 / SIM-TC-16)

Fecha: 2026-07-28
Plataforma: android
PlanStatus: Approved
RetryCount: 0
ApprovedBy: qa-explorer (validación empírica en vivo, misma sesión)
ApprovalDate: 2026-07-28
ApprovalNotes: Ambos pasos del TC verificados en dispositivo real (force-stop + pm clear + start
  SplashActivity + uiautomator dump). lbl_productsCatalog (ya existente) confirmado vigente
  (content-desc "Displays all products of catalog" sin cambios). Los 4 titleTV (instance 0-3)
  confirmados presentes con resource-id "com.saucelabs.mydemoapp.android:id/titleTV", sin
  content-desc, en orden alfabético ascendente ("Sauce Labs Backpack" < "(green)" < "(orange)" <
  "(red)"). Cero pasos ❌ — MODO CAPTURA ejecutado sin navegaciones adicionales (reutilizó el dump
  de la fase de planificación).
RejectionNotes: N/A
DispositivoExplorado: emulator-5554 (AVD "qa_android", Android 14)
ResolucionExplorada: 1080x2400 px (bounds observados en el dump; consistente con runs previos de este proyecto)

## Ticket / Test Cases

- Ticket_HU: SIM-17 — "Ordenar el catálogo de productos por nombre o precio" (proyecto Jira SIM)
- TC_id a automatizar: **SIM-TC-16 únicamente** (pedido explícito del usuario — lista `[SIM-TC-16]`)
- Fuente del TC: `aio-tests-mcp` no autorizado en esta sesión → fallback aplicado: consulta en vivo vía
  `aio_tests_client.py get --id 29` contra la API REST real de AIO Tests (proyecto SIM), no exploración
  genérica. Contenido íntegro del TC (título, descripción, precondición, 2 pasos con resultado
  esperado, `Testing Layers: Android`, `Test Type Cycle: Smoke`, `type: Component`) recibido y usado
  tal cual — ver detalle completo en la sección "Objetivo" y "Pasos" abajo.
- Exploración previa reutilizada: `FuncionalQaPm/docs/QaExplorer/Android/MyDemoApp/Catalogo/Catalogo/Catalogo.md`
  (actualizada 2026-07-28 en la misma sesión QA que creó este TC).

## Punto de entrada (setUp)

- TC/Keyword reutilizado: `CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()`
  (`Keywords/com/MyDemoApp/steps/android/AppLifecycleSteps.groovy` → `AppLifecyclePage.restartApp()`).
- Motivo: ya existe en el proyecto (usado por SIM-TC-4/5/6 y SIM-TC-1/2/3) y hace exactamente lo que
  pide la precondición de SIM-TC-16 — `adb shell pm clear` (borra cache/sesión/preferencias, deja la
  app como recién instalada, **sin ningún criterio de orden aplicado**) + reapertura +
  `productsPage.ensureOnProductsScreen()` interno, que ya deja la app en el catálogo "Products". No
  hace falta ningún setUp nuevo ni navegación adicional — reutilizar esto es más fuerte que solo
  "abrir la app", porque además garantiza el estado limpio que el AC exige explícitamente ("sin haber
  aplicado previamente ningún criterio de orden").
- Nota sobre precondición extra del usuario ("Keywords y funciones de login"): evaluada y **no
  aplica** a este TC — el catálogo es la pantalla de entrada de la app tras el splash, sin
  autenticación (confirmado en `Catalogo.md` y en el dump en vivo de esta sesión). No se fuerza
  ningún paso de login. Las keywords de Login (`LoginSteps`/`LoginPage`) ya existen en el proyecto por
  si un TC futuro de este módulo las necesitara, pero SIM-TC-16 no las usa.

## Objetivo

- SIM-TC-16: Validar que el catálogo de productos se muestre ordenado alfabéticamente de forma
  ascendente (Name - Ascending) como criterio de orden por defecto, sin que el cliente aplique
  ninguna acción de ordenamiento.

## Precondiciones

- App `com.saucelabs.mydemoapp.android` instalada en `emulator-5554`.
- Ningún criterio de orden aplicado en la sesión actual (garantizado por `restartApp()` vía `pm clear`).

## Datos de prueba

Ninguno (TC de solo lectura/observación — no requiere input de usuario). `test_data: "N/A"` en ambos
pasos originales de AIO Tests.

## Pasos funcionales validados en dispositivo (ejecutados en vivo, emulator-5554)

**Estado inicial**: `adb shell am force-stop com.saucelabs.mydemoapp.android` → `pm clear
com.saucelabs.mydemoapp.android` → `am start -n .../SplashActivity` → tras ~3s, catálogo "Products"
visible (confirmado por `lbl_productsCatalog` / `productRV`, content-desc="Displays all products of
catalog", igual al ya capturado en `Object Repository/android/Products/lbl_productsCatalog.rs`).

**TC Paso 1 (AIO) — "Abrir la app MyDemoApp"**
1. `restartApp()` dejó la app en "Products" sin requerir login. ✅ (mapea a PRECONDICIÓN del Script)

Step 1: Confirmar catálogo visible tras el splash
  → Pre-tap Wait: N/A (no hay tap, es una validación de estado)
  → Post-tap Wait: `SmartWaitPage.waitVisible(lbl_productsCatalog, SmartWaitPage.MEDIUM)` (ya ocurre
    dentro de `ProductsPage.ensureOnProductsScreen()`, reutilizado explícitamente como Step 1 del
    Script para que quede como fila propia en el árbol de ejecución, igual que TC Paso 1 de AIO)
  → Wait Constant: MEDIUM (10s)
  → Rationale: espera de red/renderizado inicial tras el splash, ya validada en runs previos (Login)

**TC Paso 2 (AIO) — "Observar el listado de productos (productRV) y el ícono de orden (sortIV)"**

2. Dump UIAutomator (`adb shell uiautomator dump`) sobre el catálogo recién cargado, sin scroll,
   muestra 4 nodos `titleTV` visibles (2 filas del grid de 2 columnas):

   | Posición (instance) | text | bounds |
   |---|---|---|
   | 0 | Sauce Labs Backpack | [52,1101][411,1152] |
   | 1 | Sauce Labs Backpack (green) | [561,1101][1028,1195] |
   | 2 | Sauce Labs Backpack (orange) | [52,2046][519,2140] |
   | 3 | Sauce Labs Backpack (red) | [561,2046][1009,2097] |

   Orden alfabético ascendente confirmado entre las 4: "Backpack" < "Backpack (green)" < "Backpack
   (orange)" < "Backpack (red)" (comparación case-insensitive). ✅ Coincide con el ejemplo del AC
   ("'Sauce Labs Backpack' antes que 'Sauce Labs Backpack (green)'").

Step 2: Leer y comparar los 4 títulos visibles sin scroll
  → Pre-tap Wait: `SmartWaitPage.waitVisible(lbl_productsCatalog, SmartWaitPage.SHORT)` antes de leer
    texto, por si el layout aún está asentándose tras el Step 1
  → Post-tap Wait: N/A (solo lectura, no dispara navegación)
  → Wait Constant: SHORT (5s)
  → Rationale: los 4 `titleTV` ya están en pantalla apenas carga el catálogo — no requiere red
    adicional ni scroll, un timeout corto es suficiente

## Alcance de la validación (decisión de diseño explícita)

El AC menciona 3 productos como ejemplo ilustrativo ("...y estas antes que 'Sauce Labs Bike Light'"),
pero "Bike Light" solo es visible tras hacer scroll (queda fuera de las 2 filas iniciales). Se decide
**no scrollear** y validar solo los 4 productos visibles sin interacción adicional, porque:
1. El propio Paso 2 del TC en AIO Tests no menciona scroll ("Observar el listado..."), a diferencia de
   los TCs de SIM-7 (ya borrados) que sí incluían "recorrer el listado completo" explícitamente.
2. 4 elementos en 2 filas ya son evidencia suficiente y determinista de que el criterio "Name -
   Ascending" está activo por defecto — es la validación más simple que cumple el AC sin agregar
   pasos de scroll no pedidos (Fase 5 del skill: control de alcance, no sobre-automatizar).
3. Mantiene el TC estable y rápido (validación de solo lectura, sin gestos adicionales que puedan
   fallar por timing).

Si en el futuro se pide validar el listado completo (como hacía el TC de SIM-7 ya eliminado), sería un
TC nuevo y explícito, no una ampliación silenciosa de este.

## Componentes exploratorios capturados (sin registrar .rs todavía)

| Paso | pantalla | class | text | identifier (resource-id) | label/content-desc | bounds | .rs sugerido | locator preferido | locator respaldo |
|---|---|---|---|---|---|---|---|---|---|
| 2 | Products | android.widget.TextView | Sauce Labs Backpack | com.saucelabs.mydemoapp.android:id/titleTV | (sin content-desc) | [52,1101][411,1152] | lbl_productTitle1 | ANDROID_UI_AUTOMATOR (resourceId + instance(0)) | ATTRIBUTES (XPath posición 1) |
| 2 | Products | android.widget.TextView | Sauce Labs Backpack (green) | com.saucelabs.mydemoapp.android:id/titleTV | (sin content-desc) | [561,1101][1028,1195] | lbl_productTitle2 | ANDROID_UI_AUTOMATOR (resourceId + instance(1)) | ATTRIBUTES (XPath posición 2) |
| 2 | Products | android.widget.TextView | Sauce Labs Backpack (orange) | com.saucelabs.mydemoapp.android:id/titleTV | (sin content-desc) | [52,2046][519,2140] | lbl_productTitle3 | ANDROID_UI_AUTOMATOR (resourceId + instance(2)) | ATTRIBUTES (XPath posición 3) |
| 2 | Products | android.widget.TextView | Sauce Labs Backpack (red) | com.saucelabs.mydemoapp.android:id/titleTV | (sin content-desc) | [561,2046][1009,2097] | lbl_productTitle4 | ANDROID_UI_AUTOMATOR (resourceId + instance(3)) | ATTRIBUTES (XPath posición 4) |

Nota: mismo patrón ya usado en `Object Repository/android/Products/btn_firstProductImage.rs`
(resource-id `productIV` repetido → `new UiSelector().resourceId("...").instance(0)`). `titleTV`
repite igual que `productIV`, por eso se captura por instancia en vez de por texto (el texto es
justamente el dato a leer, no puede ser parte del locator).

## Componentes validados empíricamente

`.rs` generados en `Object Repository/android/Products/` (MODO CAPTURA, sin navegaciones
adicionales — reutilizó `/tmp/catalog_default.xml` de la fase de planificación). Elementos de solo
lectura (no `clickable`), por lo que `tap_validated` no aplica — se confirma en su lugar que el
`resource-id`/`instance` resuelve al nodo con el `text` esperado (equivalente a la verificación
empírica para elementos interactivos).

| .rs generado | resource-id | content-desc | bounds reales | text confirmado | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|---|
| lbl_productTitle1 | com.saucelabs.mydemoapp.android:id/titleTV (instance 0) | NOT AVAILABLE | [52,1101][411,1152] | "Sauce Labs Backpack" | N/A (solo lectura) | ANDROID_UI_AUTOMATOR | ATTRIBUTES/XPATH |
| lbl_productTitle2 | com.saucelabs.mydemoapp.android:id/titleTV (instance 1) | NOT AVAILABLE | [561,1101][1028,1195] | "Sauce Labs Backpack (green)" | N/A (solo lectura) | ANDROID_UI_AUTOMATOR | ATTRIBUTES/XPATH |
| lbl_productTitle3 | com.saucelabs.mydemoapp.android:id/titleTV (instance 2) | NOT AVAILABLE | [52,2046][519,2140] | "Sauce Labs Backpack (orange)" | N/A (solo lectura) | ANDROID_UI_AUTOMATOR | ATTRIBUTES/XPATH |
| lbl_productTitle4 | com.saucelabs.mydemoapp.android:id/titleTV (instance 3) | NOT AVAILABLE | [561,2046][1009,2097] | "Sauce Labs Backpack (red)" | N/A (solo lectura) | ANDROID_UI_AUTOMATOR | ATTRIBUTES/XPATH |

Checklist R-K1/R-K2/R-K3 (`rs-hard-rules.md`) verificado en los 4 archivos: `<selectorMethod>BASIC`
(nunca una estrategia), `<locator>` texto plano coherente con `<locatorStrategy>
ANDROID_UI_AUTOMATOR` (empieza con `new UiSelector()`), sin ejes XPath prohibidos en la entrada
`XPATH`/`ATTRIBUTES` (solo predicado `[N]`), 2 estrategias pobladas (ANDROID_UI_AUTOMATOR +
ATTRIBUTES/XPATH; ACCESSIBILITY documentado como `NOT AVAILABLE`), sin duplicados de nombre.

## Riesgos y bifurcaciones

- Si el dispositivo de ejecución tiene una resolución/densidad muy distinta a `emulator-5554`
  (1080x2400), podría renderizar solo 1 fila (2 productos) en vez de 2 filas (4 productos) sin scroll.
  Mitigación: `instance(0..3)` con `Mobile.waitForElementPresent(..., OPTIONAL)` antes de leer cada
  título — si `instance(2)`/`instance(3)` no aparecen, el assert compara solo los que sí están
  presentes (mínimo 2) en vez de fallar por un elemento fuera de viewport. `qa-test-creator` debe
  implementar esta tolerancia explícitamente en `ProductsPage`.
- `titleTV` no tiene `content-desc` (confirmado en el dump) — no hay alternativa ACCESSIBILITY,
  coherente con el mismo caso ya documentado en `btn_firstProductImage.rs`.
- Restart completo (`pm clear`) es más lento (~3s extra) que solo reabrir, pero es el mismo costo que
  ya pagan todos los TCs existentes del proyecto (Login, Checkout) — no es una regresión de este TC.

## Cobertura mínima recomendada

- SIM-TC-16 (Positivo/Smoke) — único TC pedido por el usuario en este run. Los otros 3 TCs de
  SIM-17 (SIM-TC-17/18/19, Name-Descending/Price-Ascending/Price-Descending) quedan fuera de
  alcance explícito de este run — no fueron pedidos.

## Criterios de aceptación

- [ ] Tras `restartApp()`, el catálogo "Products" está visible sin requerir login (Paso 1 del TC)
- [ ] Los 4 títulos de producto visibles sin scroll (`titleTV` instance 0-3) están en orden
      alfabético ascendente case-insensitive, consecutivo (título[i] <= título[i+1]) (Paso 2 del TC)

## Instrucciones para qa-test-creator

- setUp: `CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()` (ya existe,
  no crear nada nuevo en `AppLifecyclePage`/`AppLifecycleSteps`).
- Keywords a reutilizar tal cual: `ProductsSteps.ensureOnProductsScreen()` (ya existe) para el Step 1
  del Script; `Object Repository/android/Products/lbl_productsCatalog.rs` (ya existe).
- Nuevos a crear:
  - `Object Repository/android/Products/lbl_productTitle1.rs` (instance 0)
  - `Object Repository/android/Products/lbl_productTitle2.rs` (instance 1)
  - `Object Repository/android/Products/lbl_productTitle3.rs` (instance 2)
  - `Object Repository/android/Products/lbl_productTitle4.rs` (instance 3)
  - Método nuevo en `ProductsPage.groovy`: `List<String> getVisibleProductTitlesInOrder()` — lee con
    `Mobile.getText` cada uno de los 4 objetos anteriores usando `FailureHandling.OPTIONAL` (según
    riesgo documentado arriba, tolerante a menos de 4 visibles), devuelve solo los que sí aparecieron.
  - Método nuevo en `ProductsPage.groovy`: `void verifyDefaultSortIsNameAscending()` — obtiene la
    lista anterior, falla (`STOP_ON_FAILURE`, mensaje explícito con los títulos comparados) si algún
    `título[i].compareToIgnoreCase(título[i+1]) > 0`.
  - Método nuevo en `ProductsSteps.groovy`: `@Keyword void assertDefaultSortIsNameAscending()` →
    delega 1:1 a `productsPage.verifyDefaultSortIsNameAscending()` (mismo patrón que
    `LoginSteps.assertLockedOutError()` → `LoginPage.verifyLockedOutError()`).
  - `Test Cases/android/Catalogo/SIM-TC-16-ordenAscendentePorDefecto.tc`
  - `Scripts/android/Catalogo/SIM-TC-16-ordenAscendentePorDefecto/Script<timestamp>.groovy`
- Nombre canónico del TC (convención obligatoria `<PROYECTO>-TC-<ID>-<validación>`):
  **`SIM-TC-16-ordenAscendentePorDefecto`**
- Script (orden de llamadas):
  1. `Mobile.comment('SIM-TC-16: ...')` + PRECONDICIÓN `AppLifecycleSteps.restartApp()`
  2. `Mobile.comment('STEP 1: ...')` + `ProductsSteps.ensureOnProductsScreen()`
  3. `Mobile.comment('ASSERT 1: ...')` + `ProductsSteps.assertDefaultSortIsNameAscending()`

## Instrucciones para qa-explorer

- Repetir el ciclo `force-stop` + `pm clear` + `start SplashActivity` en `emulator-5554` para
  confirmar reproducibilidad de los 4 `titleTV` y sus `instance(N)` antes de registrar los `.rs`
  definitivos (dump ya capturado en esta sesión de planificación, pero re-confirmar en MODO CAPTURA
  es el protocolo estándar).
- No aplica protocolo de tap empírico (pre/post dump tras tap) — son 4 elementos de solo lectura, no
  interactivos; usar `Mobile.getText` como validación de que el locator resuelve al nodo correcto
  (comparar el texto leído contra la tabla de "Componentes exploratorios capturados" arriba).

## Compatibilidad multi-dispositivo

- No usa coordenadas (`tapAtPosition`) en ningún paso — 100% basado en locators
  (`ANDROID_UI_AUTOMATOR` con `resourceId + instance`), por lo tanto no depende de
  `DeviceResolutionPage.scaleX/scaleY` ni de una resolución de referencia fija.
- Único riesgo de variación entre dispositivos: cuántas filas del grid caben en pantalla sin scroll
  (ver "Riesgos y bifurcaciones" — mitigado con lectura `OPTIONAL` tolerante a 2-4 elementos en vez de
  exigir exactamente 4).
- Perfil objetivo de esta validación: `emulator-5554` (AVD "qa_android", Android 14, 1080x2400) — el
  mismo dispositivo usado en todos los runs previos de este proyecto.

## Skills invocados

- qa-flow-planner: done (este archivo)
- qa-explorer (validar): done (validación empírica en vivo, 0 pasos ❌, Approved)
- qa-explorer (capturar): done (4 .rs creados, ver "Archivos generados")
- qa-test-creator: done
- runner: passed (SIM-TC-16-ordenAscendentePorDefecto, 5203ms, primer intento)
- qa-debugger: n/a (cero ciclos de debug — PASSED al primer intento)

Phase: COMPLETED
RunnerRetryCount: 0

## Archivos generados

- Object Repository (4 .rs, fase Explorar):
  - `Object Repository/android/Products/lbl_productTitle1.rs`
  - `Object Repository/android/Products/lbl_productTitle2.rs`
  - `Object Repository/android/Products/lbl_productTitle3.rs`
  - `Object Repository/android/Products/lbl_productTitle4.rs`
- Page/Steps (métodos añadidos a archivos ya existentes, fase Crear Tests):
  - `Keywords/com/MyDemoApp/page/android/ProductsPage.groovy` — agregados
    `verifyDefaultSortIsNameAscending()` (público, `@Keyword`) y
    `getVisibleProductTitlesInOrder()` (privado, helper)
  - `Keywords/com/MyDemoApp/steps/android/ProductsSteps.groovy` — agregado
    `assertDefaultSortIsNameAscending()` (`@Keyword`, delega a Page)
- Scripts:
  - `Scripts/android/Catalogo/SIM-TC-16-ordenAscendentePorDefecto/Script1785270224106.groovy`
- Test Cases:
  - `Test Cases/android/Catalogo/SIM-TC-16-ordenAscendentePorDefecto.tc`

## Reporte final

✅ Pipeline completado — 1/1 TC PASSED, 0 ciclos de debug, 0 rechazos de plan.

Runner: SIM-TC-16-ordenAscendentePorDefecto PASSED (5203ms)

Nota de infraestructura: el runner requiere un servidor Appium real en `http://localhost:4723`
(distinto de los procesos `appium-mcp` del MCP, que no exponen ese puerto). No había ninguno
corriendo al iniciar esta fase — se lanzó uno (`appium --address 127.0.0.1 --port 4723`,
v3.5.2, driver `uiautomator2@8.1.0`) en background antes de poder ejecutar el runner.

Pendiente fuera de alcance de este run (no solicitado por el usuario):
- SIM-TC-17/18/19 (Name-Descending, Price-Ascending, Price-Descending, mismo TC_id de SIM-17) —
  el usuario pidió explícitamente solo `[SIM-TC-16]`.
- Gate R-K5 (Katalon Studio Problems panel = 0 errors) — requiere abrir el proyecto en Katalon
  Studio Desktop; no aplica al loop autónomo headless per `qa-test-creator/manifest.yaml`.
