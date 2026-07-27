package com.MyDemoApp.steps.android

import com.kms.katalon.core.annotation.Keyword
import com.MyDemoApp.page.android.AppLifecyclePage

/**
 * Steps de ciclo de vida de la app MyDemoApp (Android).
 */
public class AppLifecycleSteps {

    private AppLifecyclePage appLifecyclePage = new AppLifecyclePage()

    /**
     * Cierra y reabre la app, y vacia el carrito si quedo algun item de un
     * TC anterior, para garantizar un punto de partida limpio al inicio de
     * cada test. Las keywords de Login se encargan de autenticar despues,
     * solo si el test lo requiere.
     */
    @Keyword
    void restartApp() {
        appLifecyclePage.restartApp()
    }
}
