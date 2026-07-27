package com.kms.katalon.core.testcase

/**
 * Stub de TestCaseFactory de Katalon.
 * Los scripts la usan como: import static ...TestCaseFactory.findTestCase
 * Luego: findTestCase('Test Cases/android/openApp')
 */
class TestCaseFactory {

    static TestCaseEntity findTestCase(String path) {
        String name = path.split('/').last()
        return new TestCaseEntity(path: path, name: name)
    }
}
