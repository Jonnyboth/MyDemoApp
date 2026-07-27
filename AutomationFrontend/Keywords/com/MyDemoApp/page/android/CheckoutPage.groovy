package com.MyDemoApp.page.android

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.MyDemoApp.page.common.UtilsPage
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Page Object del formulario de direccion de "Checkout" y de la pantalla de
 * pago de MyDemoApp (Android). Cubre los 3 escenarios de SIM-5: envio exitoso
 * (SIM-TC-1), validacion de campos obligatorios vacios (SIM-TC-2) y foco del
 * campo Zip Code (SIM-TC-3, regresion del bug SIM-10).
 *
 * Nota de orden de campos (obligatoria, ver run QA-20260725-checkout-sim5):
 * llenar siempre Full Name, luego Address Line 1, luego City, luego Zip Code
 * y por ultimo Country -- y cerrar el teclado (Mobile.hideKeyboard()) justo
 * despues de escribir en cada campo, antes de tocar el siguiente. En la
 * sesion de validacion, tocar un campo distinto con el teclado en pantalla
 * todavia abierto hizo que el toque cayera sobre una tecla del teclado en vez
 * del EditText real, concatenando texto en el campo equivocado.
 */
public class CheckoutPage {

    private static final String LOGIN_USERNAME = 'Object Repository/android/Login/input_username'

    private static final String LBL_CHECKOUT_TITLE = 'Object Repository/android/Checkout/lbl_checkoutTitle'
    private static final String INPUT_FULL_NAME     = 'Object Repository/android/Checkout/input_fullName'
    private static final String INPUT_ADDRESS1      = 'Object Repository/android/Checkout/input_address1'
    private static final String INPUT_CITY          = 'Object Repository/android/Checkout/input_city'
    private static final String INPUT_ZIP           = 'Object Repository/android/Checkout/input_zip'
    private static final String INPUT_COUNTRY       = 'Object Repository/android/Checkout/input_country'
    private static final String BTN_TO_PAYMENT      = 'Object Repository/android/Checkout/btn_toPayment'

    private static final String LBL_FULL_NAME_ERROR = 'Object Repository/android/Checkout/lbl_fullNameError'
    private static final String LBL_ADDRESS1_ERROR  = 'Object Repository/android/Checkout/lbl_address1Error'
    private static final String LBL_CITY_ERROR      = 'Object Repository/android/Checkout/lbl_cityError'
    private static final String LBL_ZIP_ERROR       = 'Object Repository/android/Checkout/lbl_zipError'
    private static final String LBL_COUNTRY_ERROR   = 'Object Repository/android/Checkout/lbl_countryError'

    private static final String LBL_ENTER_PAYMENT_METHOD = 'Object Repository/android/Payment/lbl_enterPaymentMethod'

    private UtilsPage utils = new UtilsPage()

    /**
     * Indica si, tras tocar "Proceed To Checkout", la app redirigio a Login
     * (sin sesion activa) en vez de ir directo a Checkout. No falla el test
     * si no aparece -- es una verificacion opcional de branching.
     *
     * @return true si el campo Username de Login esta visible
     */
    @Keyword
    boolean isLoginScreenShowing() {
        return utils.isElementVisible(findTestObject(LOGIN_USERNAME), SmartWaitPage.SHORT)
    }

    /** Espera a que el formulario "Checkout" este visible. */
    @Keyword
    void waitForCheckoutScreen() {
        SmartWaitPage.waitVisible(findTestObject(LBL_CHECKOUT_TITLE), SmartWaitPage.MEDIUM)
    }

    /** Escribe un valor real en "Full Name" y cierra el teclado. */
    @Keyword
    void fillFullName(String value) {
        setFieldAndHideKeyboard(INPUT_FULL_NAME, value)
    }

    /** Escribe un valor real en "Address Line 1" y cierra el teclado. */
    @Keyword
    void fillAddress1(String value) {
        setFieldAndHideKeyboard(INPUT_ADDRESS1, value)
    }

    /** Escribe un valor real en "City" y cierra el teclado. */
    @Keyword
    void fillCity(String value) {
        setFieldAndHideKeyboard(INPUT_CITY, value)
    }

