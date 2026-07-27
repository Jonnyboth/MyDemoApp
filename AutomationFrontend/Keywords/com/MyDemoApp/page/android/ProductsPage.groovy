package com.MyDemoApp.page.android

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Page Object del catalogo "Products" y del detalle de producto de MyDemoApp
 * (Android). Cubre unicamente lo necesario para llegar al carrito con 1 item:
 * seleccionar el primer producto del grid y agregarlo desde su detalle.
 */
public class ProductsPage {

    private static final String FIRST_PRODUCT_IMAGE = 'Object Repository/android/Products/btn_firstProductImage'
    private static final String PRODUCTS_CATALOG     = 'Object Repository/android/Products/lbl_productsCatalog'
    private static final String BTN_ADD_TO_CART      = 'Object Repository/android/ProductDetail/btn_addToCart'
    private static final String BTN_VIEW_CART        = 'Object Repository/android/Products/btn_viewCart'
    private static final String LBL_CART_ITEM_COUNT  = 'Object Repository/android/Products/lbl_cartItemCount'

    private static final int MAX_BACK_ATTEMPTS = 5

    /**
     * Deja la app en el catalogo "Products" sin importar en que pantalla haya
     * quedado el TC anterior (Cart, Checkout o Payment) -- el runner reutiliza
     * la misma sesion Appium entre TCs Android, igual que en el modulo Login.
     * Presiona "back" hasta MAX_BACK_ATTEMPTS veces, deteniendose apenas el
     * catalogo sea visible.
     *
     * Usa PRODUCTS_CATALOG (no FIRST_PRODUCT_IMAGE) para confirmar la pantalla:
     * el resource-id de FIRST_PRODUCT_IMAGE (productIV) se repite en las
     * imagenes de producto dentro de "My Cart", asi que chequear solo eso da
     * falso positivo si el TC anterior dejo la app en Cart con items -- se
     * detecta "un productIV visible" y se retorna sin haber vuelto al catalogo
     * real. PRODUCTS_CATALOG usa el content-desc del RecyclerView, exclusivo
     * del catalogo (Cart tiene el suyo propio con content-desc distinto).
     */
    @Keyword
    void ensureOnProductsScreen() {
        for (int attempt = 0; attempt < MAX_BACK_ATTEMPTS; attempt++) {
            if (SmartWaitPage.waitVisible(
                    findTestObject(PRODUCTS_CATALOG),
                    SmartWaitPage.SHORT,
                    com.kms.katalon.core.model.FailureHandling.OPTIONAL)) {
                return
            }
            Mobile.pressBack()
        }
        SmartWaitPage.waitVisible(findTestObject(PRODUCTS_CATALOG), SmartWaitPage.MEDIUM)
    }

    /**
     * Abre el detalle del primer producto del catalogo y lo agrega al carrito.
     * Asume que el catalogo "Products" ya esta visible (no navega desde Home).
     */
    @Keyword
    void addFirstProductToCart() {
        Mobile.tap(findTestObject(FIRST_PRODUCT_IMAGE), SmartWaitPage.MEDIUM)
        // Mobile.tap() ya espera el elemento internamente (findWithWait + click) --
        // un waitVisible() previo por separado solo agrega un hueco entre "encontrado"
        // y "tocado" donde la animacion de entrada del detalle de producto puede
        // hacer que el elemento desaparezca justo antes del tap.
        Mobile.tap(findTestObject(BTN_ADD_TO_CART), SmartWaitPage.MEDIUM)
        SmartWaitPage.waitVisible(findTestObject(BTN_VIEW_CART), SmartWaitPage.MEDIUM)
    }

    /** Toca el icono de carrito en el header y navega a "My Cart". */
    @Keyword
    void openCart() {
        Mobile.tap(findTestObject(BTN_VIEW_CART), SmartWaitPage.MEDIUM)
    }

    /**
     * Indica si el carrito tiene items pendientes (el badge numerico sobre el
     * icono de carrito esta visible). Asume que el catalogo "Products" ya
     * esta visible. Nunca falla el test -- solo informa para decidir un branch.
     */
    @Keyword
    boolean hasItemsInCart() {
        return SmartWaitPage.waitVisible(
            findTestObject(LBL_CART_ITEM_COUNT), SmartWaitPage.SHORT, FailureHandling.OPTIONAL)
    }
}
