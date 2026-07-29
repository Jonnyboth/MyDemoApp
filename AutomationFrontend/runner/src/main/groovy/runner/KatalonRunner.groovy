package runner

import com.kms.katalon.core.testobject.ObjectRepository
import internal.GlobalVariable
import org.yaml.snakeyaml.Yaml

/**
 * Punto de entrada del Katalon Headless Runner.
 *
 * Uso:
 *   ./run.sh run --case android/TC_CompraProductosTurboDev
 *   ./run.sh run --tag smoke
 *   ./run.sh run --suite Android/Smoke/Android-Smoke
 *   ./run.sh run --all
 *   ./run.sh list
 *   ./run.sh list --tag smoke
 *
 * Opciones:
 *   --project <dir>    Directorio raíz del proyecto (default: System property runner.projectDir)
 *   --config  <file>   Archivo YAML de configuración (default: runner/config/runner.yml)
 *   --report  <dir>    Directorio de reportes (default: <project>/runner/reports)
 *   --suite   <path>   Ejecuta un Test Suite nativo de Katalon (.ts bajo 'Test Suites/')
 *   --record           Fuerza la grabación de video para este run puntual (ver abajo)
 *   --no-driver        No iniciar ningún driver, ni Appium ni navegador (dry-run)
 *   --no-appium        Alias retrocompatible de --no-driver
 *
 * Reportes: siempre se generan runner/reports/test-results.xml (JUnit) y
 * runner/reports/report.html (HTML, con el video embebido si se grabó).
 *
 * Video: se activa si Profiles/default.glbl > G_RecordVideo: true (misma fuente que lee
 * Katalon Studio real via Test Listeners/VideoRecorderListener.groovy), o puntualmente
 * con --record (el flag de terminal es un override — funciona aunque G_RecordVideo esté
 * en false). Requiere adb + ffmpeg, solo Android. Cada corrida deja su propia carpeta,
 * nunca pisa la anterior: runner/reports/videos/<nombre-suite-o-test>_<fecha>_<hora>/video.mp4
 *
 * Multiplataforma: la plataforma de cada test case se deriva del primer segmento
 * de su ruta bajo 'Test Cases/' (android/ → Appium+UiAutomator2, web/ → Selenium,
 * ios/ → Appium+XCUITest — pendiente de driver XCUITest, ver runner/SETUP.md).
 * Un mismo run puede mezclar test cases de distintas plataformas: cada una
 * inicializa su propio driver de forma perezosa, solo cuando hace falta.
 */
class KatalonRunner {

