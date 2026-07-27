package com.MyDemoApp.page.android

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.MyDemoApp.page.common.UtilsPage
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Page Object de la pantalla Login y del drawer de Menu de MyDemoApp (Android).
 * No asume estado previo: ensureOnLoginScreen() detecta si hay sesion activa
 * (item "Log Out" visible) y cierra sesion antes de navegar a Login, para
 * garantizar que la pantalla llegue siempre con los campos vacios -- esto
 * permite correr SIM-TC-4 y SIM-TC-5 en cualquier orden dentro de la misma
 * sesion de Appium (el runner reutiliza la sesion entre test cases).
 */
public class LoginPage {

    private static final String MENU_ICON        = 'Object Repository/android/Menu/btn_menuIcon'
    private static final String LOGIN_ITEM       = 'Object Repository/android/Menu/btn_logInMenuItem'
    private static final String LOGOUT_ITEM      = 'Object Repository/android/Menu/btn_logOutMenuItem'
    private static final String CONFIRM_LOGOUT   = 'Object Repository/android/Menu/btn_confirmLogout'
    private static final String INPUT_USERNAME   = 'Object Repository/android/Login/input_username'
    private static final String INPUT_PASSWORD   = 'Object Repository/android/Login/input_password'
    private static final String BTN_LOGIN        = 'Object Repository/android/Login/btn_login'
    private static final String LBL_LOGIN_ERROR  = 'Object Repository/android/Login/lbl_loginError'
    private static final String LBL_USERNAME_ERROR = 'Object Repository/android/Login/lbl_usernameError'

    private static final String BOD_USERNAME     = 'bod@example.com'
    private static final String BOD_PASSWORD     = '10203040'
    private static final String ALICE_USERNAME   = 'alice@example.com'
    private static final String ALICE_PASSWORD   = '10203040'

    private UtilsPage utils = new UtilsPage()

    /**
     * Deja la app en la pantalla Login con los campos vacios, sin importar el
     * estado de sesion previo. Si detecta sesion activa (item "Log Out" en el
     * drawer), primero hace logout (tap "Log Out" + confirmar en el AlertDialog)
     * y vuelve a abrir el drawer antes de tocar "Log In".
     */
    @Keyword
    void ensureOnLoginScreen() {
        openDrawer()
        if (utils.isElementVisible(findTestObject(LOGOUT_ITEM), SmartWaitPage.SHORT)) {
            Mobile.tap(findTestObject(LOGOUT_ITEM), SmartWaitPage.MEDIUM)
            Mobile.tap(findTestObject(CONFIRM_LOGOUT), SmartWaitPage.MEDIUM)
            openDrawer()
        }
        Mobile.tap(findTestObject(LOGIN_ITEM), SmartWaitPage.MEDIUM)
        SmartWaitPage.waitVisible(findTestObject(INPUT_USERNAME), SmartWaitPage.MEDIUM)
    }

    /**
     * Indica si hay una sesion activa (item "Log Out" visible en el drawer).
     * Abre el drawer para chequear y lo vuelve a cerrar antes de retornar --
     * no deja rastro en la pantalla, es seguro usarlo como query aislada.
     */
    @Keyword
    boolean isLoggedIn() {
        openDrawer()
        boolean loggedIn = utils.isElementVisible(findTestObject(LOGOUT_ITEM), SmartWaitPage.SHORT)
        closeDrawer()
        return loggedIn
    }

    /**
     * Cierra la sesion activa desde el drawer (tap "Log Out" + confirmar en
     * el AlertDialog). Asume que ya hay sesion activa -- llamar solo despues
     * de confirmar isLoggedIn() == true.
     */
    @Keyword
    void logout() {
        openDrawer()
        Mobile.tap(findTestObject(LOGOUT_ITEM), SmartWaitPage.MEDIUM)
        Mobile.tap(findTestObject(CONFIRM_LOGOUT), SmartWaitPage.MEDIUM)
    }

    /**
     * Escribe usuario y password de "bod@example.com" (cuenta activa) en los
     * campos de texto de la pantalla Login. No usa el atajo de la app que
     * autocompleta los campos con el valor cacheado -- simula la escritura
     * real del usuario, que es el comportamiento correcto para este test.
     */
    @Keyword
    void typeBodCredentials() {
        typeCredentials(BOD_USERNAME, BOD_PASSWORD)
    }

