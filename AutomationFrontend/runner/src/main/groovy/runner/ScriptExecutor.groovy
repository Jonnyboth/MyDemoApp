package runner

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ObjectRepository
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords
import internal.GlobalVariable
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * Compila y ejecuta scripts Groovy de Katalon en tiempo de ejecución.
 *
 * Estrategia:
 *   1. Crea un GroovyClassLoader (GCL) con las bridge-classes como parent classloader.
 *   2. Agrega los directorios de Keywords, Libs e Include al classpath del GCL
 *      → los Keywords se compilan automáticamente cuando son importados.
 *   3. Compila el Script*.groovy usando el GCL.
 *   4. Expone CustomKeywords.classLoader = GCL para que el routing funcione.
 *   5. Ejecuta el script y captura fallos.
 */
class ScriptExecutor {

    private final File projectRoot
    private GroovyClassLoader gcl

    ScriptExecutor(File projectRoot) {
        this.projectRoot = projectRoot
    }

    /**
     * Inicializa el GroovyClassLoader con todas las rutas de fuente del proyecto.
     * Debe llamarse una vez antes de ejecutar tests.
     */
    void init() {
        CompilerConfiguration config = new CompilerConfiguration()
        config.sourceEncoding = 'UTF-8'

        // Imports automáticos que Katalon inyecta en cada script
        ImportCustomizer imports = new ImportCustomizer()
        imports.addStaticStars('com.kms.katalon.core.testobject.ObjectRepository')
        config.addCompilationCustomizers(imports)

        // GCL hereda el classloader del runner → tiene acceso a todas las bridge-classes
        gcl = new GroovyClassLoader(Thread.currentThread().contextClassLoader, config)

        // Agregar rutas de fuente del proyecto para compilación on-demand
        addIfExists('Keywords')
        addIfExists('Include/scripts/groovy')
        addIfExists('Libs')

        // Conectar el classloader al router de CustomKeywords
        CustomKeywords.classLoader = gcl

        // Inyectar executor en WebUI.callTestCase para sub-tests
        WebUiBuiltInKeywords.testCaseRunner = { String path, Map params, FailureHandling fh ->
            String cleanPath = path
                .replaceFirst(/^Test Cases\//, '')
                .replaceFirst(/\.tc$/, '')
            TestCaseLoader loader = new TestCaseLoader(projectRoot)
            TestCase subTc = loader.load(cleanPath)
            TestResult result = execute(subTc)
            if ((result.isFailed() || result.isError())) {
                switch (fh) {
                    case FailureHandling.STOP_ON_FAILURE:
                        throw new AssertionError(
                            "callTestCase '${path}' falló: ${result.errorMessage}"
                        )
                    case FailureHandling.CONTINUE_ON_FAILURE:
                        KeywordUtil.markFailed("callTestCase '${path}' falló: ${result.errorMessage}")
                        break
                    default:
                        KeywordUtil.logInfo("callTestCase '${path}' falló (OPTIONAL): ${result.errorMessage}")
                }
            }
        }

        println "[Runner] ScriptExecutor listo — classpath del proyecto agregado"
    }

    /**
     * Ejecuta un test case y retorna su resultado.
     * Pre-condition: init() debe haberse llamado.
     */
    TestResult execute(TestCase tc) {
        if (!tc.scriptFile?.exists()) {
            return new TestResult(
                name:         tc.name,
                status:       'SKIPPED',
                errorMessage: "Script no encontrado: ${tc.path}",
                durationMs:   0
            )
        }

        println ''
        println "  Ejecutando: ${tc.name}"
        println "  Script:     ${tc.scriptFile.name}"

        // Limpiar fallos anteriores del hilo
        KeywordUtil.clearFailures()

        long startMs = System.currentTimeMillis()

        try {
            Class  scriptClass = gcl.parseClass(tc.scriptFile)
            Script script      = scriptClass.newInstance() as Script
            script.binding     = new Binding()
            script.run()

            long duration = System.currentTimeMillis() - startMs

            // Verificar fallos acumulados (markFailed sin excepción)
            if (KeywordUtil.hasFailures()) {
                List<String> failures = KeywordUtil.getFailures()
                return new TestResult(
                    name:         tc.name,
                    status:       'FAILED',
                    errorMessage: failures.join('\n'),
                    durationMs:   duration
                )
            }

            return new TestResult(
                name:       tc.name,
                status:     'PASSED',
                durationMs: duration
            )

        } catch (AssertionError e) {
            long duration = System.currentTimeMillis() - startMs
            // Screenshot automático en fallo
            try { com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords.takeScreenshot() } catch (ignored) {}
            return new TestResult(
                name:         tc.name,
                status:       'FAILED',
                errorMessage: e.message,
                durationMs:   duration
            )
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs
            // Desenvuelve InvocationTargetException para mostrar la causa real
            Throwable cause = e
            while (cause.cause && cause.class.simpleName in ['InvocationTargetException', 'ExceptionInInitializerError']) {
                cause = cause.cause
            }
            // Log completo al stderr para debugging
            System.err.println "[Runner] ERROR en ${tc.name}:"
            cause.printStackTrace(System.err)
            return new TestResult(
                name:         tc.name,
                status:       'ERROR',
                errorMessage: "${cause.class.simpleName}: ${cause.message}",
                durationMs:   duration
            )
        }
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private void addIfExists(String relativePath) {
        File dir = new File(projectRoot, relativePath)
        if (dir.exists()) {
            gcl.addClasspath(dir.absolutePath)
        }
    }
}
