package com.MyDemoApp.page.common

import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject

/**
 * Utilidades genericas reutilizables entre Pages de MyDemoApp
 * (no especificas de una sola pantalla).
 */
public class UtilsPage {

    /**
     * Chequea si un elemento esta visible sin fallar el test cuando no aparece
     * (usa FailureHandling.OPTIONAL internamente). Util para decidir un branch
     * segun el estado real de la pantalla -- por ejemplo, detectar si hay una
     * sesion activa antes de navegar a Login.
     *
     * @param obj TestObject a chequear
     * @param timeoutSec segundos maximos de espera antes de asumir que no esta
     * @return true si aparecio dentro del timeout, false si no (nunca lanza excepcion)
     */
    boolean isElementVisible(TestObject obj,
                              int timeoutSec) {
        return Mobile.waitForElementPresent(obj, timeoutSec, FailureHandling.OPTIONAL)
    }
}
