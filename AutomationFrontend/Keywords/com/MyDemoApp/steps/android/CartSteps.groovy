package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.CartPage

/**
 * Steps de negocio para la pantalla "My Cart" de MyDemoApp.
 */
public class CartSteps {

    private CartPage cartPage = new CartPage()

    /** Presiona "Proceed To Checkout" desde "My Cart". */
    @Keyword
    void tapProceedToCheckout() {
        cartPage.tapProceedToCheckout()
    }

    /** Remueve todos los productos de "My Cart" hasta dejarlo vacio. */
    @Keyword
    void emptyCart() {
        cartPage.emptyCart()
    }
}
