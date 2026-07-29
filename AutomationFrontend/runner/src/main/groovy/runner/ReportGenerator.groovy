package runner

import groovy.xml.MarkupBuilder

/**
 * Genera reportes de los resultados de ejecución.
 *
 * Formatos:
 *   - Consola  (siempre)
 *   - JUnit XML (compatible con CI/CD: GitHub Actions, Jenkins, etc.)
 *   - HTML     (para revisión humana, con video embebido si se grabó)
 */
class ReportGenerator {

    static void printSummary(List<TestResult> results) {
        int passed  = results.count { it.isPassed()  }
        int failed  = results.count { it.isFailed()  }
        int errors  = results.count { it.isError()   }
        int skipped = results.count { it.isSkipped() }
        long totalMs = results.sum { it.durationMs ?: 0 } as long

        println ''
        println '═' * 60
        println ' RESULTADOS'
        println '═' * 60
        results.each { r ->
            String icon   = r.isPassed() ? '✓' : (r.isSkipped() ? '○' : '✗')
            String timing = r.durationMs ? " (${r.durationMs}ms)" : ''
            println " ${icon} [${r.status.padRight(7)}] ${r.name}${timing}"
            if (r.errorMessage) {
                r.errorMessage.split('\n').take(3).each { line ->
                    println "          ${line}"
                }
            }
        }
        println '─' * 60
        println " Total: ${results.size()}  |  " +
                "✓ ${passed}  ✗ ${failed + errors}  ○ ${skipped}  |  " +
                "${(totalMs / 1000).round(1)}s"
        println '═' * 60

        if (failed + errors > 0) {
            System.exit(1)  // Exit code no-cero para CI/CD
        }
    }

    static void writeJUnitXml(List<TestResult> results, File outputFile) {
        outputFile.parentFile?.mkdirs()

        int    passed   = results.count { it.isPassed()  }
        int    failures = results.count { it.isFailed()  }
        int    errors   = results.count { it.isError()   }
        int    skipped  = results.count { it.isSkipped() }
        double totalSec = results.sum   { (it.durationMs ?: 0) } / 1000.0

        StringWriter sw  = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(sw)

        xml.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')
        xml.testsuite(
            name:      'KatalonHeadlessRunner',
            tests:     results.size(),
            failures:  failures,
            errors:    errors,
            skipped:   skipped,
            time:      String.format('%.3f', totalSec),
            timestamp: new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date())
        ) {
            results.each { r ->
                testcase(
                    name:      r.name,
                    classname: 'runner.KatalonTest',
                    time:      String.format('%.3f', (r.durationMs ?: 0) / 1000.0)
                ) {
                    if (r.isFailed()) {
                        failure(message: r.errorMessage ?: 'Test fallido', type: 'AssertionError') {
                            mkp.yield(r.errorMessage ?: '')
                        }
                    } else if (r.isError()) {
                        error(message: r.errorMessage ?: 'Error inesperado', type: 'Exception') {
                            mkp.yield(r.errorMessage ?: '')
                        }
                    } else if (r.isSkipped()) {
                        skipped()
                    }
                }
            }
        }

