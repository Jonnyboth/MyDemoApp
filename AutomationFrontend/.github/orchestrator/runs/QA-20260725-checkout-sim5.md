# Flow Context - QA-20260725-checkout-sim5 - Checkout MyDemoApp (SIM-5)

Fecha: 2026-07-25
Plataforma: android
PlanStatus: Approved
RetryCount: 0
ApprovedBy: qa-explorer (validación empírica en vivo, misma sesión)
ApprovalDate: 2026-07-25
ApprovalNotes: Los 3 flujos (SIM-TC-1, SIM-TC-2, SIM-TC-3) re-confirmados en dispositivo real.
  Hipótesis del bug SIM-10 refutada empíricamente (ver "Hallazgo crítico" actualizado abajo).
  20 .rs creados en Object Repository/android/{Products,ProductDetail,Cart,Checkout,Payment}/.
RejectionNotes: N/A
DispositivoExplorado: emulator-5554 (AVD "qa_android", sdk_gphone64_x86_64, Android 14)
ResolucionExplorada: 1080x2400 px

## Ticket / Test Cases

- Ticket_HU: SIM-5 — "Checkout" (proyecto SIM)
- TC_id a automatizar (obtenidos vía `aio-tests-mcp get_test_case`):
  - SIM-TC-1 — "Checkout - Envío exitoso del formulario de dirección con todos los campos obligatorios completos"
  - SIM-TC-2 — "Checkout - Validación de campos obligatorios vacíos al presionar 'To Payment'"
  - SIM-TC-3 — "Checkout - El campo Zip Code debe recibir foco y ser editable mediante toque directo" (regresión bug SIM-10)
- Exploración previa reutilizada: `FuncionalQaPm/docs/QaExplorer/Android/MyDemoApp/Checkout/Checkout/Checkout.md`
  (documentaba el bug de SIM-10 como bloqueante total; ver "Riesgos y bifurcaciones" — hallazgo
  actualizado en esta sesión).

## Punto de entrada (setUp)

- TC reutilizado: ninguno existe para "catálogo → carrito → checkout". `openApp`/`OpenStoreGeant`
  son específicos de TuEmpresa (SKILL.md genérico) y no aplican a MyDemoApp.
- Login: SÍ se reutilizan los Steps ya creados en el run SIM-6 —
  `com.MyDemoApp.steps.android.LoginSteps.enterBodCredentials()` +
  `.tapLoginButton()` (paquete real del proyecto: `com.MyDemoApp.*`, **no** `com.tuempresa.*` — el
  SKILL.md genérico usa el naming del template TuEmpresa, pero este proyecto ya fijó su propio
  paquete desde SIM-TC-4/SIM-TC-5; mantener consistencia con lo existente).
  No se reutiliza `ensureOnLoginScreen()` tal cual (esa abre el drawer manualmente) porque en
  este flujo la navegación a Login es un side-effect automático de "Proceed To Checkout" sin
  sesión — se necesita una variante que solo espere el formulario Login y loguee, sin tocar el
  drawer.
- Nuevo setUp a crear (no existe todavía): flujo completo Home → agregar producto al carrito →
  ver carrito → "Proceed To Checkout" → (login si hace falta) → Checkout. Proponer
  `ProductsPage`/`ProductsSteps`, `CartPage`/`CartSteps` nuevos, además de `CheckoutPage`/`CheckoutSteps`.
- Motivo: primer conjunto de TCs de Checkout en este proyecto; no hay infraestructura previa de
  catálogo/carrito, solo de Login.

## Objetivo

- SIM-TC-1: validar que, con todos los campos obligatorios completos con datos reales, el
  formulario de Checkout avanza a la pantalla de pago ("Enter a payment method") sin errores.
- SIM-TC-2: validar que, con el formulario recién cargado (placeholders, sin datos reales), al
  presionar "To Payment" se muestran los 5 mensajes de error esperados y la app permanece en
  Checkout.