    static void main(String[] args) {
        println ''
        println '╔══════════════════════════════════════════════════╗'
        println '║       Katalon Headless Runner  v1.0.0            ║'
        println '╚══════════════════════════════════════════════════╝'

        // ── Parsear argumentos CLI ─────────────────────────────────────────
        Map<String, String> opts = parseArgs(args)

        String command = opts['_command'] ?: 'run'

        File projectRoot = resolveProjectRoot(opts)
        File configFile  = resolveConfigFile(opts, projectRoot)
        File reportDir   = resolveReportDir(opts, projectRoot)

        println "[Runner] Proyecto: ${projectRoot.absolutePath}"
        println "[Runner] Config:   ${configFile.absolutePath}"
        println "[Runner] Reportes: ${reportDir.absolutePath}"

        // ── Cargar configuración YAML ──────────────────────────────────────
        Map<String, Object> config = loadConfig(configFile)

        // ── Inicializar bridge singletons ──────────────────────────────────
        initBridges(projectRoot, config)

        // ── Cargar test cases ──────────────────────────────────────────────
        TestCaseLoader loader = new TestCaseLoader(projectRoot)
        List<TestCase>  cases = selectTestCases(command, opts, loader, projectRoot)

        if (command == 'list') {
            printList(cases)
            return
        }

        if (cases.isEmpty()) {
            println "[Runner] No se encontraron test cases con los filtros dados."
            return
        }

        println "[Runner] Tests a ejecutar: ${cases.size()}"
        println "[Runner] Plataformas detectadas: ${cases.collect { it.platform }.unique().join(', ')}"

        // ── Dry-run: sin ningún driver (ni Appium ni navegador) ─────────────
        // --no-appium se conserva por compatibilidad con invocaciones existentes.
        boolean noDriver = opts.containsKey('--no-driver') || opts.containsKey('--no-appium')

        // ── Grabación de video (Profiles/default.glbl > G_RecordVideo, o --record puntual) ──
        // Arranca ANTES del primer test y se detiene en el finally de abajo,
        // así cubre literalmente "desde el primer segundo hasta el último"
        // incluso si algún test lanza una excepción no controlada.
        // G_RecordVideo es la MISMA variable que lee Katalon Studio real (via
        // Test Listeners/VideoRecorderListener.groovy) — una sola fuente de verdad.
        boolean recordFromGlobalVar = GlobalVariable.getAll()['G_RecordVideo'] == true
        boolean record = opts.containsKey('--record') || recordFromGlobalVar
        String  udid   = ((config.device ?: [:]) as Map)?.udid?.toString()
        String  runLabel = determineRunLabel(opts)
        VideoRecorder videoRecorder = record ? new VideoRecorder(udid, runLabel) : null
        File videoFile = null
        videoRecorder?.start()

        // ── Ejecutar tests ─────────────────────────────────────────────────
        ScriptExecutor   executor = new ScriptExecutor(projectRoot)
        executor.init()

        List<TestResult> results = []

        try {
            cases.each { tc ->
                TestResult result = runOne(tc, executor, config, noDriver)
                results << result
                String icon = result.isPassed() ? '✓' : (result.isSkipped() ? '○' : '✗')
                println "  ${icon} ${result.status.padRight(7)} ${tc.name} (${result.durationMs}ms)"
            }
        } finally {
            // Siempre cerrar TODAS las sesiones de driver activas aunque haya fallos.
            // Un mismo run puede haber mezclado test cases android/ y web/, cada uno
            // con su propio driver perezosamente iniciado — ambos se cierran aquí.
            if (AppiumDriverManager.isActive()) AppiumDriverManager.quit()
            if (WebDriverManager.isActive())    WebDriverManager.quit()

            // La grabación se detiene siempre, incluso si el batch falló a mitad de camino.
            videoFile = videoRecorder?.stop(reportDir)
        }

        // ── Generar reportes ──────────────────────────────────────────────
        reportDir.mkdirs()
        ReportGenerator.writeJUnitXml(results, new File(reportDir, 'test-results.xml'))
        ReportGenerator.writeHtmlReport(results, new File(reportDir, 'report.html'), videoFile)
        ReportGenerator.printSummary(results)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Privados
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta un test case de forma que NINGUNA excepción escape al loop de cases.each.
     * Sin este envoltorio, un fallo al iniciar el driver (ej. un TC ios/ sin soporte,
     * o Appium/Chrome caído a mitad de sesión) se propagaría fuera del try/finally de
     * main(), saltándose ReportGenerator.writeJUnitXml() y descartando en silencio los
     * resultados de TODOS los test cases anteriores que ya habían pasado en el batch.
     */
    private static TestResult runOne(TestCase tc, ScriptExecutor executor,
                                      Map<String, Object> config, boolean noDriver) {
        long startMs = System.currentTimeMillis()
        try {
            if (!noDriver) ensureDriverForPlatform(tc.platform, config)
            return executor.execute(tc)
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs
            System.err.println "[Runner] ERROR iniciando driver para ${tc.name} (platform=${tc.platform}):"
            e.printStackTrace(System.err)
            return new TestResult(
                name:         tc.name,
                status:       'ERROR',
                errorMessage: "${e.class.simpleName}: ${e.message}",
                durationMs:   duration
            )
        }
    }

    /**
     * Garantiza que el driver correspondiente a la plataforma del test case esté
     * iniciado, sin forzar la sesión de la otra plataforma. Cada DriverManager
     * es un singleton idempotente (init() no hace nada si ya hay sesión activa),
     * así que test cases consecutivos de la misma plataforma reutilizan sesión.
     */
    private static void ensureDriverForPlatform(Platform platform, Map<String, Object> config) {
        switch (platform) {
            case Platform.ANDROID:
                if (!AppiumDriverManager.isActive()) AppiumDriverManager.init(config)
                break
            case Platform.WEB:
                if (!WebDriverManager.isActive()) WebDriverManager.init(config)
                break
            case Platform.IOS:
                // AppiumDriverManager solo construye AndroidDriver+UiAutomator2Options —
                // reutilizarlo aquí crearía una sesión Android para un TC iOS y fallaría
                // de forma confusa más adelante. Alcance actual: Web + Android únicamente
                // (ver runner/SETUP.md). Se falla rápido y explícito en vez de silencioso.
                throw new IllegalStateException(
                    "Test case iOS detectado (${platform}) pero no hay driver XCUITest implementado. " +
                    "El runner soporta Android (Appium/UiAutomator2) y Web (Selenium) únicamente."
                )
            default:
                // UNKNOWN: test case fuera de la convención android/ios/web (ej. API pura).
                // No se inicializa ningún driver — el script decide si lo necesita.
                break
        }
    }

    private static void initBridges(File projectRoot, Map<String, Object> config) {
        // ObjectRepository necesita la raíz del proyecto
        ObjectRepository.projectRoot = projectRoot

        // GlobalVariable carga desde Profiles/default.glbl
        File glblFile = new File(projectRoot, 'Profiles/default.glbl')
        Map<String, Object> vars = GlobalVariableLoader.load(glblFile)

        // Sobrescribir con valores del config YAML si existen
        Map deviceCfg = (config.device ?: [:]) as Map
        if (deviceCfg.udid)       vars['G_DevicesName'] = deviceCfg.udid.toString()
        if (deviceCfg.appPackage) vars['G_AppBundleID'] = deviceCfg.appPackage.toString()

        GlobalVariable.init(vars)

        println "[Runner] GlobalVariables inicializadas: ${vars.size()} variables"
    }

    private static List<TestCase> selectTestCases(String command, Map<String, String> opts,
                                                   TestCaseLoader loader, File projectRoot) {
        if (opts['--case']) {
            return [loader.load(opts['--case'])]
        }
        if (opts['--suite']) {
            return new TestSuiteLoader(projectRoot).load(opts['--suite'])
        }
        if (opts['--tag']) {
            return loader.loadByTag(opts['--tag'])
        }
        if (command == 'run' && !opts.containsKey('--all')) {
            // Sin filtro explícito en 'run' — requerir uno
            System.err.println "Error: especifica --case, --tag o --all"
            System.err.println "Ejemplo: ./run.sh run --tag smoke"
            System.exit(1)
        }
        return loader.loadAll()
    }

    /**
     * Nombre descriptivo de la corrida, usado para nombrar la carpeta del video.
     * Prioriza el criterio de selección más específico: --suite/--case ya identifican
     * exactamente qué se corrió; --tag y --all son más genéricos.
     */
    private static String determineRunLabel(Map<String, String> opts) {
        if (opts['--suite']) return lastPathSegment(opts['--suite'])
        if (opts['--case'])  return lastPathSegment(opts['--case'])
        if (opts['--tag'])   return "tag-${opts['--tag']}"
        return 'all'
    }

    private static String lastPathSegment(String path) {
        String clean = path.replaceAll(/\.(ts|tc)$/, '')
        String[] parts = clean.split('/')
        return parts[-1]
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = [:]
        List<String> argList = args.toList()

        // Primer argumento no-flag es el comando
        if (argList && !argList[0].startsWith('-')) {
            opts['_command'] = argList.remove(0)
        }

        // Parsear pares --key value y flags --key
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList[i]
            if (arg.startsWith('--')) {
                if (i + 1 < argList.size() && !argList[i + 1].startsWith('--')) {
                    opts[arg] = argList[++i]
                } else {
                    opts[arg] = 'true'
                }
            }
        }
        return opts
    }

