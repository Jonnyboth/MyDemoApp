import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-5 - Checkout
// Objetivo: valida que, con todos los campos obligatorios completos con datos
//           reales, el formulario de Checkout avanza a la pantalla de pago
//           sin errores de validacion.
// Datos: Full Name=QA Tester, Address Line 1=Calle Falsa 123, City=Bogota,
//        Zip Code=110111, Country=Colombia. Login: bod@example.com/10203040

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-1: Checkout - envio exitoso del formulario de direccion')
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

Mobile.comment('STEP 6: Llenar el formulario con datos reales (Full Name, Address 1, City, Zip, Country)')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.fillFormWithValidData'(
    'QA Tester', 'Calle Falsa 123', 'Bogota', '110111', 'Colombia')

Mobile.comment('STEP 7: Presionar "To Payment"')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.tapToPayment'()

Mobile.comment('ASSERT 1: avanzo a "Enter a payment method" sin errores de validacion')
CustomKeywords.'com.MyDemoApp.steps.android.CheckoutSteps.assertAdvancedToPaymentScreen'()