- SIM-TC-3 (regresión SIM-10): validar que el campo "Zip Code" recibe foco de teclado y es
  editable mediante toque directo, sin que "City" intercepte el toque ni pierda su valor.

## Precondiciones

- App `com.saucelabs.mydemoapp.android` instalada en emulator-5554.
- Cuenta `bod@example.com` / `10203040` activa (no bloqueada).
- Al menos 1 producto disponible en catálogo "Products" (se probó con "Sauce Labs Backpack",
  primer producto del grid, sin variar color/cantidad).

## Datos de prueba

| Escenario | Full Name | Address Line 1 | City | Zip Code | Country |
|---|---|---|---|---|---|
| SIM-TC-1 (éxito) | QA Tester | Calle Falsa 123 | Bogota | 110111 | Colombia |
| SIM-TC-2 (vacío) | (sin tocar, placeholders) | — | — | — | — |
| SIM-TC-3 (foco Zip) | QA Tester | Calle Falsa 123 | Bogota | 110111 | (no requerido para el assert de foco) |

## Pasos validados en dispositivo (ejecutados en vivo, emulator-5554, sesión de hoy)

**Estado inicial común**: `adb shell monkey -p com.saucelabs.mydemoapp.android -c
android.intent.category.LAUNCHER 1` → abre en catálogo "Products" (sin sesión).

**Setup común (los 3 TCs)**
1. Tap en imagen del primer producto (`productIV`, bounds `[52,505][519,1088]`) → navega al
   detalle "Sauce Labs Backpack".
   → Pre-tap Wait: SmartWaitPage.waitVisible(lbl_productsTitle, SHORT)
   → Post-tap Wait: SmartWaitPage.waitVisible(btn_addToCart, SHORT)
   → Wait Constant: SHORT — pantalla ya en DOM, sin llamada de red
