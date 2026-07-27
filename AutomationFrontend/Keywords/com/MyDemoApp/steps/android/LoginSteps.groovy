package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.LoginPage

/**
 * Steps de negocio para Login (SIM-6): login exitoso (SIM-TC-4) y bloqueo por
 * cuenta locked out (SIM-TC-5). Orquesta LoginPage -- los Scripts solo llaman
 * estos metodos via CustomKeywords, nunca Mobile.* ni findTestObject() directo.
 */
public class LoginSteps {

    private LoginPage loginPage = new LoginPage()

    /** Navega a la pantalla Login con los campos vacios, sin asumir sesion previa. */
    @Keyword
    void openLoginScreen() {
        loginPage.ensureOnLoginScreen()
    }

    /** Indica si hay una sesion activa (item "Log Out" visible en el drawer). */
    @Keyword
    boolean isLoggedIn() {
        return loginPage.isLoggedIn()
    }

    /** Cierra la sesion activa desde el drawer. Asume que ya hay sesion activa. */
    @Keyword
    void logout() {
        loginPage.logout()
    }

    /** Escribe las credenciales de bod@example.com (cuenta activa) en los campos de Login. */
    @Keyword
    void enterBodCredentials() {
        loginPage.typeBodCredentials()
    }

    /** Escribe las credenciales de alice@example.com (cuenta locked out) en los campos de Login. */
    @Keyword
    void enterAliceCredentials() {
        loginPage.typeAliceCredentials()
    }

    /** Presiona el boton Login con las credenciales ya escritas en los campos. */
    @Keyword
    void tapLoginButton() {
        loginPage.tapLoginButton()
    }

    /** Assert de SIM-TC-4: confirma que el login fue exitoso (menu muestra "Log Out"). */
    @Keyword
    void assertLoginSuccessful() {
        loginPage.verifyLoggedInMenuState()
    }

    /** Assert de SIM-TC-5: confirma el mensaje exacto de cuenta bloqueada. */
    @Keyword
    void assertLockedOutError() {
        loginPage.verifyLockedOutError()
    }

    /** Assert de SIM-TC-6: confirma "Username is required" con campos vacios. */
    @Keyword
    void assertUsernameRequiredError() {
        loginPage.verifyUsernameRequiredError()
    }
}
