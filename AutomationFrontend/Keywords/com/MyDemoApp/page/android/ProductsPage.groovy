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
    private static final String PRODUCT_TITLE_1      = 'Object Repository/android/Products/lbl_productTitle1'
    private static final String PRODUCT_TITLE_2      = 'Object Repository/android/Products/lbl_productTitle2'
    private static final String PRODUCT_TITLE_3      = 'Object Repository/android/Products/lbl_productTitle3'
    private static final String PRODUCT_TITLE_4      = 'Object Repository/android/Products/lbl_productTitle4'

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

    /**
     * Verifica el criterio de aceptacion de SIM-TC-16: el catalogo esta
     * ordenado alfabeticamente de forma ascendente (Name - Ascending) por
     * defecto. Compara cada titulo visible contra el siguiente
     * (case-insensitive) usando getVisibleProductTitlesInOrder(). Falla con
     * AssertionError si algun par esta desordenado, o si se leyeron menos de
     * 2 titulos (no hay nada que comparar -- no deberia pasar en un
     * dispositivo con al menos 1 fila completa visible). Asume que el
     * catalogo "Products" ya esta visible.
     */
    @Keyword
    void verifyDefaultSortIsNameAscending() {
        List<String> titles = getVisibleProductTitlesInOrder()
        if (titles.size() < 2) {
            throw new AssertionError('Se esperaban al menos 2 titulos de producto visibles '
                + 'para comparar el orden por defecto, se leyeron ' + titles.size()
                + ': ' + titles)
        }
        for (int i = 0; i < titles.size() - 1; i++) {
            String current = titles[i]
            String next = titles[i + 1]
            if (current.compareToIgnoreCase(next) > 0) {
                throw new AssertionError('Orden alfabetico ascendente roto: "' + current
                    + '" deberia ir antes que "' + next + '". Titulos leidos en orden: '
                    + titles)
            }
        }
    }

    // -- Privado -------------------------------------------------------

    /**
     * Lee el texto de los titulos de producto visibles sin scroll en el
     * catalogo (primeras 2 filas del grid, hasta 4 tarjetas), en el orden en
     * que aparecen en pantalla. Tolerante a dispositivos con menos filas
     * visibles: cada lectura usa FailureHandling.OPTIONAL, asi que si
     * instance(2) o instance(3) no estan presentes (pantalla mas chica),
     * simplemente se omiten de la lista en vez de fallar el test. Asume que
     * el catalogo "Products" ya esta visible.
     *
     * @return lista de titulos en el orden de aparicion (2 a 4 elementos)
     */
    private List<String> getVisibleProductTitlesInOrder() {
        List<String> titles = []
        List<String> titleObjectIds = [PRODUCT_TITLE_1, PRODUCT_TITLE_2, PRODUCT_TITLE_3, PRODUCT_TITLE_4]
        for (String objectId : titleObjectIds) {
            def titleObj = findTestObject(objectId)
            boolean visible = SmartWaitPage.waitVisible(
                titleObj, SmartWaitPage.SHORT, FailureHandling.OPTIONAL)
            if (visible) {
                titles.add(Mobile.getText(titleObj, SmartWaitPage.SHORT))
            }
        }
        return titles
    }
}
