package com.kms.katalon.core.util

/**
 * Bridge de KeywordUtil de Katalon.
 *
 * markFailed()        → registra fallo, el test continúa (usa ThreadLocal)
 * markFailedAndStop() → lanza AssertionError, detiene el test
 * markWarning()       → log de advertencia, no falla
 * logInfo()           → log informativo
 */
class KeywordUtil {

    // Fallos acumulados por hilo — ScriptExecutor los lee al finalizar el test
    static final ThreadLocal<List<String>> FAILURES = ThreadLocal.withInitial { [] }

    static void logInfo(String message) {
        println "  [INFO]    ${message}"
    }

    static void markFailed(String message) {
        println "  [FAILED]  ${message}"
        FAILURES.get() << message
    }

    static void markFailedAndStop(String message) {
        println "  [FATAL]   ${message}"
        throw new AssertionError("[FATAL] ${message}")
    }

    static void markWarning(String message) {
        println "  [WARN]    ${message}"
    }

    static void markPassed(String message) {
        println "  [PASSED]  ${message}"
    }

    // ── Helpers para ScriptExecutor ──────────────────────────────────────────

    static boolean hasFailures() {
        !FAILURES.get().isEmpty()
    }

    static List<String> getFailures() {
        new ArrayList<>(FAILURES.get())
    }

    static void clearFailures() {
        FAILURES.get().clear()
    }
}
