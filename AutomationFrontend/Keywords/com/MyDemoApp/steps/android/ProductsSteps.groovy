package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.ProductsPage

/**
 * Steps de negocio para el catalogo "Products" y el detalle de producto.
 * Usado como precondicion de los TCs de Checkout (SIM-5): agrega 1 producto
 * al carrito antes de proceder a pagar.
 */
public class ProductsSteps {

    private ProductsPage productsPage = new ProductsPage()

    /**
     * Deja la app en el catalogo "Products", sin importar en que pantalla
     * haya quedado el TC anterior dentro de la misma sesion Appium.
     */
    @Keyword
    void ensureOnProductsScreen() {
        productsPage.ensureOnProductsScreen()
    }

    /** Agrega el primer producto del catalogo al carrito (Sauce Labs Backpack). */
    @Keyword
    void addFirstProductToCart() {
        productsPage.addFirstProductToCart()
    }

    /** Navega a "My Cart" desde el icono de carrito del header. */
    @Keyword
    void openCart() {
        productsPage.openCart()
    }
}