    /**
     * Escribe un valor real en "Zip Code" y cierra el teclado. Este es el
     * campo del bug SIM-10 (SIM-TC-3) -- debe llamarse solo despues de
     * fillCity(), nunca antes.
     */
    @Keyword
    void fillZip(String value) {
        setFieldAndHideKeyboard(INPUT_ZIP, value)
    }

    /** Escribe un valor real en "Country" y cierra el teclado. */
    @Keyword
    void fillCountry(String value) {
        setFieldAndHideKeyboard(INPUT_COUNTRY, value)
    }

    /** Presiona "To Payment" -- dispara la validacion de campos obligatorios. */
    @Keyword
    void tapToPayment() {
        Mobile.tap(findTestObject(BTN_TO_PAYMENT), SmartWaitPage.MEDIUM)
    }

    /**
     * Verifica los 5 mensajes de error exactos que la app muestra cuando se
     * presiona "To Payment" con el formulario vacio (SIM-TC-2). Los mensajes
     * de Zip Code y Country se muestran truncados en la UI -- se comparan
     * verbatim tal como aparecen, no es un error de captura.
     */
    @Keyword
    void assertAllValidationErrorsShown() {
        Mobile.verifyElementText(
            findTestObject(LBL_FULL_NAME_ERROR),
            'Please provide your full name.',
            FailureHandling.STOP_ON_FAILURE)
        Mobile.verifyElementText(
            findTestObject(LBL_ADDRESS1_ERROR),
            'Please provide your address.',
            FailureHandling.STOP_ON_FAILURE)
        Mobile.verifyElementText(
            findTestObject(LBL_CITY_ERROR),
            'Please provide your city.',
            FailureHandling.STOP_ON_FAILURE)
        Mobile.verifyElementText(
            findTestObject(LBL_ZIP_ERROR),
            'Please provide your zip',
            FailureHandling.STOP_ON_FAILURE)
        Mobile.verifyElementText(
            findTestObject(LBL_COUNTRY_ERROR),
            'Please provide your',
            FailureHandling.STOP_ON_FAILURE)
    }

    /**
     * Confirma el criterio de aceptacion de SIM-TC-3: "Zip Code" recibio
     * realmente el valor escrito (prueba indirecta de que tuvo foco de
     * teclado) y "City" conserva su valor original sin alteraciones.
     *
     * @param expectedCity valor que se escribio previamente en City
     * @param expectedZip valor que se escribio en Zip Code
     */
    @Keyword
    void assertZipReceivedInputAndCityUnaltered(String expectedCity,
                                                  String expectedZip) {
        String actualZip = Mobile.getText(findTestObject(INPUT_ZIP), SmartWaitPage.MEDIUM)
        String actualCity = Mobile.getText(findTestObject(INPUT_CITY), SmartWaitPage.MEDIUM)
        if (actualZip != expectedZip) {
            throw new AssertionError('Zip Code no recibio el valor esperado. Esperado: '
                + expectedZip + ' - Actual: ' + actualZip)
        }
        if (actualCity != expectedCity) {
            throw new AssertionError('City fue alterado por la escritura en Zip Code. Esperado: '
                + expectedCity + ' - Actual: ' + actualCity)
        }
    }

    /**
     * Confirma el criterio de aceptacion de SIM-TC-1: tras "To Payment" con
     * todos los campos completos, la app avanzo a "Enter a payment method"
     * sin ningun error de validacion.
     */
    @Keyword
    void assertAdvancedToPaymentScreen() {
        SmartWaitPage.waitVisible(findTestObject(LBL_ENTER_PAYMENT_METHOD), SmartWaitPage.MEDIUM)
    }

    // -- Privado -----------------------------------------------------------

    /** Toca el campo, escribe el valor y cierra el teclado antes de continuar. */
    private void setFieldAndHideKeyboard(String objectId, String value) {
        Mobile.setText(findTestObject(objectId), value, SmartWaitPage.MEDIUM)
        Mobile.hideKeyboard()
    }
}
