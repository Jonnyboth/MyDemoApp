package com.kms.katalon.core.testobject

/**
 * Bridge de TestObject de Katalon.
 *
 * Soporta dos modos de creación:
 *   1. Desde .rs XML   → ObjectRepositoryParser rellena `locators` map
 *   2. Dinámico        → LocatorHelper llama setSelectorMethod/setSelectorValue
 */
class TestObject {

    String objectId
    String name

    // Localizadores cargados desde .rs XML — key = estrategia, value = expresión
    Map<String, String> locators = [:]

    // Parámetros para interpolación en localizadores (p.ej. ${platform})
    Map<String, String> params = [:]

    // Para TestObjects creados dinámicamente por LocatorHelper
    SelectorMethod selectorMethod
    String         selectorValue

    // Constructor requerido por LocatorHelper: new TestObject("dynamic_id")
    TestObject(String objectId) {
        this.objectId = objectId
        this.name     = objectId
    }

    // Constructor para ObjectRepositoryParser
    TestObject(String objectId, String name, Map<String, String> locators) {
        this.objectId  = objectId
        this.name      = name
        this.locators  = locators ?: [:]
    }

    // ── API de Katalon que usan los Keywords ─────────────────────────────────

    String getObjectId() { objectId }

    void setSelectorMethod(SelectorMethod method) {
        this.selectorMethod = method
        syncDynamicLocator()
    }

    SelectorMethod getSelectorMethod() { selectorMethod }

    void setSelectorValue(String value) {
        this.selectorValue = value
        syncDynamicLocator()
    }

    String getSelectorValue() { selectorValue }

    /** Parámetros para localizadores con variables, ej: findTestObject(path, [key: val]) */
    TestObject setObjectParam(String paramName, Object value) {
        params[paramName] = value?.toString()
        return this
    }

    @Override
    String toString() { "TestObject(${objectId})" }

    // ── Privado ──────────────────────────────────────────────────────────────

    private void syncDynamicLocator() {
        if (selectorMethod && selectorValue) {
            locators[selectorMethod.name()] = selectorValue
        }
    }
}
