package MyDemoApp.utils

import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject

/**
 * Esperas estandarizadas para reemplazar Mobile.delay() por esperas basadas
 * en la aparicion real del elemento, en vez de temporizadores fijos.
 */
public class SmartWaitPage {

    static final int SHORT  = 5   // chequeos rapidos, ej. detectar un estado opcional
    static final int MEDIUM = 10  // default para taps y navegacion normal
    static final int LONG   = 20  // pantallas con carga de red pesada

    /**
     * Espera a que un elemento este presente en el arbol de accesibilidad.
     * A diferencia de Mobile.delay(), no duerme un tiempo fijo: retorna en
     * cuanto el elemento aparece, y solo espera hasta timeoutSec en el peor caso.
     *
     * @param obj TestObject a esperar (resultado de findTestObject(...))
     * @param timeoutSec segundos maximos de espera (default MEDIUM)
     * @param fh que hacer si nunca aparece: STOP_ON_FAILURE lanza AssertionError,
     *           CONTINUE_ON_FAILURE marca el test fallido y sigue, OPTIONAL solo retorna false
     * @return true si el elemento aparecio dentro del timeout, false si no
     *         (excepto con STOP_ON_FAILURE, que lanza excepcion en vez de retornar false)
     */
    static boolean waitVisible(TestObject obj, 
                                int timeoutSec = MEDIUM,
                                FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        return Mobile.waitForElementPresent(obj, timeoutSec, fh)
    }
}
