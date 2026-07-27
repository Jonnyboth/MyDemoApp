package com.MyDemoApp.page.android

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Page Object de la pantalla "My Cart" de MyDemoApp (Android).
 */
public class CartPage {

    private static final String BTN_PROCEED_TO_CHECKOUT = 'Object Repository/android/Cart/btn_proceedToCheckout'
    private static final String BTN_REMOVE_ITEM          = 'Object Repository/android/Cart/btn_removeItem'

    private static final int MAX_REMOVE_ATTEMPTS = 10

    /**
     * Toca "Proceed To Checkout". La siguiente pantalla depende del estado de
     * sesion (Login si no hay sesion activa, Checkout directo si ya la hay)
     * -- esa decision la resuelve CheckoutSteps.isLoginScreenShowing(), no
     * esta clase.
     */
    @Keyword
    void tapProceedToCheckout() {
        Mobile.tap(findTestObject(BTN_PROCEED_TO_CHECKOUT), SmartWaitPage.MEDIUM)
    }

    /**
     * Toca "Remove Item" repetidamente hasta que el carrito quede vacio.
     * Asume que la pantalla "My Cart" ya esta visible. Cada tap elimina la
     * primera linea de producto restante -- si el carrito nunca queda vacio
     * tras MAX_REMOVE_ATTEMPTS intentos, deja de intentar (evita loop infinito
     * ante un estado inesperado de la app).
     */
    void emptyCart() {
        for (int attempt = 0; attempt < MAX_REMOVE_ATTEMPTS; attempt++) {
            boolean hasItem = SmartWaitPage.waitVisible(
                findTestObject(BTN_REMOVE_ITEM), SmartWaitPage.SHORT, FailureHandling.OPTIONAL)
            if (!hasItem) {
                return
            }
            Mobile.tap(findTestObject(BTN_REMOVE_ITEM), SmartWaitPage.MEDIUM)
        }
    }
}
