package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.AppLifecyclePage

/**
 * Steps de ciclo de vida de la app MyDemoApp (Android).
 */
public class AppLifecycleSteps {

    private AppLifecyclePage appLifecyclePage = new AppLifecyclePage()

    /**
     * Cierra la app y borra TODOS sus datos a nivel de sistema Android (cache,
     * sesion, carrito, preferencias -- via `adb shell pm clear`), luego la
     * vuelve a abrir, para garantizar un punto de partida limpio al inicio de
     * cada test sin importar en que estado haya quedado un TC anterior. Las
     * keywords de Login se encargan de autenticar despues, solo si el test
     * lo requiere.
     */
    @Keyword
    void restartApp() {
        appLifecyclePage.restartApp()
    }
}
