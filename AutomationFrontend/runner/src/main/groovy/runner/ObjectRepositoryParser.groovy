package runner

import com.kms.katalon.core.testobject.TestObject

/**
 * Parsea archivos .rs (XML) del Object Repository de Katalon
 * y retorna un TestObject con todos los localizadores disponibles.
 *
 * Estructura XML esperada:
 *   <MobileElementEntity>
 *     <name>btn_account</name>
 *     <locatorCollection>
 *       <entry><key>ID</key><value>account_setting_tab</value></entry>
 *       <entry><key>XPATH</key><value>//android...</value></entry>
 *       <entry><key>ACCESSIBILITY</key><value></value></entry>
 *       <entry><key>ATTRIBUTES</key><value>//android...</value></entry>
 *       <entry><key>ANDROID_UI_AUTOMATOR</key><value></value></entry>
 *     </locatorCollection>
 *   </MobileElementEntity>
 */
class ObjectRepositoryParser {

    static TestObject parse(File rsFile) {
        if (!rsFile.exists()) {
            throw new FileNotFoundException("Archivo .rs no encontrado: ${rsFile.absolutePath}")
        }

        def xml = new XmlSlurper().parse(rsFile)

        // Leer locatorCollection: cada <entry> tiene <key> y <value> (formato MobileElementEntity)
        Map<String, String> locators = [:]
        xml.locatorCollection.entry.each { entry ->
            String key   = entry.key.text()?.trim()
            String value = entry.value.text()?.trim()
            if (key && value) {
                locators[key] = value
            }
        }

        // Fallback: selectorCollection (formato WebElementEntity — e.g. objetos creados desde Katalon Web/API)
        if (locators.isEmpty()) {
            xml.selectorCollection.entry.each { entry ->
                String key   = entry.key.text()?.trim()
                String value = entry.value.text()?.trim()
                if (key && value) {
                    locators[key] = value
                }
            }
        }

        // Fallback: webElementProperties name=xpath (formato WebElementEntity antiguo)
        if (locators.isEmpty()) {
            xml.webElementProperties.each { prop ->
                String propName = prop.name?.text()?.trim()?.toLowerCase()
                if (propName == 'xpath') {
                    String val = prop.value?.text()?.trim()
                    if (val) {
                        locators['XPATH'] = val
                        locators['ATTRIBUTES'] = val
                    }
                }
            }
        }

        // Fallback: si ATTRIBUTES no tiene valor pero el elemento <locator> sí lo tiene
        if (!locators['ATTRIBUTES']) {
            String mainLocator = xml.locator?.text()?.trim()
            if (mainLocator) locators['ATTRIBUTES'] = mainLocator
        }

        String name     = xml.name?.text()?.trim() ?: rsFile.name.replace('.rs', '')
        String objectId = rsFile.absolutePath

        return new TestObject(objectId, name, locators)
    }
}
