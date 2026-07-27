import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-5 - Checkout (regresion bug SIM-10)
// Objetivo: valida que el campo obligatorio "Zip Code" recibe foco y es
//           editable mediante toque directo, sin que "City" intercepte la
//           interaccion ni pierda su propio valor.
// Datos: Full Name=QA Tester, Address Line 1=Calle Falsa 123, City=Bogota,
//        Zip Code=110111. Login: bod@example.com/10203040

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-3: Checkout - Zip Code recibe foco por toque directo (regresion SIM-10)')
Mobile.comment('PRECONDICION: reiniciar la app (cerrar + abrir) para partir de un estado base limpio')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()

Mobile.comment('STEP 1: Asegurar estado limpio en el catalogo Products')
CustomKeywords.'com.MyDemoApp.steps.android.ProductsSteps.ensureOnProductsScreen'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('STEP 2: Agregar el primer producto del catalogo al carrito')
CustomKeywords.'com.MyDemoApp.steps.android.ProductsSteps.addFirstProductToCart'()

Mobile.comment('STEP 3: Abrir "My Cart"')
CustomKeywords.'com.MyDemoApp.steps.android.ProductsSteps.openCart'()

Mobile.comment('STEP 4: Presionar "Proceed To Checkout"')
CustomKeywords.'com.MyDemoApp.steps.android.CartSteps.tapProceedToCheckout'()

Mobile.comment('STEP 5: Si no hay sesion activa, autenticar con bod@example.com')
if (CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.isLoginScreenShowing'()) {
    CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.enterBodCredentials'()
    CustomKeywords.'com.MyDemoApp.steps.android.LoginSteps.tapLoginButton'()
}
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.waitForCheckoutScreen'()

Mobile.comment('STEP 6: Llenar Full Name, Address 1, City, Zip Code y Country (orden obligatorio)')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.fillFormWithValidData'(
    'QA Tester', 'Calle Falsa 123', 'Bogota', '110111', 'Colombia')

Mobile.comment('ASSERT 1: Zip Code recibio el valor escrito y City conserva el suyo sin alteraciones')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.assertZipReceivedInputAndCityUnaltered'(
    'Bogota', '110111')
