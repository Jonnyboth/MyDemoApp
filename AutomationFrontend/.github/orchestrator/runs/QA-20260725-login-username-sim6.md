# Flow Context - QA-20260725-login-username-sim6 - Login Username obligatorio (SIM-6)

Fecha: 2026-07-25
Plataforma: android
PlanStatus: Approved
RetryCount: 0
ApprovedBy: qa-explorer (validación empírica en vivo, misma sesión)
ApprovalDate: 2026-07-25
ApprovalNotes: Flujo completo ejecutado y confirmado en dispositivo real (drawer -> Log In ->
  campos vacíos -> tap Login -> "Username is required" bajo Username, permanece en Login).
  0 pasos ❌. 1 .rs nuevo creado (lbl_usernameError.rs, resource-id nameErrorTV).
RejectionNotes: N/A
DispositivoExplorado: emulator-5554 (AVD "qa_android", sdk_gphone64_x86_64, Android 14)
ResolucionExplorada: 1080x2400 px

## Ticket / Test Cases

- Ticket_HU: SIM-6 — "Iniciar sesión con usuario y contraseña" (proyecto SIM, mismo ticket que
  SIM-TC-4/SIM-TC-5)
- TC_id a automatizar: SIM-TC-6 — "Login - Validación de campo Username obligatorio vacío"
- Run anterior del mismo ticket: `QA-20260725-login-sim6.md` (Phase: COMPLETED, cubrió
  SIM-TC-4 y SIM-TC-5; documentó explícitamente SIM-TC-6 como "fuera de alcance de este run").

## Punto de entrada (setUp)

- TC reutilizado: `CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.openLoginScreen'()`
  (ya existente, sin cambios) — deja la pantalla de Login con campos vacíos sin importar el
  estado de sesión previo.
- Motivo: es exactamente la misma precondición que ya resuelve `ensureOnLoginScreen()` para
  SIM-TC-4/SIM-TC-5 (abre drawer, hace logout si hace falta, navega a "Log In").
- Nada nuevo que crear en Page/Steps existentes salvo el método de assert (ver más abajo).

## Objetivo

- SIM-TC-6: validar que, con los campos Username y Password vacíos, presionar "Login" muestra
  el mensaje "Username is required" bajo el campo Username y no intenta autenticar (permanece
  en Login).

## Precondiciones

- App `com.saucelabs.mydemoapp.android` instalada en emulator-5554.
- Ninguna cuenta necesaria — el TC es puramente de validación de campo vacío.

## Datos de prueba

| Escenario | Username | Password |
|---|---|---|
| SIM-TC-6 (campo vacío) | (sin ingresar) | (sin ingresar) |

## Pasos validados en dispositivo (ejecutados en vivo, emulator-5554, sesión de hoy)

**Estado inicial**: `adb shell am force-stop com.saucelabs.mydemoapp.android` →
`adb shell monkey -p com.saucelabs.mydemoapp.android -c android.intent.category.LAUNCHER 1` →
abre en catálogo "Products" (sin sesión, confirmado force-stop cierra cualquier sesión previa
en memoria).

1. Tap `menuIV` (bounds `[32,154][111,233]`, content-desc "View menu") → abre drawer.
   → Pre-tap Wait: SmartWaitPage.waitVisible(btn_menuIcon, SHORT)
   → Post-tap Wait: SmartWaitPage.waitVisible(btn_logInMenuItem, SHORT)
2. Tap `itemTV` con content-desc "Login Menu Item" (bounds `[21,1679][709,1784]`) → navega a
   Login con campos `nameET`/`passwordET` vacíos (`text=""` confirmado por dump). ✅
   → Post-tap Wait: SmartWaitPage.waitVisible(input_username, MEDIUM)
