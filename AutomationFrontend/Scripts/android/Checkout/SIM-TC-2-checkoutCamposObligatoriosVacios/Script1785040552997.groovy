import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-5 - Checkout
// Objetivo: valida que, con el formulario recien cargado (placeholders, sin
//           datos reales), presionar "To Payment" muestra los 5 mensajes de
//           error exactos y la app permanece en Checkout.
// Datos: ninguno -- el formulario debe quedar intacto (solo placeholders).
//        Login: bod@example.com/10203040

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-2: Checkout - validacion de campos obligatorios vacios')
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

Mobile.comment('STEP 6: Presionar "To Payment" sin ingresar ningun dato')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.tapToPayment'()

Mobile.comment('ASSERT 1: se muestran los 5 mensajes de error exactos y permanece en Checkout')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.assertAllValidationErrorsShown'()
