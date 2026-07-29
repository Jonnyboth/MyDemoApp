package com.MyDemoApp.page.common

import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import org.openqa.selenium.StaleElementReferenceException

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

    /**
     * Igual que Mobile.tap(), pero resiliente a StaleElementReferenceException:
     * Appium puede reutilizar la referencia del elemento encontrado en un tap
     * anterior sobre el mismo locator, y esa referencia queda invalida cuando
     * la pantalla cambio entremedio (ej. re-tocar el mismo icono de menu antes
     * y despues de una transicion de pantalla, como antes/despues de un login).
     * No se captura StaleElementReferenceException directo porque cada motor
     * la reporta distinto: Katalon Studio real la envuelve en
     * com.kms.katalon.core.exception.StepFailedException (clase que no existe
     * en el runner headless, que en cambio la deja pasar cruda) -- por eso se
     * recorre la cadena de causas de forma generica, sin acoplarse a ninguna
     * clase de excepcion especifica del motor. Reintenta una unica vez
     * forzando un lookup fresco del elemento; cualquier otro fallo se relanza
     * tal cual.
     */
    void tapSafely(TestObject obj, int timeoutSec) {
        try {
            Mobile.tap(obj, timeoutSec)
        } catch (Exception e) {
            if (!causedByStaleElement(e)) {
                throw e
            }
            Mobile.tap(obj, timeoutSec)
        }
    }

    private boolean causedByStaleElement(Throwable e) {
        for (Throwable current = e; current != null; current = current.cause) {
            if (current instanceof StaleElementReferenceException) {
                return true
            }
        }
        return false
    }
}
