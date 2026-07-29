package MyDemoApp.utils

/**
 * Graba la pantalla del dispositivo Android de forma continua (adb shell screenrecord
 * encadenado en segmentos de ~170s + ffmpeg concat), pensada para usarse desde
 * Test Listeners/VideoRecorderListener.groovy dentro de Katalon Studio real.
 *
 * Misma lógica que runner/src/main/groovy/runner/VideoRecorder.groovy del runner
 * headless — duplicada aquí a propósito: Katalon Studio real no tiene visibilidad
 * del jar del runner (Keywords/ y runner/ son classpaths independientes), así que
 * no se puede compartir la clase entre los dos motores sin reestructurar el build.
 *
 * Nunca lanza — un fallo de grabación (adb/ffmpeg no disponibles, dispositivo
 * desconectado, etc.) no debe romper la ejecución de los tests. Solo queda inactiva
 * y lo reporta por consola.
 *
 * adbPath/ffmpegPath: por defecto asume que "adb"/"ffmpeg" están en el PATH del
 * proceso que ejecuta Katalon Studio — pero si Katalon Studio corre nativo en
 * Windows (fuera de WSL), ese PATH normalmente NO incluye el SDK de Android ni
 * ffmpeg, aunque sí existan en el sistema. Pasa la ruta absoluta de cada binario
 * (ej. "C:\\Users\\tú\\.katalon\\tools\\android_sdk\\platform-tools\\adb.exe")
 * si el PATH por defecto no funciona.
 *
 * Uso:
 *   VideoRecorderUtil recorder = new VideoRecorderUtil(udid, runLabel, adbPath, ffmpegPath)
 *   boolean started = recorder.start()   // false si adb no respondió — no lanza
 *   ... ejecutar tests ...
 *   File video = recorder.stop(outputDir)   // null si falló o nunca arrancó
 *
 * Salida: <outputDir>/videos/<runLabel-sanitizado>_<yyyyMMdd_HHmmss>/video.mp4
 */
class VideoRecorderUtil {

    private static final int SEGMENT_LIMIT_SECONDS = 170  // margen bajo el cap de 180s de screenrecord
    private static final String DEVICE_DIR = '/sdcard/qa_video_tmp'

    private final String udid
    private final String runLabel
    private final String adbPath
    private final String ffmpegPath
    private Thread loopThread
    private volatile Process currentProc
    private volatile boolean stopping = false
    private volatile int segmentCount = 0
    private volatile boolean started = false
    private String startTimestamp

    VideoRecorderUtil(String udid, String runLabel, String adbPath = 'adb', String ffmpegPath = 'ffmpeg') {
        this.udid = udid
        this.runLabel = runLabel ?: 'run'
        this.adbPath = adbPath?.trim() ?: 'adb'
        this.ffmpegPath = ffmpegPath?.trim() ?: 'ffmpeg'
    }

    /**
     * Arranca la grabación en background. Nunca lanza — si adb no responde, queda
     * inactiva y retorna false (en vez de reportar éxito a medias como antes).
     */
    boolean start() {
        if (!udid) {
            println "[VideoRecorderUtil] Sin device.udid configurado — grabación omitida"
            return false
        }
        try {
            exec([adbPath, '-s', udid, 'shell', 'mkdir', '-p', DEVICE_DIR])
        } catch (Exception e) {
            println "[VideoRecorderUtil] adb ('${adbPath}') no disponible, grabación omitida: ${e.message}"
            return false
        }

        started      = true
        stopping     = false
        segmentCount = 0
        startTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())