3. Sin tocar ningún campo, tap `loginBtn` (bounds `[53,1349][1027,1475]`, content-desc "Tap to
   login with given credentials") → permanece en Login. ✅ Mensaje exacto observado:
   **"Username is required"**, visible bajo el campo Username (confirmado por screenshot y
   por dump XML, texto verbatim).

## Hallazgo — corrección de hipótesis inicial sobre el elemento de error

La hipótesis de partida (reutilizar `Object Repository/android/Login/lbl_loginError.rs`,
resource-id `passwordErrorTV`, ya usado por `LoginPage.verifyLockedOutError()` para SIM-TC-5)
**queda descartada**: el mensaje "Username is required" NO usa `passwordErrorTV`. El dump
confirma un elemento distinto:

```
com.saucelabs.mydemoapp.android:id/nameErrorTV -> 'Username is required'  bounds [53,881][1027,932]
```

Es decir, el campo Username tiene su propio label de error (`nameErrorTV`, debajo de
`nameET`), separado del label de error del campo Password (`passwordErrorTV`, que sigue siendo
el correcto para SIM-TC-5 "Sorry this user has been locked out."). Se necesita **un `.rs`
nuevo** (`lbl_usernameError`), no una reutilización directa de `lbl_loginError`.

## Componentes capturados (sin registrar .rs — pendiente de captura empírica por qa-explorer)

| Paso | pantalla | class | text | identifier (resource-id) | label/content-desc | bounds | .rs sugerido | locator preferido | locator respaldo |
|------|----------|-------|------|---------------------------|---------------------|--------|---------------|--------------------|--------------------|
| 3 | Login | TextView | "Username is required" | nameErrorTV | — | [53,881][1027,932] | lbl_usernameError | ANDROID_UI_AUTOMATOR | ATTRIBUTES |

Resto de elementos (`input_username`, `input_password`, `btn_login`) ya existen en
`Object Repository/android/Login/**` — no requieren nueva captura.

## Componentes validados empíricamente

| .rs sugerido | resource-id | content-desc | bounds reales | base_x (1080) | base_y (2340) | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|---|---|
| lbl_usernameError | nameErrorTV | — | [53,881][1027,932] | 540 | 908 | ❌ false (label, no clickable) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |

## Riesgos y bifurcaciones

- Ninguno nuevo — reutiliza el mismo patrón ya estable de `LoginPage`/`LoginSteps` (sesión no
  persistente entre relanzamientos de proceso, `ensureOnLoginScreen()` ya maneja el estado).
- El label `nameErrorTV` es exclusivo del campo Username; no confundir con `passwordErrorTV`
  (usado por SIM-TC-5) en ningún assert futuro que toque ambos campos vacíos a la vez.

## Cobertura mínima recomendada

- SIM-TC-6 (Borde/Regresión) — único TC de este run, complementa SIM-TC-4/SIM-TC-5 ya
  automatizados. Con esto, los 3 TCs de Login (SIM-6) documentados en AIO Tests quedan
  cubiertos.

## Criterios de aceptación

- [x] SIM-TC-6: con Username y Password vacíos, "Login" muestra "Username is required" bajo
      Username y permanece en la pantalla de Login (no autentica).

## Instrucciones para qa-test-creator

- setUp: reutilizar `LoginSteps.openLoginScreen()` tal cual (sin cambios).
- Keywords a reutilizar: `LoginSteps.openLoginScreen()`, `LoginSteps.tapLoginButton()` (esta
  última ya tolera campos vacíos porque solo hace `Mobile.tap()`, no valida contenido).
- Nuevos a crear:
  - `Object Repository/android/Login/lbl_usernameError.rs` (resource-id `nameErrorTV`)
  - Método nuevo en `LoginPage.groovy`: `verifyUsernameRequiredError()` (hermano de
    `verifyLockedOutError()` ya existente, mismo patrón `Mobile.verifyElementText`, mismo
    `FailureHandling.STOP_ON_FAILURE`, pero contra `lbl_usernameError` y el texto
    "Username is required").
  - Método nuevo en `LoginSteps.groovy`: `assertUsernameRequiredError()` (delega al de arriba).
  - `Scripts/android/Login/SIM-TC-6-loginUsernameObligatorio/Script<timestamp>.groovy`
  - `Test Cases/android/Login/SIM-TC-6-loginUsernameObligatorio.tc`
  - Naming: seguir el mismo patrón ya aplicado en el run de Checkout
    (`<KEY_PROYECTO>-TC-<ID>-<validación>`) — los TCs previos SIM-TC-4/SIM-TC-5 de este
    módulo no llevan el sufijo (inconsistencia previa, no repetirla en este TC nuevo).

## Instrucciones para qa-explorer

- Validación empírica ya realizada en esta misma sesión de planificación (screenshot + dump
  confirmando `nameErrorTV` con texto exacto "Username is required"). Puede reutilizar esta
  evidencia sin repetir la navegación completa; alcanza con crear el `.rs` nuevo siguiendo
  R-K1/R-K2/R-K3.

## Skills invocados

- qa-flow-planner: done (planificación + exploración empírica en vivo, corrigió hipótesis
  inicial sobre el elemento de error)
- qa-explorer (validar): done (aprobado, 0 pasos ❌)
- qa-explorer (capturar): done (1 .rs creado)
- qa-test-creator: done
- runner: passed (SIM-TC-6 individual y en secuencia --tag login junto con SIM-TC-4/SIM-TC-5)
- qa-debugger: n/a (cero ciclos de debug -- paso al primer intento)

Phase: COMPLETED

## Archivos generados

- Object Repository (1 .rs nuevo):
  - `Object Repository/android/Login/lbl_usernameError.rs`
- Page/Steps (métodos nuevos agregados a archivos existentes, sin archivos nuevos):
  - `Keywords/com/MyDemoApp/page/android/LoginPage.groovy` (+ `verifyUsernameRequiredError()`)
  - `Keywords/com/MyDemoApp/steps/android/LoginSteps.groovy` (+ `assertUsernameRequiredError()`)
- Scripts:
  - `Scripts/android/Login/SIM-TC-6-loginUsernameObligatorio/Script1785041361789.groovy`
- Test Cases:
  - `Test Cases/android/Login/SIM-TC-6-loginUsernameObligatorio.tc`

## Reporte final

✅ Pipeline completado — 1/1 TC PASSED, 0 ciclos de debug, 0 rechazos de plan.

Runner (individual): SIM-TC-6-loginUsernameObligatorio PASSED (8243ms)
Runner (secuencia --tag login, misma sesión Appium, junto con SIM-TC-4/SIM-TC-5 ya
existentes -- confirma cero regresiones): SIM-TC-4 PASSED (11951ms) ·
SIM-TC-6-loginUsernameObligatorio PASSED (10789ms) · SIM-TC-5 PASSED (10013ms)

Con este run, los 3 TCs de Login documentados en AIO Tests (SIM-6) quedan cubiertos:
SIM-TC-4 (éxito), SIM-TC-5 (bloqueado) y SIM-TC-6 (username vacío).