    /**
     * Escribe usuario y password de "alice@example.com" (cuenta locked out)
     * en los campos de texto de la pantalla Login. No usa el atajo de la app
     * que autocompleta los campos con el valor cacheado.
     */
    @Keyword
    void typeAliceCredentials() {
        typeCredentials(ALICE_USERNAME, ALICE_PASSWORD)
    }

    /** Toca el boton "Login" con las credenciales ya escritas en los campos. */
    @Keyword
    void tapLoginButton() {
        Mobile.tap(findTestObject(BTN_LOGIN), SmartWaitPage.MEDIUM)
    }

    /**
     * Verifica que el login haya sido exitoso: abre el drawer y confirma que
     * el item "Log Out" esta visible (solo aparece con sesion activa). Falla
     * el test (STOP_ON_FAILURE) si no lo encuentra dentro del timeout.
     */
    @Keyword
    void verifyLoggedInMenuState() {
        Mobile.tap(findTestObject(MENU_ICON), SmartWaitPage.MEDIUM)
        SmartWaitPage.waitVisible(findTestObject(LOGOUT_ITEM), SmartWaitPage.MEDIUM)
    }

    /**
     * Verifica el mensaje de error de cuenta bloqueada bajo el campo password.
     * Compara el texto exacto (no solo la visibilidad del label), porque el
     * mismo label tambien se usa para otros errores de validacion (ej. usuario
     * vacio) y una comparacion parcial daria falsos positivos.
     */
    @Keyword
    void verifyLockedOutError() {
        Mobile.verifyElementText(
            findTestObject(LBL_LOGIN_ERROR),
            'Sorry this user has been locked out.',
            FailureHandling.STOP_ON_FAILURE)
    }

    /**
     * Verifica el mensaje de error de "Username" obligatorio (SIM-TC-6). Es un
     * label distinto al de LBL_LOGIN_ERROR (ese esta bajo Password) -- este
     * esta bajo el campo Username (resource-id nameErrorTV).
     */
    @Keyword
    void verifyUsernameRequiredError() {
        Mobile.verifyElementText(
            findTestObject(LBL_USERNAME_ERROR),
            'Username is required',
            FailureHandling.STOP_ON_FAILURE)
    }

    // -- Privado -----------------------------------------------------------

    /** Escribe usuario y password en sus campos de texto y cierra el teclado. */
    private void typeCredentials(String username, String password) {
        Mobile.setText(findTestObject(INPUT_USERNAME), username, SmartWaitPage.MEDIUM)
        Mobile.setText(findTestObject(INPUT_PASSWORD), password, SmartWaitPage.MEDIUM)
        Mobile.hideKeyboard()
    }

    /**
     * Toca el icono de menu para abrir el drawer lateral. Si un tap previo del
     * flujo dejo el drawer cerrado en vez de abierto (el icono actua como
     * toggle), reintenta una vez -- se detecta comprobando que "Log In" o
     * "Log Out" haya quedado visible tras el primer tap.
     */
    private void openDrawer() {
        Mobile.tap(findTestObject(MENU_ICON), SmartWaitPage.MEDIUM)
        boolean ready = utils.isElementVisible(findTestObject(LOGIN_ITEM), SmartWaitPage.SHORT) ||
                        utils.isElementVisible(findTestObject(LOGOUT_ITEM), SmartWaitPage.SHORT)
        if (!ready) {
            Mobile.tap(findTestObject(MENU_ICON), SmartWaitPage.MEDIUM)
        }
    }

    /**
     * Cierra el drawer lateral tocando fuera del panel (el "scrim" oscurecido).
     * El scrim lo maneja internamente el DrawerLayout -- no tiene resource-id
     * propio, por eso se usa una coordenada en vez de un locator. NO usar
     * Mobile.pressBack(): validado en el emulador que saca la app al Home de
     * Android en vez de cerrar el drawer (este DrawerLayout no lo intercepta).
     * Re-tocar el icono de menu tampoco sirve: con el drawer abierto, el icono
     * queda tapado por el propio panel. (900, 400) cae en zona vacia del
     * fragment_container -- fuera del panel del drawer (x <= 709) y antes del
     * primer producto del catalogo (y < 505) -- consistente en cualquier
     * pantalla que use este mismo header/drawer.
     */
    private void closeDrawer() {
        Mobile.tapAtPosition(900, 400)
    }
}
