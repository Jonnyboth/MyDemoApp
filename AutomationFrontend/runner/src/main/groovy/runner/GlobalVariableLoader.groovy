package runner

/**
 * Parsea archivos .glbl (XML) de Katalon y retorna un Map con las variables.
 *
 * Estructura XML:
 *   <GlobalVariableEntities>
 *     <GlobalVariableEntity>
 *       <name>G_Platform</name>
 *       <initValue>'android'</initValue>
 *     </GlobalVariableEntity>
 *     ...
 *   </GlobalVariableEntities>
 */
class GlobalVariableLoader {

    static Map<String, Object> load(File glblFile) {
        if (!glblFile.exists()) {
            println "[Runner] Advertencia: ${glblFile} no encontrado — usando variables vacías"
            return [:]
        }

        def xml  = new XmlSlurper().parse(glblFile)
        Map<String, Object> vars = [:]

        xml.GlobalVariableEntity.each { entity ->
            String name  = entity.name.text()?.trim()
            String raw   = entity.initValue.text()?.trim()
            if (name) {
                vars[name] = parseValue(raw)
            }
        }

        println "[Runner] GlobalVariables cargadas: ${vars.keySet().join(', ')}"
        return vars
    }

    /**
     * Convierte el valor raw de Groovy (con comillas) al tipo Java correcto.
     * Ejemplos:
     *   'android'     → "android"  (String)
     *   'true'        → true       (String, no boolean — Katalon usa strings)
     *   ''            → ""
     *   123           → 123L
     */
    private static Object parseValue(String raw) {
        if (!raw) return ''

        // String con comillas simples: 'valor'
        if (raw.startsWith("'") && raw.endsWith("'") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1)
        }

        // String con comillas dobles: "valor"
        if (raw.startsWith('"') && raw.endsWith('"') && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1)
        }

        // Booleano
        if (raw == 'true')  return true
        if (raw == 'false') return false
        if (raw == 'null')  return null

        // Número entero
        try { return Long.parseLong(raw) } catch (NumberFormatException ignored) {}

        // Número decimal
        try { return Double.parseDouble(raw) } catch (NumberFormatException ignored) {}

        // Devolver como String si no matchea nada
        return raw
    }
}
