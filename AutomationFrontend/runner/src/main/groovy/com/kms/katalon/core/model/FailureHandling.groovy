package com.kms.katalon.core.model

/**
 * Bridge de FailureHandling de Katalon.
 * Controla el comportamiento del runner cuando un paso falla.
 */
enum FailureHandling {
    /** Lanza AssertionError inmediatamente — detiene el test. */
    STOP_ON_FAILURE,

    /** El paso falla silenciosamente — el test continúa. */
    OPTIONAL,

    /** Registra el fallo pero el test sigue ejecutándose hasta el final. */
    CONTINUE_ON_FAILURE
}