    private static File resolveProjectRoot(Map<String, String> opts) {
        String path = opts['--project']
            ?: System.getProperty('runner.projectDir')
            ?: System.getenv('KATALON_PROJECT_DIR')
            ?: new File('.').canonicalPath

        File dir = new File(path)
        if (!dir.exists()) {
            System.err.println "Error: directorio del proyecto no existe: ${path}"
            System.exit(1)
        }
        return dir.canonicalFile
    }

    private static File resolveConfigFile(Map<String, String> opts, File projectRoot) {
        String path = opts['--config']
            ?: System.getProperty('runner.configFile')
            ?: "${projectRoot.absolutePath}/runner/config/runner.yml"
        return new File(path)
    }

    private static File resolveReportDir(Map<String, String> opts, File projectRoot) {
        String path = opts['--report']
            ?: System.getProperty('runner.reportDir')
            ?: "${projectRoot.absolutePath}/runner/reports"
        return new File(path)
    }

    private static Map<String, Object> loadConfig(File configFile) {
        if (!configFile.exists()) {
            println "[Runner] Advertencia: ${configFile.name} no encontrado — usando defaults"
            return [appium: [url: 'http://localhost:4723'], device: [:]]
        }
        Map config = new Yaml().load(configFile.text) as Map
        println "[Runner] Configuración cargada: ${configFile.name}"
        return config
    }

    private static void printList(List<TestCase> cases) {
        println "\nTest cases disponibles (${cases.size()}):\n"
        cases.each { tc ->
            String tags = tc.tags ? " [${tc.tags.join(', ')}]" : ''
            String script = tc.scriptFile ? '✓' : '✗'
            println "  ${script} ${tc.path}${tags}"
        }
    }
}
