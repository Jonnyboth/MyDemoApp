# Flow Context - QA-20260725-login-sim6 - Login MyDemoApp (SIM-6)

Fecha: 2026-07-25
Plataforma: android
PlanStatus: Approved
RetryCount: 0
ApprovedBy: qa-explorer (validación empírica en vivo, misma sesión)
ApprovalDate: 2026-07-25
ApprovalNotes: Ambos flujos (login exitoso y login bloqueado) ejecutados y confirmados en dispositivo real antes de generar código. Cero pasos ❌.
RejectionNotes: N/A
DispositivoExplorado: emulator-5554 (AVD "qa_android", Pixel 6, API 34)
ResolucionExplorada: 1080x2400 px

## Ticket / Test Cases

- Ticket_HU: SIM-6 — "Iniciar sesión con usuario y contraseña" (proyecto SIM)
- TC_id automatizados: SIM-TC-4 (Login exitoso), SIM-TC-5 (Login bloqueado)
- Fuente de los TC (AIO Tests / `aio-tests-mcp` no autorizado en esta sesión — se usó por
  error el MCP `smartbear-zephyr`, que no es la herramienta de este proyecto: 401 inválido.
  Corregido para runs futuros — ver `aio-tests-mcp` en la sección de pipeline vigente):
  `FuncionalQaPm/docs/QaExplorer/pending_aio_test_cases/SIM_test_cases.json` (índices 3 y 4,
  confirmados contra el orden 1-11 documentado en `pending_aio_test_cases/README.md`, tabla
  de cobertura filas #4 y #5).
- Exploración previa reutilizada: `FuncionalQaPm/docs/QaExplorer/Android/MyDemoApp/Autenticacion/Login/Login.md`

## Punto de entrada (setUp)

- TC reutilizado: ninguno (proyecto Katalon vacío tras "Limpiar AutomationFrontend" — no existe
  setUp para MyDemoApp todavía; el catálogo `openApp`/`OpenStoreGeant` es específico de TuEmpresa
  y no aplica).
- Motivo: primer test de este proyecto para MyDemoApp. El flujo arranca desde app cerrada
  (`am force-stop` + `am start` sobre `SplashActivity`, confirmado que abre en el catálogo
  "Products", NO en Login directo).
- Nuevo setUp creado: apertura de app + navegación Home → menú lateral → "Log In", inline en
  `LoginPage`/`LoginSteps` (no se crea test case setUp separado; alcance mínimo para 2 TCs).

## Objetivo

- SIM-TC-4: Validar que un usuario con cuenta activa (bod@example.com/10203040) inicia sesión
  correctamente y accede al catálogo, con el menú lateral mostrando "Log Out".
- SIM-TC-5: Validar que una cuenta bloqueada (alice@example.com/10203040) es rechazada con el
  mensaje "Sorry this user has been locked out." y permanece en Login.

## Precondiciones

- App `com.saucelabs.mydemoapp.android` instalada en emulator-5554.
- Cuenta bod@example.com activa (no bloqueada), password 10203040.
- Cuenta alice@example.com marcada como locked out, password 10203040.

## Datos de prueba

| Escenario | Usuario | Password |
|---|---|---|
| Login exitoso (SIM-TC-4) | bod@example.com | 10203040 |
| Login bloqueado (SIM-TC-5) | alice@example.com | 10203040 |

## Pasos validados en dispositivo (ejecutados en vivo, emulator-5554)

**Estado inicial común**: `adb shell am force-stop com.saucelabs.mydemoapp.android` →
`am start -n .../SplashActivity` → abre en catálogo "Products".

**SIM-TC-4 — Login exitoso**
1. Tap `menuIV` (☰) → abre drawer. ✅ (dump confirmó `itemTV` con `content-desc="Login Menu Item"`, text="Log In")
2. Tap item "Log In" (bounds [21,1679][709,1784] en el drawer) → navega a pantalla Login (campos vacíos). ✅
3. Tap `username1TV` ("bod@example.com", bounds [85,1690][713,1734]) → autocompleta `nameET`="bod@example.com" y `passwordET`="••••••••". ✅ (confirmado por screenshot)
4. Tap `loginBtn` (bounds [53,1349][1027,1475]) → navega al catálogo "Products". ✅
5. Tap `menuIV` de nuevo → dump confirma `itemTV` con `content-desc="Logout Menu Item"`, text="Log Out". ✅ (criterio de aceptación cumplido)

**SIM-TC-5 — Login bloqueado**
1. Reset a estado inicial (force-stop + relaunch) → catálogo "Products".
2. Tap `menuIV` → tap "Log In" → pantalla Login vacía. ✅
3. Tap `username2TV` ("alice@example.com (locked out)", bounds [85,1766][713,1810]) → autocompleta `nameET`="alice@example.com (locked out)", `passwordET`="••••••••". ✅
4. Tap `loginBtn` → permanece en Login; `passwordErrorTV` muestra texto exacto
   "Sorry this user has been locked out." ✅ (screenshot confirmado, criterio de aceptación cumplido)

## Componentes validados empíricamente (dump real, emulator-5554)

| .rs sugerido | resource-id | content-desc | bounds reales | tap_validated | estrategia_primaria | fallback |
|---|---|---|---|---|---|---|
| btn_menuIcon | menuIV | View menu | [32,154][111,233] | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| btn_logInMenuItem | itemTV (compartido — desambiguar por content-desc) | Login Menu Item | [21,1679][709,1784] | ✅ true | ACCESSIBILITY | ATTRIBUTES (xpath por content-desc) |
| btn_logOutMenuItem | itemTV (compartido — desambiguar por content-desc) | Logout Menu Item | [21,1679][709,1784] | ✅ true | ACCESSIBILITY | ATTRIBUTES (xpath por content-desc) |
| input_username | nameET | (sin content-desc) | [53,758][1027,876] | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| input_password | passwordET | (sin content-desc) | [53,1024][1027,1142] | ✅ true (foco confirmado) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |
| btn_login | loginBtn | Tap to login with given credentials | [53,1349][1027,1475] | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |
| btn_userBod | username1TV | Tap to use this username for login (NO único — repetido en username2TV) | [85,1690][713,1734] | ✅ true | ANDROID_UI_AUTOMATOR (resource-id único) | ATTRIBUTES |
| btn_userAlice | username2TV | Tap to use this username for login (NO único) | [85,1766][713,1810] | ✅ true | ANDROID_UI_AUTOMATOR (resource-id único) | ATTRIBUTES |
| lbl_loginError | passwordErrorTV | (sin content-desc) | [53,1158][1027,1202] | ❌ false (label, no clickable) | ANDROID_UI_AUTOMATOR | ATTRIBUTES |

Nota: `btn_userBod`/`btn_userAlice` NO usan ACCESSIBILITY como estrategia poblada porque el
`content-desc` es idéntico entre ambos elementos (ambiguo) — se documenta como
`<!-- NOT AVAILABLE: content-desc no único entre username1TV/2TV -->`. `resource-id` sí es
único por elemento, por eso es la estrategia primaria.

## Riesgos y bifurcaciones

- El mensaje en `lbl_loginError` (`passwordErrorTV`) es el mismo elemento que muestra tanto
  "Sorry this user has been locked out." como "Username is required" (validación de campo
  vacío, TC no incluido en este alcance: SIM-TC-6). La Steps class valida el texto exacto
  esperado por escenario, no solo la visibilidad del label.
- Sesión no persistente entre relanzamientos de proceso (confirmado en Login.md) — cada Script
  hace su propio ciclo `force-stop`/`start` + navegación completa, no asume estado previo.

## Cobertura mínima recomendada

- SIM-TC-4 (Positivo/Smoke) y SIM-TC-5 (Negativo/Regresión) — ambos cubiertos end-to-end.
- Fuera de alcance explícito de este run: SIM-TC-6 (validación de campo Username vacío) — no
  fue pedido por el usuario.

## Criterios de aceptación

- [x] SIM-TC-4: tras login con bod@example.com/10203040, navega a catálogo y el menú muestra "Log Out"
- [x] SIM-TC-5: tras login con alice@example.com/10203040, permanece en Login y muestra
      "Sorry this user has been locked out."

## Instrucciones para qa-test-creator

- setUp: ninguno reutilizable — crear ciclo app-launch inline en cada Script
  (`Mobile.startApplication`/`Mobile.startExistingApplication` + `force-stop` previo vía Script
  no es posible desde Groovy Katalon puro; usar `Mobile.startApplication(G_AppBundleID, true)`
  con `Mobile.closeApplication()` en postcondición de cada TC para garantizar estado limpio
  entre corridas).
- Keywords a reutilizar: ninguno existe aún — crear `tuempresa.utils.SmartWaitPage`,
  `com.tuempresa.page.common.UtilsPage` (mínimos, alcance de este run).
- Nuevos a crear:
  - `Object Repository/android/Menu/*.rs`, `Object Repository/android/Login/*.rs`
  - `Keywords/com/tuempresa/page/android/LoginPage.groovy`
  - `Keywords/com/tuempresa/steps/android/LoginSteps.groovy`
  - `Scripts/android/Login/SIM-TC-4/Script<timestamp>.groovy`
  - `Scripts/android/Login/SIM-TC-5/Script<timestamp>.groovy`
  - `Test Cases/android/Login/SIM-TC-4.tc`
  - `Test Cases/android/Login/SIM-TC-5.tc`

## Skills invocados

- qa-flow-planner: done (planificación reutilizando exploración previa + datos del usuario)
- qa-explorer (validar): done (validación empírica en vivo, ambos flujos, 0 pasos ❌)
- qa-explorer (capturar): done (9 .rs creados, ver "Archivos generados")
- qa-test-creator: done
- runner: passed (SIM-TC-4 y SIM-TC-5, individual y en secuencia)
- qa-debugger: n/a (cero ciclos de debug — ambos TCs pasaron al primer intento)

Phase: COMPLETED

## Archivos generados

- Object Repository (9 .rs):
  - `Object Repository/android/Menu/btn_menuIcon.rs`
  - `Object Repository/android/Menu/btn_logInMenuItem.rs`
  - `Object Repository/android/Menu/btn_logOutMenuItem.rs`
  - `Object Repository/android/Menu/btn_confirmLogout.rs`
  - `Object Repository/android/Login/input_username.rs`
  - `Object Repository/android/Login/input_password.rs`
  - `Object Repository/android/Login/btn_login.rs`
  - `Object Repository/android/Login/btn_userBod.rs`
  - `Object Repository/android/Login/btn_userAlice.rs`
  - `Object Repository/android/Login/lbl_loginError.rs`
- Utils (nuevos, alcance mínimo — proyecto estaba vacío):
  - `Keywords/tuempresa/utils/SmartWaitPage.groovy`
  - `Keywords/com/tuempresa/page/common/UtilsPage.groovy`
- Page/Steps:
  - `Keywords/com/tuempresa/page/android/LoginPage.groovy`
  - `Keywords/com/tuempresa/steps/android/LoginSteps.groovy`
- Scripts:
  - `Scripts/android/Login/SIM-TC-4/Script1784995948314.groovy`
  - `Scripts/android/Login/SIM-TC-5/Script1784995949316.groovy`
- Test Cases:
  - `Test Cases/android/Login/SIM-TC-4.tc`
  - `Test Cases/android/Login/SIM-TC-5.tc`

## Reporte final

✅ Pipeline completado — 2/2 TCs PASSED, 0 ciclos de debug, 0 rechazos de plan.

Runner (individual): SIM-TC-4 PASSED (13264ms) · SIM-TC-5 PASSED (7194ms)
Runner (secuencia --tag login, misma sesión Appium): SIM-TC-4 PASSED (11114ms) · SIM-TC-5 PASSED (7313ms)

Nota de diseño: la sesión Appium del runner es singleton y se reutiliza entre TCs Android
dentro de una misma invocación (`AppiumDriverManager`, ver `KatalonRunner.ensureDriverForPlatform`).
Por eso `LoginPage.ensureOnLoginScreen()` detecta el estado real (sesión activa/inactiva vía
presencia del item "Log Out") antes de navegar, en vez de asumir un estado inicial fijo — validado
explícitamente ejecutando ambos TCs en los tres órdenes posibles (solo TC-4, solo TC-5, TC-4→TC-5).
Por el mismo motivo, los Scripts NO llaman `Mobile.closeApplication()` en postcondición (romperia
el siguiente TC de la misma corrida).

Pendiente fuera de alcance de este run (no solicitado por el usuario):
- SIM-TC-6 (validación de campo Username obligatorio) — mismo módulo, no incluido en el pedido.
- Gate R-K5 (Katalon Studio Problems panel = 0 errors) — requiere abrir el proyecto en Katalon
  Studio Desktop; no aplica al loop autónomo headless per `qa-test-creator/manifest.yaml`.
