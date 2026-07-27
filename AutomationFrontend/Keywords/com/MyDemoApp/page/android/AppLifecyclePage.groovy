package com.MyDemoApp.page.android

import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Ciclo de vida de la app MyDemoApp (Android): cerrar + reabrir para dejarla
 * en un estado base limpio antes de cada test (sin sesion activa, sin items
 * en el carrito). No importa `internal.GlobalVariable` -- Katalon puede
 * compilar este Keyword antes que `internal/GlobalVariable.groovy` y lanzar
 * ClassNotFoundException; por eso se accede a G_AppBundleID via reflection
 * en runtime (ver appBundleId()) en vez de un import estatico arriba.
 */
public class AppLifecyclePage {

    private static final String MENU_ICON = 'Object Repository/android/Menu/btn_menuIcon'

    private ProductsPage productsPage = new ProductsPage()
    private CartPage cartPage = new CartPage()
    private LoginPage loginPage = new LoginPage()

    /**
     * Cierra la app (mata el proceso) y la vuelve a abrir desde cero, luego
     * evalua sesion y carrito para dejarla en el estado base esperado por
     * cada test. closeApplication() exige una sesion Appium ya activa -- si
     * este es el primer test que corre en la sesion actual de Katalon Studio
     * (sin driver todavia), lanza "No driver found"; en ese caso no hay nada
     * que cerrar, asi que el fallo se ignora y startExistingApplication()
     * sigue siendo quien crea la sesion si hace falta.
     */
    void restartApp() {
        try {
            Mobile.closeApplication()
        } catch (Exception e) {
            // Sin sesion previa (primer test de la corrida) -- no hay app que cerrar.
        }
        Mobile.startExistingApplication(appBundleId())
        SmartWaitPage.waitVisible(findTestObject(MENU_ICON), SmartWaitPage.MEDIUM)

        productsPage.ensureOnProductsScreen()
        prepareCleanState()
    }

    // -- Privado -----------------------------------------------------------

    /**
     * Reglas de negocio para el estado base de cada test (sesion + carrito):
     *
     *   | Carrito  | Sesion    | Accion                                |
     *   |----------|-----------|----------------------------------------|
     *   | vacio    | inactiva  | continuar (ya esta en el estado base)  |
     *   | vacio    | activa    | logout                                 |
     *   | con items| activa    | vaciar carrito + logout                |
     *   | con items| inactiva  | continuar (sin sesion no hay que tocar)|
     *
     * Sin sesion activa nunca hay nada que hacer -- por eso una unica guarda
     * temprana ("si no hay sesion, retornar") cubre 2 de las 4 combinaciones
     * sin anidar if/else. Con sesion activa, el carrito se vacia primero
     * (si tiene items) y despues siempre se cierra la sesion.
     */
    private void prepareCleanState() {
        if (!loginPage.isLoggedIn()) {
            return
        }
        if (productsPage.hasItemsInCart()) {
            productsPage.openCart()
            cartPage.emptyCart()
            Mobile.pressBack()
            productsPage.ensureOnProductsScreen()
        }
        loginPage.logout()
    }

    /**
     * internal.GlobalVariable tiene forma distinta segun donde corra el test:
     * - Katalon Studio real: clase regenerada por el IDE con campos estaticos
     *   publicos (`public static Object G_AppBundleID`).
     * - Runner headless propio: Map interno expuesto solo via getAll().
     * Se prueba primero el campo (entorno real) y se cae a getAll() si no
     * existe (runner propio), para que el mismo Keyword funcione en ambos.
     */
    private String appBundleId() {
        Class globalVariableClass = Class.forName('internal.GlobalVariable')
        try {
            return globalVariableClass.getField('G_AppBundleID').get(null)
        } catch (NoSuchFieldException ignored) {
            Map<String, Object> vars = globalVariableClass.getMethod('getAll').invoke(null)
            return vars.get('G_AppBundleID')
        }
    }
}
