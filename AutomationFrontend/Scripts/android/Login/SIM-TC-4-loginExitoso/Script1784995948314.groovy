import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-6 - Login con usuario y contrasena
// Objetivo: valida que bod@example.com/10203040 inicie sesion y llegue al
//           catalogo, con el menu lateral mostrando "Log Out"
// Datos: bod@example.com / 10203040 (cuenta activa)

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-4: Login exitoso con credenciales validas (bod@example.com)')
Mobile.comment('PRECONDICION: reiniciar la app (cerrar + abrir) para partir de un estado base limpio')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()
Mobile.comment('PRECONDICION: reiniciar la app (cerrar + abrir) para partir de un estado base limpio')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('STEP 1: Abrir menu lateral y navegar a Login (estado limpio garantizado)')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.openLoginScreen'()

Mobile.comment('STEP 2: Escribir usuario y password de bod@example.com')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.enterBodCredentials'()

Mobile.comment('STEP 3: Presionar Login')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.tapLoginButton'()

Mobile.comment('ASSERT 1: Login exitoso - catalogo cargado y menu lateral muestra Log Out')
CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.assertLoginSuccessful'()