        outputFile.text = sw.toString()
        println "[Runner] Reporte JUnit XML: ${outputFile.absolutePath}"
    }

    /**
     * Genera un reporte HTML autocontenido (sin dependencias externas) con el
     * resumen del run, el detalle por test y, si se grabó, el video completo
     * embebido. `videoFile`, si no es null, debe vivir bajo el mismo directorio
     * que `outputFile` (p. ej. <reportDir>/videos/run_*.mp4) para poder
     * referenciarlo con una ruta relativa portable.
     */
    static void writeHtmlReport(List<TestResult> results, File outputFile, File videoFile = null) {
        outputFile.parentFile?.mkdirs()

        int    passed   = results.count { it.isPassed()  }
        int    failed   = results.count { it.isFailed() || it.isError() }
        int    skipped  = results.count { it.isSkipped() }
        double totalSec = results.sum   { (it.durationMs ?: 0) } / 1000.0
        String runAt    = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())

        String videoSection = ''
        if (videoFile && videoFile.exists()) {
            // Relativiza contra la carpeta del propio report.html en vez de asumir un path
            // fijo — el video vive en una subcarpeta propia por corrida (videos/<label>_<ts>/).
            String relPath = outputFile.parentFile.toPath()
                .relativize(videoFile.toPath()).toString().replace('\\', '/')
            videoSection = """
      <section class="video">
        <h2>Video de la ejecución (primer segundo → último segundo)</h2>
        <video controls preload="metadata" src="${escapeHtml(relPath)}"></video>
      </section>"""
        }

        String rows = results.collect { r ->
            String cssClass = r.isPassed() ? 'passed' : (r.isSkipped() ? 'skipped' : 'failed')
            String icon     = r.isPassed() ? '✓' : (r.isSkipped() ? '○' : '✗')
            String errorRow = r.errorMessage
                ? """
        <tr class="${cssClass}-detail">
          <td colspan="4"><pre>${escapeHtml(r.errorMessage)}</pre></td>
        </tr>"""
                : ''
            """
        <tr class="${cssClass}">
          <td class="icon">${icon}</td>
          <td>${escapeHtml(r.name)}</td>
          <td>${r.status}</td>
          <td>${String.format('%.2f', (r.durationMs ?: 0) / 1000.0)}s</td>
        </tr>${errorRow}"""
        }.join('\n')

        String html = """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Reporte de ejecución — Katalon Headless Runner</title>
<style>
  :root { color-scheme: light dark; }
  body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; padding: 2rem;
         background: #f5f6f8; color: #1c1e21; }
  @media (prefers-color-scheme: dark) {
    body { background: #16181c; color: #e4e6eb; }
    table { background: #1f2226 !important; }
    .summary .card { background: #24272c !important; }
    video { background: #000; }
  }
  h1 { margin-top: 0; }
  .summary { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
  .summary .card { background: #fff; border-radius: 8px; padding: 1rem 1.5rem; min-width: 110px;
                    box-shadow: 0 1px 3px rgba(0,0,0,.12); }
  .summary .card .n { font-size: 1.8rem; font-weight: 700; display: block; }
  .summary .card.total .n { color: #4b5563; }
  .summary .card.passed .n { color: #16a34a; }
  .summary .card.failed .n { color: #dc2626; }
  .summary .card.skipped .n { color: #ca8a04; }
  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden;
          box-shadow: 0 1px 3px rgba(0,0,0,.12); }
  th, td { text-align: left; padding: .6rem .9rem; border-bottom: 1px solid rgba(127,127,127,.2); }
  th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; opacity: .7; }
  tr.passed .icon { color: #16a34a; }
  tr.failed .icon { color: #dc2626; }
  tr.skipped .icon { color: #ca8a04; }
  .icon { font-weight: 700; width: 1.5rem; }
  pre { white-space: pre-wrap; word-break: break-word; margin: 0; font-size: .85rem; opacity: .85; }
  .video video { max-width: 100%; border-radius: 8px; }
  .video { margin-top: 2rem; }
  footer { margin-top: 2rem; font-size: .8rem; opacity: .6; }
</style>
</head>
<body>
  <h1>Reporte de ejecución</h1>
  <p>${runAt}</p>
  <div class="summary">
    <div class="card total"><span class="n">${results.size()}</span>Total</div>
    <div class="card passed"><span class="n">${passed}</span>Passed</div>
    <div class="card failed"><span class="n">${failed}</span>Failed</div>
    <div class="card skipped"><span class="n">${skipped}</span>Skipped</div>
    <div class="card total"><span class="n">${(totalSec).round(1)}s</span>Duración</div>
  </div>
  <table>
    <thead>
      <tr><th></th><th>Test</th><th>Estado</th><th>Duración</th></tr>
    </thead>
    <tbody>${rows}
    </tbody>
  </table>${videoSection}
  <footer>Katalon Headless Runner</footer>
</body>
</html>
"""
        outputFile.text = html
        println "[Runner] Reporte HTML: ${outputFile.absolutePath}"
    }

    private static String escapeHtml(String s) {
        if (!s) return ''
        s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')
    }
}
