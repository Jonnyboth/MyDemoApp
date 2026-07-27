package runner

import groovy.xml.MarkupBuilder

/**
 * Genera reportes de los resultados de ejecución.
 *
 * Formatos:
 *   - Consola  (siempre)
 *   - JUnit XML (compatible con CI/CD: GitHub Actions, Jenkins, etc.)
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
}
