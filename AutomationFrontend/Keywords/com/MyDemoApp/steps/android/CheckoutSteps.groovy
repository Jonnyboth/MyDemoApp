package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.CheckoutPage

/**
 * Steps de negocio para "Checkout" (SIM-5): envio exitoso del formulario
 * (SIM-TC-1), validacion de campos obligatorios vacios (SIM-TC-2) y foco del
 * campo Zip Code (SIM-TC-3, regresion del bug SIM-10).
 */
public class CheckoutSteps {

    private CheckoutPage checkoutPage = new CheckoutPage()

    /** Indica si "Proceed To Checkout" redirigio a Login (sin sesion activa). */
    @Keyword
    boolean isLoginScreenShowing() {
        return checkoutPage.isLoginScreenShowing()
    }

    /** Espera a que el formulario "Checkout" este visible. */
    @Keyword
    void waitForCheckoutScreen() {
        checkoutPage.waitForCheckoutScreen()
    }

    /**
     * Llena los 5 campos del formulario con datos reales, en el orden
     * obligatorio (Full Name, Address Line 1, City, Zip Code, Country).
     */
    @Keyword
    void fillFormWithValidData(String fullName,
                                 String address1,
                                 String city,
                                 String zip,
                                 String country) {
        checkoutPage.fillFullName(fullName)
        checkoutPage.fillAddress1(address1)
        checkoutPage.fillCity(city)
        checkoutPage.fillZip(zip)
        checkoutPage.fillCountry(country)
    }

    /** Presiona "To Payment". */
    @Keyword
    void tapToPayment() {
        checkoutPage.tapToPayment()
    }

    /** Assert de SIM-TC-2: confirma los 5 mensajes de error exactos. */
    @Keyword
    void assertAllValidationErrorsShown() {
        checkoutPage.assertAllValidationErrorsShown()
    }

    /**
     * Assert de SIM-TC-3: confirma que Zip Code recibio el valor escrito
     * (foco de teclado real) y que City conserva su valor original.
     */
    @Keyword
    void assertZipReceivedInputAndCityUnaltered(String expectedCity, String expectedZip) {
        checkoutPage.assertZipReceivedInputAndCityUnaltered(expectedCity, expectedZip)
    }

    /** Assert de SIM-TC-1: confirma que avanzo a "Enter a payment method". */
    @Keyword
    void assertAdvancedToPaymentScreen() {
        checkoutPage.assertAdvancedToPaymentScreen()
    }
}
