import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext
import com.kms.katalon.core.util.KeywordUtil
import MyDemoApp.utils.VideoRecorderUtil

/**
 * Graba video continuo (adb screenrecord + ffmpeg) cuando se corre manualmente
 * desde Katalon Studio — botón Run sobre un Test Suite o sobre un Test Case
 * suelto. Controlado por Profiles/default.glbl > G_RecordVideo (true/false),
 * la misma variable que lee el runner headless por terminal.
 *
 * Si un Test Case corre DENTRO de un Test Suite, @BeforeTestSuite ya arrancó
 * la grabación del suite completo — @BeforeTestCase/@AfterTestCase detectan
 * eso (suiteRecording) y no abren una segunda grabación en paralelo.
 *
 * Salida: <projectDir>/Reports/videos/<nombre-suite-o-TC>_<fecha>_<hora>/video.mp4
 * — al lado de la carpeta Reports/ nativa de Katalon, no dentro de ella (el
 * path de reporte de la corrida actual solo lo resuelve Katalon al terminar).
 *
 * G_AdbPath / G_FfmpegPath (opcionales, Profiles/default.glbl): ruta absoluta a
 * adb.exe / ffmpeg.exe. Si Katalon Studio corre nativo en Windows (no dentro de
 * WSL), el PATH de ese proceso normalmente NO incluye el SDK de Android ni
 * ffmpeg aunque sí existan en el sistema — deja estas variables vacías para usar
 * el PATH tal cual, o pon la ruta completa si "adb"/"ffmpeg" solos no funcionan.
 */
class VideoRecorderListener {

    private static VideoRecorderUtil recorder
    private static boolean suiteRecording = false

    @BeforeTestSuite
    def beforeTestSuite(TestSuiteContext testSuiteContext) {
        if (!recordEnabled()) return
        suiteRecording = true
        startRecording(lastSegment(testSuiteContext.getTestSuiteId()))
    }

    @AfterTestSuite
    def afterTestSuite(TestSuiteContext testSuiteContext) {
        if (!suiteRecording) return
        stopRecording()
        suiteRecording = false
    }

    @BeforeTestCase
    def beforeTestCase(TestCaseContext testCaseContext) {
        if (suiteRecording) return  // ya grabando el suite completo — no duplicar
        if (!recordEnabled()) return
        startRecording(lastSegment(testCaseContext.getTestCaseId()))
    }

    @AfterTestCase
    def afterTestCase(TestCaseContext testCaseContext) {
        if (suiteRecording) return  // se cierra en afterTestSuite
        if (recorder == null) return
        stopRecording()
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private static void startRecording(String label) {
        String udid       = readGlobalVar('G_DevicesName')?.toString()
        String adbPath    = readGlobalVar('G_AdbPath')?.toString()
        String ffmpegPath = readGlobalVar('G_FfmpegPath')?.toString()
        recorder = new VideoRecorderUtil(
            udid, label,
            adbPath ?: 'adb',
            ffmpegPath ?: 'ffmpeg'
        )
        boolean ok = recorder.start()
        if (ok) {
            KeywordUtil.logInfo("[VideoRecorderListener] Grabación iniciada: ${label}")
        } else {
            KeywordUtil.logInfo(
                "[VideoRecorderListener] No se pudo iniciar la grabación de '${label}' — " +
                "revisa que adb esté en G_AdbPath (o en el PATH) y que el dispositivo esté conectado."
            )
            recorder = null
        }
    }

    private static void stopRecording() {
        File reportsDir = new File(RunConfiguration.getProjectDir(), 'Reports')
        File video = recorder?.stop(reportsDir)
        if (video) {
            KeywordUtil.logInfo("[VideoRecorderListener] Video: ${video.absolutePath}")
        }
        recorder = null
    }

    private static boolean recordEnabled() {
        readGlobalVar('G_RecordVideo') == true
    }

    private static String lastSegment(String path) {
        if (!path) return 'run'
        String clean = path.replaceAll(/\.(ts|tc)$/, '')
        String[] parts = clean.split('/')
        return parts[-1]
    }

    /**
     * internal.GlobalVariable tiene forma distinta según dónde corre: Katalon Studio
     * real usa campos estáticos públicos regenerados por el IDE; el runner headless
     * propio expone un Map vía getAll(). Se prueba el campo primero y se cae a
     * getAll() si no existe — mismo patrón que AppLifecyclePage.appBundleId().
     * Por reflection (no import estático) para evitar ClassNotFoundException si
     * Katalon compila este listener antes que internal/GlobalVariable.groovy.
     */
    private static Object readGlobalVar(String name) {
        try {
            Class globalVariableClass = Class.forName('internal.GlobalVariable')
            try {
                return globalVariableClass.getField(name).get(null)
            } catch (NoSuchFieldException ignored) {
                Map<String, Object> vars = globalVariableClass.getMethod('getAll').invoke(null)
                return vars.get(name)
            }
        } catch (Exception e) {
            return null
        }
    }
}
