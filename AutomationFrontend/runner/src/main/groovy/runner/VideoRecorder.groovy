package runner

/**
 * Graba la pantalla del dispositivo Android de forma continua durante todo un
 * batch de ejecución (desde antes del primer test hasta después del último),
 * encadenando segmentos de `adb shell screenrecord` — limitado a ~3 min por
 * invocación — y uniéndolos con ffmpeg en un único MP4 al finalizar.
 *
 * Alcance actual: solo Android (único driver con dispositivo real en este
 * runner — ver AppiumDriverManager). Si `ffmpeg` no está disponible se
 * conserva el primer segmento sin unir en vez de fallar el run completo:
 * la grabación es evidencia auxiliar, nunca debe romper la ejecución de tests.
 *
 * Uso:
 *   VideoRecorder recorder = new VideoRecorder(udid, runLabel)
 *   recorder.start()
 *   ... ejecutar tests ...
 *   File video = recorder.stop(reportDir)   // null si falló o nunca arrancó
 *
 * Salida: <reportDir>/videos/<runLabel-sanitizado>_<yyyyMMdd_HHmmss>/video.mp4
 * — una carpeta nueva por corrida (timestamp tomado al arrancar, no al terminar),
 * así que nunca pisa el video de una corrida anterior.
 */
class VideoRecorder {

    private static final int SEGMENT_LIMIT_SECONDS = 170  // margen bajo el cap de 180s de screenrecord
    private static final String DEVICE_DIR = '/sdcard/qa_video_tmp'

    private final String udid
    private final String runLabel
    private Thread loopThread
    private volatile Process currentProc
    private volatile boolean stopping = false
    private volatile int segmentCount = 0
    private volatile boolean started = false
    private String startTimestamp

    VideoRecorder(String udid, String runLabel) {
        this.udid = udid
        this.runLabel = runLabel ?: 'run'
    }

    /** Arranca la grabación en background. Nunca lanza — si adb no responde, queda inactiva. */
    void start() {
        if (!udid) {
            println "[Runner] [Video] Sin device.udid configurado — grabación omitida"
            return
        }
        try {
            exec(['adb', '-s', udid, 'shell', 'mkdir', '-p', DEVICE_DIR])
        } catch (Exception e) {
            println "[Runner] [Video] adb no disponible, grabación omitida: ${e.message}"
            return
        }

        started   = true
        stopping  = false
        segmentCount = 0
        startTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())

        loopThread = new Thread({
            while (!stopping) {
                int idx = segmentCount++
                String remotePath = "${DEVICE_DIR}/seg_${String.format('%03d', idx)}.mp4"
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        'adb', '-s', udid, 'shell', 'screenrecord',
                        '--time-limit', SEGMENT_LIMIT_SECONDS.toString(),
                        remotePath
                    )
                    pb.redirectErrorStream(true)
                    currentProc = pb.start()
                    currentProc.inputStream.eachLine { /* drenar stdout */ }
                    currentProc.waitFor()
                } catch (Exception e) {
                    println "[Runner] [Video] Error en segmento ${idx}: ${e.message}"
                    stopping = true
                }
            }
        })
        loopThread.daemon = true
        loopThread.start()
        println "[Runner] [Video] Grabación iniciada (udid=${udid})"
    }

    /**
     * Detiene la grabación, descarga los segmentos y los concatena en un único
     * MP4 continuo bajo <reportDir>/videos/. Retorna el archivo final o null
     * si la grabación nunca arrancó o no se produjo ningún segmento válido.
     */
    File stop(File reportDir) {
        if (!started) return null

        stopping = true
        // screenrecord solo cierra el contenedor MP4 (escribe el atomo 'moov') si recibe
        // SIGINT — destruir el cliente `adb` local (SIGTERM) mata el proceso remoto en seco
        // y deja un MP4 sin finalizar ("moov atom not found"). Se manda SIGINT directo al
        // proceso remoto y se da margen antes de tocar el cliente local.
        try { exec(['adb', '-s', udid, 'shell', 'killall', '-2', 'screenrecord']) } catch (ignored) {}
        sleep(1500)
        try { currentProc?.destroy() } catch (ignored) {}
        loopThread?.join(10_000)

        File localTmp = File.createTempDir('qa_video_', '')

        try {
            exec(['adb', '-s', udid, 'pull', DEVICE_DIR, localTmp.absolutePath])
        } catch (Exception e) {
            println "[Runner] [Video] No se pudo descargar la grabación: ${e.message}"
            return null
        } finally {
            try { exec(['adb', '-s', udid, 'shell', 'rm', '-rf', DEVICE_DIR]) } catch (ignored) {}
        }

        File pulledDir = new File(localTmp, 'qa_video_tmp')
        List<File> segments = (pulledDir.exists() ? pulledDir.listFiles()?.toList() : [])
            ?.findAll { it.name.endsWith('.mp4') && it.length() > 0 }
            ?.sort { it.name }

        if (!segments) {
            println "[Runner] [Video] Ningún segmento válido descargado"
            return null
        }

        File runDir = new File(reportDir, "videos/${sanitize(runLabel)}_${startTimestamp}")
        runDir.mkdirs()
        File finalVideo = new File(runDir, 'video.mp4')

        if (segments.size() == 1) {
            segments[0].renameTo(finalVideo)
            println "[Runner] [Video] Video final: ${finalVideo.absolutePath}"
            return finalVideo
        }

        try {
            File concatList = new File(localTmp, 'concat.txt')
            concatList.text = segments.collect { "file '${it.absolutePath}'" }.join('\n')
            exec(['ffmpeg', '-y', '-f', 'concat', '-safe', '0',
                  '-i', concatList.absolutePath, '-c', 'copy', finalVideo.absolutePath])
            println "[Runner] [Video] Video final (${segments.size()} segmentos unidos): ${finalVideo.absolutePath}"
            return finalVideo
        } catch (Exception e) {
            println "[Runner] [Video] ffmpeg falló (${e.message}) — se conserva el primer segmento sin unir"
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
