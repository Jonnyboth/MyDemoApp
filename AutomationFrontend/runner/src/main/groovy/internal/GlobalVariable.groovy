package internal

/**
 * Bridge de GlobalVariable de Katalon.
 *
 * Los scripts acceden a variables como:
 *   GlobalVariable.G_AppBundleID
 *   GlobalVariable.G_Platform
 *
 * KatalonRunner.init() carga las variables desde Profiles/default.glbl
 * antes de ejecutar cualquier test.
 */
class GlobalVariable {

    private static Map<String, Object> vars = [:]

    static {
        // Registrar propertyMissing estático para que GlobalVariable.NOMBRE funcione
        GlobalVariable.metaClass.static.propertyMissing = { String name ->
            if (vars.containsKey(name)) return vars[name]
            throw new MissingPropertyException(
                "GlobalVariable.${name} no está definida. " +
                "Verifica Profiles/default.glbl", GlobalVariable
            )
        }
    }

    /** Llamado por GlobalVariableLoader al iniciar el runner. */
    static void init(Map<String, Object> variables) {
        vars = new HashMap<>(variables)
    }

    static Map<String, Object> getAll() {
        Collections.unmodifiableMap(vars)
    }

    /** Override de variable en runtime (útil para tests parametrizados). */
    static void set(String name, Object value) {
        vars[name] = value
    }
}
