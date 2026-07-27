package com.kms.katalon.core.testobject

import runner.ObjectRepositoryParser

/**
 * Bridge de ObjectRepository de Katalon.
 *
 * findTestObject("Object Repository/android/Home/btn_account") →
 *   lee el .rs XML del proyecto y retorna un TestObject con todos sus localizadores.
 */
class ObjectRepository {

    /** Inicializado por KatalonRunner antes de ejecutar cualquier test. */
    static File projectRoot

    static TestObject findTestObject(String path) {
        return findTestObject(path, [:])
    }

    static TestObject findTestObject(String path, Map<String, ?> params) {
        if (!projectRoot) {
            throw new IllegalStateException(
                "ObjectRepository.projectRoot no inicializado. " +
                "Asegúrate de que KatalonRunner.init() fue llamado antes."
            )
        }

        // La ruta ya viene sin extensión: "Object Repository/android/Home/btn_account"
        File rsFile = new File(projectRoot, "${path}.rs")
        if (!rsFile.exists()) {
            throw new FileNotFoundException(
                "Objeto no encontrado: ${path}.rs\n" +
                "Buscado en: ${rsFile.absolutePath}"
            )
        }

        TestObject obj = ObjectRepositoryParser.parse(rsFile)
        if (params) {
            obj.params = params.collectEntries { k, v -> [k.toString(), v?.toString()] }
        }
        return obj
    }
}
