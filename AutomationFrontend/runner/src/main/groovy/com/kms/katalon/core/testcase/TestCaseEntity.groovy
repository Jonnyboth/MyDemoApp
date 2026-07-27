package com.kms.katalon.core.testcase

/**
 * Stub de TestCaseEntity de Katalon.
 * Usado por findTestCase() y WebUI.callTestCase().
 */
class TestCaseEntity {
    String path   // "Test Cases/android/openApp"
    String name   // "openApp"

    @Override
    String toString() { "TestCase(${path})" }
}
