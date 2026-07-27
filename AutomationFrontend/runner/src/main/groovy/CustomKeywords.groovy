/**
 * Bridge de CustomKeywords de Katalon.
 *
 * Los scripts llaman keywords así:
 *   CustomKeywords.'com.tuempresa.steps.android.TurboSteps.addProductsToCart'(35)
 *
 * Groovy interpreta esto como acceso a propiedad + llamada de closure.
 * El static initializer registra un propertyMissing que retorna una Closure
 * que carga la clase por reflexión y llama el método.
 *
 * ScriptExecutor debe setear `CustomKeywords.classLoader` antes de correr scripts.
 */
class CustomKeywords {

    /** GroovyClassLoader con los Keywords compilados — seteado por ScriptExecutor. */
    static ClassLoader classLoader

    static {
        CustomKeywords.metaClass.static.propertyMissing = { String route ->
            return { Object... callArgs ->
                if (!classLoader) {
                    throw new IllegalStateException(
                        "CustomKeywords.classLoader no inicializado. " +
                        "ScriptExecutor debe setearlo antes de ejecutar tests."
                    )
                }

                // route = "com.tuempresa.steps.android.TurboSteps.addProductsToCart"
                int lastDot = route.lastIndexOf('.')
                if (lastDot < 0) {
                    throw new RuntimeException("Ruta de keyword inválida: '${route}'")
                }

                String className  = route.substring(0, lastDot)
                String methodName = route.substring(lastDot + 1)

                Class  clazz    = classLoader.loadClass(className)
                Object instance = clazz.newInstance()

                try {
                    if (callArgs && callArgs.length > 0) {
                        return instance."${methodName}"(*callArgs)
                    } else {
                        return instance."${methodName}"()
                    }
                } catch (MissingMethodException e) {
                    throw new RuntimeException(
                        "Keyword no encontrado: ${className}.${methodName}(${callArgs?.join(', ')})\n" +
                        "Causa: ${e.message}"
                    )
                }
            }
        }
    }
}