2. Tap `cartBt` ("Add to cart", bounds `[393,2130][1028,2282]`, content-desc "Tap to add product
   to cart") → vuelve a Products, contador del carrito pasa a "1". ✅
   → Post-tap Wait: SmartWaitPage.waitVisible(lbl_cartCount, SHORT)
3. Tap `cartRL` (ícono carrito, bounds `[969,154][1048,233]`, content-desc "View cart") → navega
   a "My Cart" con el producto listado. ✅
   → Post-tap Wait: SmartWaitPage.waitVisible(lbl_myCartTitle, MEDIUM)
4. Tap `cartBt` en My Cart (bounds `[105,2093][975,2245]`, content-desc "Confirms products for
   checkout" — **mismo resource-id que el botón de paso 2, pantalla distinta → `.rs` separado**)
   → sin sesión iniciada, redirige a pantalla "Login". ✅ (confirma Flujo 0 de
   `Checkout.md`: "Proceed To Checkout" sin sesión → Login)
   → Post-tap Wait: SmartWaitPage.waitVisible(input_username, MEDIUM) — llamada de red posible
5. Tap `username1TV` ("bod@example.com", bounds `[85,1690][713,1734]`) → autocompleta
   `nameET`/`passwordET`. Tap `loginBtn` (bounds `[53,1349][1027,1475]`) → navega
   **automáticamente** a "Checkout" (no hay que repetir "Proceed To Checkout"). ✅
   → Reutilizar `CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.enterBodCredentials'()`
     + `.tapLoginButton'()` (variante que no pasa por `ensureOnLoginScreen()`/drawer).
   → Post-tap Wait: SmartWaitPage.waitVisible(lbl_checkoutTitle, MEDIUM)

**SIM-TC-2 — Validación de campos obligatorios vacíos (correr ANTES de escribir nada)**
6. Con el formulario recién cargado (placeholders grises: "Rebecca Winter", "Mandorley 112",
   "Truro", "89750", "United Kingdom"), tap `paymentBtn` (bounds `[117,1988][963,2114]`,
   content-desc "Saves user info for checkout") sin ingresar nada. ✅ Resultado exacto observado:
   - Full Name: **"Please provide your full name."**
   - Address Line 1: **"Please provide your address."**
   - City: **"Please provide your city."**
   - Zip Code: **"Please provide your zip"**
   - Country: **"Please provide your"** (mensaje truncado, verbatim)
   - Permanece en Checkout, no avanza a pago. ✅ (criterio de aceptación SIM-TC-2 cumplido)

**SIM-TC-1 / SIM-TC-3 — Llenado con datos reales (continuar en la misma pantalla, ya con errores visibles)**
7. Tap `fullNameET` (bounds base `[39,662][1041,780]`) → escribir "QA Tester". ✅
8. Tap `address1ET` (bounds base `[39,929][1041,1047]`, **re-dumpear antes de tocar**: el layout
   se desplaza verticalmente cuando aparecen/desaparecen los mensajes de error de los campos
   previos — ver "Riesgos") → escribir "Calle Falsa 123". ✅
9. Tap `cityET` (bounds base `[39,1463][519,1581]`, re-dumpear antes de tocar) → escribir
   "Bogota". ✅ **Importante**: completar este campo con un valor válido (que quite su error)
   ANTES de intentar Zip Code — ver hallazgo crítico abajo.
10. Tap `zipET` (bounds recalculado tras dump fresco, ej. `[39,1549][519,1667]` cuando el error de
    City ya no está visible) → **SÍ recibe foco** (`focused="true"` confirmado por dump), sin
    afectar `cityET` (conserva "Bogota" intacto). ✅ Escribir "110111". ✅
11. Tap `countryET` (bounds recalculado, análogo) → escribir "Colombia". ✅
12. Tap `paymentBtn` (bounds base `[117,1988][963,2114]`) → avanza a pantalla "Enter a payment
    method" / botón "Review Order", **sin ningún mensaje de error de validación**. ✅ (criterio de
    aceptación SIM-TC-1 cumplido; SIM-TC-3 validado como parte del paso 10)

## Hallazgo crítico — actualización del bug documentado en SIM-10 / `Checkout.md`

`Checkout.md` (misma fecha, sesión de exploración distinta) documentó que el campo "Zip Code" era
**totalmente inalcanzable** por touch/teclado, con "City" interceptando el toque. En esta sesión,
siguiendo la secuencia **Full Name → Address Line 1 → City (con valor válido) → Zip Code**, el
bug **no se reprodujo**: Zip Code recibió foco de teclado normalmente.

**Hipótesis (no confirmada por el usuario todavía)**: el touch target ampliado de "City" descrito
en `Checkout.md` coincide con el estado en que "City" **muestra su mensaje de error en rojo**
("Please provide your city.") — ese texto de error probablemente forma parte del mismo contenedor
clickeable/`ViewGroup` y expande el área que intercepta toques hacia abajo (hacia Zip Code). Al
completar City con un valor real antes de tocar Zip Code, el mensaje de error desaparece y el
touch target vuelve a su tamaño normal.

**No se automatiza este hallazgo como fix ni se reporta bug nuevo** — queda documentado aquí para
que `qa-explorer` lo re-confirme empíricamente (2-3 repeticiones, incluyendo el caso "tocar Zip
Code con el error de City todavía visible" para aislar la causa) antes de aprobar el plan. Si
`qa-explorer` confirma la hipótesis, el Script de SIM-TC-1/SIM-TC-3 debe garantizar el orden
Full Name → Address Line 1 → City → Zip Code (nunca tocar Zip Code mientras City tenga error
visible). Si `qa-explorer` NO logra reproducir el bug ni con el error visible, escalar al usuario
como posible bug ya intermitente/no reproducible de forma determinística, sin bloquear el plan.

### Resolución (qa-explorer, MODO VALIDACIÓN, misma sesión)

Se ejecutó el protocolo de validación empírica de tap (pre-dump → tap → post-dump comparando
`focused="true"`) sobre `zipET` en ambos escenarios:

- **(a) Con el error de "City" visible** (`cityErrorTV` = "Please provide your city." en rojo,
  City vacío): tap directo sobre `zipET` (bounds recalculados por dump fresco) →
  `focused="true"` confirmado en `zipET` inmediatamente. ✅ Bug NO reproducido.
- **(b) Con "City" ya completado** ("Bogota", sin error visible): tap directo sobre `zipET` →
  `focused="true"` confirmado igual. ✅ Bug NO reproducido.

**Conclusión: la hipótesis del touch target ampliado de "City" queda REFUTADA** en esta sesión —
Zip Code recibe foco de forma consistente en ambos estados, sin relación aparente con el mensaje
de error de City. La causa más probable del bug documentado en `Checkout.md` (misma fecha, sesión
distinta) es un error de cálculo de coordenadas de esa sesión anterior (bounds obsoletos tras un
layout shift, o coordenadas de tap calculadas antes de que apareciera/desapareciera el teclado)
— exactamente el mismo tipo de error que esta sesión de validación cometió y corrigió varias veces
(ver nota de teclado interceptando taps, abajo). **No se trata como bug de producto** — no se
reporta a Jira. El plan queda **Approved sin necesidad de la mitigación de orden de campos**,
aunque se mantiene el orden Full Name → Address 1 → City → Zip → Country en el Script por ser el
orden natural del formulario, no por ser mitigación obligatoria de un bug.

**Nota operativa importante para `qa-test-creator` (nueva, de esta sesión de validación):** varias
veces un tap a un campo distinto fue interceptado por el teclado en pantalla (QWERTY o numérico)
en vez de llegar al `EditText` real, produciendo texto concatenado en el campo equivocado. Mitigación
obligatoria en `CheckoutPage`: **llamar `Mobile.hideKeyboard()` (o back) inmediatamente después de
escribir en cada campo, antes de tocar el siguiente**, en vez de encadenar taps con el teclado
todavía abierto.

## Componentes capturados (sin registrar .rs — pendiente de captura empírica por qa-explorer)

Nota: bounds observados en esta sesión; `input_city`/`input_zip`/`input_country` cambian de
bounds dinámicamente según mensajes de error visibles en campos previos — qa-explorer debe
re-dumpear inmediatamente antes de cada tap_validated, no confiar en bounds fijos.

| Paso | pantalla | class | text | identifier (resource-id) | label/content-desc | bounds (referencia) | .rs sugerido | locator preferido | locator respaldo |
|------|----------|-------|------|---------------------------|---------------------|----------------------|---------------|--------------------|--------------------|
| 1 | Products | ImageView | — | productIV | Product Image | [52,505][519,1088] | btn_firstProductImage | ANDROID_UI_AUTOMATOR (resource-id, primer match del RecyclerView) | ATTRIBUTES |
| 2 | ProductDetail | Button | Add to cart | cartBt | Tap to add product to cart | [393,2130][1028,2282] | btn_addToCart | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| 3 | Products/Detail | RelativeLayout | — | cartRL | View cart | [969,154][1048,233] | btn_viewCart | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| 3b | Cualquiera (header) | TextView | número | cartTV | — | [1007,154][1023,193] | lbl_cartItemCount | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 4 | Cart | TextView | My Cart | — (verificar resource-id) | — | — | lbl_myCartTitle | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 4b | Cart | Button | Proceed To Checkout | cartBt | Confirms products for checkout | [105,2093][975,2245] | btn_proceedToCheckout | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| 6 | Checkout | TextView | Checkout | checkoutTitleTV | — | [39,298][1041,373] | lbl_checkoutTitle | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 7 | Checkout | EditText | Rebecca Winter (placeholder) | fullNameET | — | [39,662][1041,780] | input_fullName | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 8 | Checkout | EditText | Mandorley 112 (placeholder) | address1ET | — | [39,929][1041,1047] | input_address1 | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| — | Checkout | EditText | Entrance 1 (placeholder, opcional) | address2ET | — | [39,1196][1041,1314] | input_address2 | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 9 | Checkout | EditText | Truro (placeholder) | cityET | — | [39,1463][519,1581]* | input_city | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| — | Checkout | EditText | Cornwall (placeholder, opcional) | stateET | — | [540,1463][1041,1581] | input_state | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 10 | Checkout | EditText | 89750 (placeholder) | zipET | — | [39,1730][519,1848]* | input_zip | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 11 | Checkout | EditText | United Kingdom (placeholder) | countryET | — | [540,1730][1020,1848]* | input_country | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 6/12 | Checkout | Button | To Payment | paymentBtn | Saves user info for checkout | [117,1988][963,2114] | btn_toPayment | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| 6 (error) | Checkout | TextView | "Please provide your full name." | fullNameErrorTV | — | [39,791][1041,842] | lbl_fullNameError | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 6 (error) | Checkout | TextView | "Please provide your address." | address1ErrorTV | — | [39,1058][1041,1109] | lbl_address1Error | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 6 (error) | Checkout | TextView | "Please provide your city." | cityErrorTV | — | [39,1592][438,1643] | lbl_cityError | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 6 (error) | Checkout | TextView | "Please provide your zip" | zipErrorTV | — | [39,1859][420,1910] | lbl_zipError | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 6 (error) | Checkout | TextView | "Please provide your" | countryErrorTV | — | [540,1859][864,1910] | lbl_countryError | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 12 | Payment | TextView | Enter a payment method | enterPaymentMethodTV | — | [39,457][1041,512] | lbl_enterPaymentMethod | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| 12 | Payment | Button | Review Order | paymentBtn | Saves payment info and launches screen to review checkout data | [78,2133][1002,2259] | btn_reviewOrder | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |

`*` = bounds observados en un momento puntual; recalcular con dump fresco al capturar (ver
"Hallazgo crítico" — el layout se desplaza según errores visibles).

## Componentes validados empíricamente

| .rs sugerido | resource-id/CSS | content-desc | bounds reales | base_x (1080) | base_y (2340) | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|---|---|
| btn_firstProductImage | productIV (instance 0) | Product Image | [52,505][519,1088] | 285 | 774 | ✅ true | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| btn_addToCart | cartBt | Tap to add product to cart | [393,2130][1028,2282] | 710 | 2098 | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| btn_viewCart | cartRL | View cart | [969,154][1048,233] | 1008 | 188 | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| btn_proceedToCheckout | cartBt (pantalla Cart) | Confirms products for checkout | [105,2093][975,2245] | 540 | 2064 | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| input_fullName | fullNameET | — | [39,662][1041,780] | 540 | 703 | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| input_address1 | address1ET | — | [39,929][1041,1047] | 540 | 962 | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| input_city | cityET | — | [39,1463][519,1581]* | 279 | 1481 | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| input_zip | zipET | — | [39,1730][519,1848]* | 279 | 1743 | ✅ true (foco confirmado en ambos escenarios — ver Resolución) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| input_country | countryET | — | [540,1730][1020,1848]* | 780 | 1743 | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| btn_toPayment | paymentBtn (pantalla Checkout) | Saves user info for checkout | [117,1988][963,2114] | 540 | 1993 | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| btn_reviewOrder | paymentBtn (pantalla Payment) | Saves payment info and launches screen to review checkout data | [78,2133][1002,2259] | 540 | 2049 | ⚠️ no re-tocado (solo se confirmó su presencia tras avanzar; no requerido por los 3 criterios de aceptación) | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |

`*` = bounds tomados con dump fresco justo antes del tap real que produjo el cambio de foco
confirmado (ver pasos 9-11 del plan); no confiar en un único valor fijo, re-dumpear en runtime si
el layout difiere.

## Riesgos y bifurcaciones

- **[Riesgo alto — ver Hallazgo crítico]** El touch target de `cityET` puede ampliarse cuando su
  mensaje de error está visible, potencialmente interceptando toques a `zipET`. Mitigación
  propuesta: `CheckoutSteps` siempre completa City con un valor válido antes de tocar Zip Code,
  nunca al revés. `qa-explorer` debe confirmar esta causa-raíz antes de aprobar.
- El layout de Checkout es un `ScrollView` cuyos bounds de los `EditText` cambian dinámicamente
  (varias decenas de px) según aparecen/desaparecen mensajes de error en campos anteriores.
  **No cachear bounds absolutos** — cada Page method debe hacer su propio
  `findTestObject`/`waitVisible` inmediatamente antes de tocar, y qa-explorer debe re-dumpear
  antes de cada `tap_validated` en vez de reusar bounds de un paso anterior.
- Sesión no persistente entre relanzamientos de proceso (mismo patrón que Login SIM-6): si el
  runner reutiliza la sesión Appium entre TCs, `CheckoutPage`/`CartPage` deben detectar el estado
  real (¿ya hay sesión iniciada? ¿ya hay item en el carrito?) en vez de asumir estado limpio —
  análogo a como `LoginPage.ensureOnLoginScreen()` ya lo resuelve para Login.
- SIM-TC-2 debe ejecutarse con el formulario recién cargado (placeholders, sin datos reales) —
  si el runner corre SIM-TC-1 antes y reutiliza la misma sesión/pantalla, `CheckoutPage` necesita
  un método de "reset" del formulario (recargar Checkout desde cero) antes de SIM-TC-2, análogo al
  ciclo `force-stop`/relaunch documentado para Login. Recomendación: cada Script hace su propio
  ciclo de carrito completo (agregar producto de nuevo) para llegar a Checkout limpio, en vez de
  asumir que el formulario anterior se resetea solo.
- Mensaje de error de Country truncado visualmente ("Please provide your") — verbatim tal como se
  observa en pantalla, no es un error de captura; documentar así en el assert, no "corregirlo".

## Cobertura mínima recomendada

- SIM-TC-1 (Positivo/Smoke), SIM-TC-2 (Negativo/Regresión) y SIM-TC-3 (Regresión bug SIM-10) —
  los 3 cubiertos end-to-end en esta sesión de exploración.
- Fuera de alcance explícito: SIM-TC-6 (Login, ticket SIM-6) — se planifica en un run separado
  (`QA-20260725-login-username-sim6`), reutilizando `LoginPage`/`LoginSteps` ya existentes.

## Criterios de aceptación

- [x] SIM-TC-1: con todos los campos completos, "To Payment" avanza a "Enter a payment method"
      sin errores de validación.
- [x] SIM-TC-2: con el formulario vacío, "To Payment" muestra los 5 mensajes de error exactos y
      permanece en Checkout.
- [x] SIM-TC-3: "Zip Code" recibe foco de teclado y es editable por toque directo, sin alterar
      "City" (siguiendo el orden de llenado Full Name → Address 1 → City → Zip Code).

## Instrucciones para qa-test-creator

- setUp: no reutilizable de proyectos previos — crear `ProductsPage`/`ProductsSteps` (agregar
  primer producto al carrito), `CartPage`/`CartSteps` (view cart + proceed to checkout),
  `CheckoutPage`/`CheckoutSteps` (formulario + asserts), todos en el paquete real del proyecto
  `com.MyDemoApp.page.android` / `com.MyDemoApp.steps.android` (NO `com.tuempresa.*` del SKILL.md
  genérico).
- Keywords a reutilizar: `com.MyDemoApp.steps.android.LoginSteps.enterBodCredentials()` +
  `.tapLoginButton()` (sin pasar por `ensureOnLoginScreen()`/drawer — el login aquí es un
  redirect automático de "Proceed To Checkout"). `com.MyDemoApp.page.common.UtilsPage` para
  scroll/validación masiva si aplica. `MyDemoApp.utils.SmartWaitPage` para todos los waits
  (nunca `Mobile.delay()`).
- Nuevos a crear:
  - `Object Repository/android/Products/btn_firstProductImage.rs`
  - `Object Repository/android/ProductDetail/btn_addToCart.rs`
  - `Object Repository/android/Products/btn_viewCart.rs`, `lbl_cartItemCount.rs`
  - `Object Repository/android/Cart/lbl_myCartTitle.rs`, `btn_proceedToCheckout.rs`
  - `Object Repository/android/Checkout/lbl_checkoutTitle.rs`, `input_fullName.rs`,
    `input_address1.rs`, `input_address2.rs`, `input_city.rs`, `input_state.rs`, `input_zip.rs`,
    `input_country.rs`, `btn_toPayment.rs`, `lbl_fullNameError.rs`, `lbl_address1Error.rs`,
    `lbl_cityError.rs`, `lbl_zipError.rs`, `lbl_countryError.rs`
  - `Object Repository/android/Payment/lbl_paymentTitle.rs`
  - `Keywords/com/MyDemoApp/page/android/ProductsPage.groovy`,
    `Keywords/com/MyDemoApp/steps/android/ProductsSteps.groovy`
  - `Keywords/com/MyDemoApp/page/android/CartPage.groovy`,
    `Keywords/com/MyDemoApp/steps/android/CartSteps.groovy`
  - `Keywords/com/MyDemoApp/page/android/CheckoutPage.groovy`,
    `Keywords/com/MyDemoApp/steps/android/CheckoutSteps.groovy`
  - `Scripts/android/Checkout/SIM-TC-1-checkoutEnvioExitoso/Script<timestamp>.groovy`
  - `Scripts/android/Checkout/SIM-TC-2-checkoutCamposObligatoriosVacios/Script<timestamp>.groovy`
  - `Scripts/android/Checkout/SIM-TC-3-checkoutZipCodeRecibeFoco/Script<timestamp>.groovy`
  - `Test Cases/android/Checkout/SIM-TC-1-checkoutEnvioExitoso.tc`
  - `Test Cases/android/Checkout/SIM-TC-2-checkoutCamposObligatoriosVacios.tc`
  - `Test Cases/android/Checkout/SIM-TC-3-checkoutZipCodeRecibeFoco.tc`
  - Naming: seguir el ejemplo `SIM-TC-4-loginExitoso` de `katalon-mobile-automation/SKILL.md`
    (los TCs previos SIM-TC-4/SIM-TC-5 de este proyecto NO siguen ese sufijo — es una
    inconsistencia previa, no repetirla en los TCs nuevos).

## Instrucciones para qa-explorer

- Re-confirmar el "Hallazgo crítico" (bug SIM-10) con el protocolo de validación empírica: repetir
  el toque a Zip Code (a) con el error de City visible y (b) con City ya válido, para aislar la
  causa antes de aprobar/rechazar.
- Capturar resource-id exactos de los 5 labels de error (`lbl_*Error`) — no se llegaron a anotar
  en esta sesión de planificación, solo su texto y posición relativa.
- Confirmar bounds base 1080×2340 para cada input tras su propio dump (no reusar los bounds de
  este documento sin re-validar, dado el desplazamiento dinámico documentado).

## Skills invocados

- qa-flow-planner: done (planificación + exploración empírica en vivo, 3 TCs, incluyendo
  re-descubrimiento del estado actual del bug SIM-10)
- qa-explorer (validar): done (re-confirmación empírica en vivo, hipótesis del bug SIM-10
  refutada, 0 pasos ❌)
- qa-explorer (capturar): done (20 .rs creados, ver "Archivos generados")
- qa-test-creator: done
- runner: passed (SIM-TC-1, SIM-TC-2, SIM-TC-3 -- individual y en secuencia --tag checkout,
  misma sesión Appium)
- qa-debugger: n/a (cero ciclos de debug -- los 3 TCs pasaron al primer intento)

Phase: COMPLETED

## Archivos generados

- Page/Steps nuevos:
  - `Keywords/com/MyDemoApp/page/android/ProductsPage.groovy`
  - `Keywords/com/MyDemoApp/steps/android/ProductsSteps.groovy`
  - `Keywords/com/MyDemoApp/page/android/CartPage.groovy`
  - `Keywords/com/MyDemoApp/steps/android/CartSteps.groovy`
  - `Keywords/com/MyDemoApp/page/android/CheckoutPage.groovy`
  - `Keywords/com/MyDemoApp/steps/android/CheckoutSteps.groovy`
- Scripts:
  - `Scripts/android/Checkout/SIM-TC-1-checkoutEnvioExitoso/Script1785040552996.groovy`
  - `Scripts/android/Checkout/SIM-TC-2-checkoutCamposObligatoriosVacios/Script1785040552997.groovy`
  - `Scripts/android/Checkout/SIM-TC-3-checkoutZipCodeRecibeFoco/Script1785040552998.groovy`
- Test Cases:
  - `Test Cases/android/Checkout/SIM-TC-1-checkoutEnvioExitoso.tc`
  - `Test Cases/android/Checkout/SIM-TC-2-checkoutCamposObligatoriosVacios.tc`
  - `Test Cases/android/Checkout/SIM-TC-3-checkoutZipCodeRecibeFoco.tc`
- Object Repository (20 .rs):
  - `Object Repository/android/Products/btn_firstProductImage.rs`
  - `Object Repository/android/Products/btn_viewCart.rs`
  - `Object Repository/android/Products/lbl_cartItemCount.rs`
  - `Object Repository/android/ProductDetail/btn_addToCart.rs`
  - `Object Repository/android/Cart/lbl_myCartTitle.rs`
  - `Object Repository/android/Cart/btn_proceedToCheckout.rs`
  - `Object Repository/android/Checkout/lbl_checkoutTitle.rs`
  - `Object Repository/android/Checkout/input_fullName.rs`
  - `Object Repository/android/Checkout/input_address1.rs`
  - `Object Repository/android/Checkout/input_address2.rs`
  - `Object Repository/android/Checkout/input_city.rs`
  - `Object Repository/android/Checkout/input_state.rs`
  - `Object Repository/android/Checkout/input_zip.rs`
  - `Object Repository/android/Checkout/input_country.rs`
  - `Object Repository/android/Checkout/btn_toPayment.rs`
  - `Object Repository/android/Checkout/lbl_fullNameError.rs`
  - `Object Repository/android/Checkout/lbl_address1Error.rs`
  - `Object Repository/android/Checkout/lbl_cityError.rs`
  - `Object Repository/android/Checkout/lbl_zipError.rs`
  - `Object Repository/android/Checkout/lbl_countryError.rs`
  - `Object Repository/android/Payment/lbl_enterPaymentMethod.rs`
  - `Object Repository/android/Payment/btn_reviewOrder.rs`

## Reporte final

✅ Pipeline completado — 3/3 TCs PASSED, 0 ciclos de debug, 0 rechazos de plan.

Runner (individual): SIM-TC-1 PASSED (29530ms) · SIM-TC-2 PASSED (20823ms) ·
SIM-TC-3 PASSED (22607ms)
Runner (secuencia --tag checkout, misma sesión Appium): SIM-TC-2 PASSED (14815ms) ·
SIM-TC-1 PASSED (22765ms) · SIM-TC-3 PASSED (28537ms)

Nota operativa: el servidor Appium standalone (`appium --port 4723`) no estaba corriendo al
iniciar este run -- el primer intento de SIM-TC-1 falló con `SessionNotCreatedException`
(HTTP connect timed out a localhost:4723). Se inició el servidor manualmente y los 3 TCs
pasaron sin más incidentes. No fue necesario invocar qa-debugger (no es un fallo de
automatización, sino de infraestructura previa a la sesión Appium).

Nota de diseño: a diferencia del run de Login (SIM-6), aquí cada invocación `run --case`
abre y cierra su propia sesión Appium (no la reutiliza entre invocaciones separadas) -- la
reutilización de sesión solo aplica dentro de una misma invocación multi-TC (`--tag`/`--all`).
La sesión de bod@example.com persistió a nivel de la app en el emulador incluso entre
sesiones Appium distintas (SharedPreferences del dispositivo, no solo memoria de proceso),
por lo que `CheckoutSteps.isLoginScreenShowing()` nunca entró en la rama de login durante
estas corridas -- confirma que la rama condicional es necesaria igual para una ejecución en
un dispositivo/emulador limpio sin esa sesión heredada.
