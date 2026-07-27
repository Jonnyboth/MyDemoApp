import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-6 - Login con usuario y contrasena
// Objetivo: valida que alice@example.com (locked out) sea rechazada con el
//           mensaje "Sorry this user has been locked out." y no navegue
// Datos: alice@example.com / 10203040 (cuenta bloqueada)

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-5: Bloqueo de login para cuenta locked out (alice@example.com)')
Mobile.comment('PRECONDICION: reiniciar la app (cerrar + abrir) para partir de un estado base limpio')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('STEP 1: Abrir menu lateral y navegar a Login (estado limpio garantizado)')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.openLoginScreen'()

Mobile.comment('STEP 2: Escribir usuario y password de alice@example.com (locked out)')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.enterAliceCredentials'()

Mobile.comment('STEP 3: Presionar Login')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.tapLoginButton'()

Mobile.comment('ASSERT 1: Mensaje "Sorry this user has been locked out." visible, sin navegar')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.assertLockedOutError'()