        loopThread = new Thread({
            while (!stopping) {
                int idx = segmentCount++
                String remotePath = "${DEVICE_DIR}/seg_${String.format('%03d', idx)}.mp4"
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        adbPath, '-s', udid, 'shell', 'screenrecord',
                        '--time-limit', SEGMENT_LIMIT_SECONDS.toString(),
                        remotePath
                    )
                    pb.redirectErrorStream(true)
                    currentProc = pb.start()
                    currentProc.inputStream.eachLine { /* drenar stdout */ }
                    currentProc.waitFor()
                } catch (Exception e) {
                    println "[VideoRecorderUtil] Error en segmento ${idx}: ${e.message}"
                    stopping = true
                }
            }
        })
        loopThread.daemon = true
        loopThread.start()
        println "[VideoRecorderUtil] Grabación iniciada (udid=${udid}, label=${runLabel})"
        return true
    }

    /**
     * Detiene la grabación, descarga los segmentos y los concatena en un único
     * MP4 continuo bajo <outputDir>/videos/<runLabel>_<timestamp>/video.mp4.
     * Retorna el archivo final o null si la grabación nunca arrancó o no se
     * produjo ningún segmento válido.
     */
    File stop(File outputDir) {
        if (!started) return null

        stopping = true
        // screenrecord solo cierra el contenedor MP4 (escribe el atomo 'moov') si recibe
        // SIGINT — destruir el cliente `adb` local (SIGTERM) mata el proceso remoto en seco
        // y deja un MP4 sin finalizar ("moov atom not found"). Se manda SIGINT directo al
        // proceso remoto y se da margen antes de tocar el cliente local.
        try { exec([adbPath, '-s', udid, 'shell', 'killall', '-2', 'screenrecord']) } catch (ignored) {}
        sleep(1500)
        try { currentProc?.destroy() } catch (ignored) {}
        loopThread?.join(10_000)

        File localTmp = File.createTempDir('qa_video_', '')

        try {
            exec([adbPath, '-s', udid, 'pull', DEVICE_DIR, localTmp.absolutePath])
        } catch (Exception e) {
            println "[VideoRecorderUtil] No se pudo descargar la grabación: ${e.message}"
            return null
        } finally {
            try { exec([adbPath, '-s', udid, 'shell', 'rm', '-rf', DEVICE_DIR]) } catch (ignored) {}
        }

        File pulledDir = new File(localTmp, 'qa_video_tmp')
        List<File> segments = (pulledDir.exists() ? pulledDir.listFiles()?.toList() : [])
            ?.findAll { it.name.endsWith('.mp4') && it.length() > 0 }
            ?.sort { it.name }

        if (!segments) {
            println "[VideoRecorderUtil] Ningún segmento válido descargado"
            return null
        }

        File runDir = new File(outputDir, "videos/${sanitize(runLabel)}_${startTimestamp}")
        runDir.mkdirs()
        File finalVideo = new File(runDir, 'video.mp4')

        if (segments.size() == 1) {
            segments[0].renameTo(finalVideo)
            println "[VideoRecorderUtil] Video final: ${finalVideo.absolutePath}"
            return finalVideo
        }

        try {
            File concatList = new File(localTmp, 'concat.txt')
            concatList.text = segments.collect { "file '${it.absolutePath}'" }.join('\n')
            exec([ffmpegPath, '-y', '-f', 'concat', '-safe', '0',
                  '-i', concatList.absolutePath, '-c', 'copy', finalVideo.absolutePath])
            println "[VideoRecorderUtil] Video final (${segments.size()} segmentos unidos): ${finalVideo.absolutePath}"
            return finalVideo
        } catch (Exception e) {
            println "[VideoRecorderUtil] ffmpeg ('${ffmpegPath}') falló (${e.message}) — se conserva el primer segmento sin unir"
            segments[0].renameTo(finalVideo)
            return finalVideo
        }
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    /** Limpia el nombre de la corrida para que sea seguro como nombre de carpeta. */
    private static String sanitize(String label) {
        String clean = label.replaceAll(/[^a-zA-Z0-9._-]/, '_')
        return clean.length() > 80 ? clean.substring(0, 80) : clean
    }

    private static void exec(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command)
        pb.redirectErrorStream(true)
        Process p = pb.start()
        String output = p.inputStream.text
        int code = p.waitFor()
        if (code != 0) {
            throw new RuntimeException("Comando falló (${code}): ${command.join(' ')}\n${output}")
        }
    }
}
