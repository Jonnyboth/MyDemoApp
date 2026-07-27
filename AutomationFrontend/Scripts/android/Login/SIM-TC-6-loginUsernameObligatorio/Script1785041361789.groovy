import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-6 - Login con usuario y contrasena
// Objetivo: valida que, con Username y Password vacios, "Login" muestra
//           "Username is required" bajo Username y no intenta autenticar.
// Datos: ninguno (campos vacios)

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-6: Login - validacion de campo Username obligatorio vacio')
Mobile.comment('PRECONDICION: reiniciar la app (cerrar + abrir) para partir de un estado base limpio')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('STEP 1: Abrir menu lateral y navegar a Login (estado limpio garantizado)')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.openLoginScreen'()

Mobile.comment('STEP 2: Presionar Login sin ingresar usuario ni contrasena')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.tapLoginButton'()

Mobile.comment('ASSERT 1: se muestra "Username is required" bajo Username')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.assertUsernameRequiredError'()
