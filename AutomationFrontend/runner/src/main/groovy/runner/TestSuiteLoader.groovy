package runner

/**
 * Carga Test Suites nativos de Katalon (.ts) y los resuelve a la lista de
 * TestCase que referencian, reutilizando TestCaseLoader para cada uno.
 *
 * Mapeo de rutas:
 *   Test Suites/android/Smoke/Android-Smoke.ts
 *     → cada <testCaseLink><testCaseId>Test Cases/android/...</testCaseId></testCaseLink>
 */
class TestSuiteLoader {

    private final File projectRoot
    private final TestCaseLoader testCaseLoader

    TestSuiteLoader(File projectRoot) {
        this.projectRoot = projectRoot
        this.testCaseLoader = new TestCaseLoader(projectRoot)
    }

    /**
     * Carga un test suite por su ruta relativa (con o sin prefijo "Test Suites/",
     * con o sin extensión .ts) y resuelve cada testCaseLink con isRun=true a su
     * TestCase correspondiente. Un testCaseId que ya no exista se reporta como
     * advertencia y se omite — no aborta el resto del suite (mismo criterio
     * tolerante que TestCaseLoader.loadAll()).
     */
    List<TestCase> load(String relativePath) {
        String cleanPath = relativePath
            .replaceFirst(/^Test Suites\//, '')
            .replaceFirst(/\.ts$/, '')

        File tsFile = new File(projectRoot, "Test Suites/${cleanPath}.ts")
        if (!tsFile.exists()) {
            throw new FileNotFoundException(
                "Test suite no encontrado: Test Suites/${cleanPath}.ts\n" +
                "Buscado en: ${tsFile.absolutePath}"
            )
        }

        def xml = new XmlSlurper().parse(tsFile)
        String suiteName = xml.name?.text()?.trim() ?: tsFile.name

        List<TestCase> cases = []
        xml.testCaseLink.each { link ->
            boolean isRun = link.isRun?.text()?.trim() != 'false'  // default true si el nodo falta
            if (!isRun) return

            String testCaseId = link.testCaseId?.text()?.trim()
            if (!testCaseId) return

            try {
                cases << testCaseLoader.load(testCaseId)
            } catch (FileNotFoundException e) {
                println "[Runner] Advertencia: suite '${suiteName}' referencia un test case inexistente: ${testCaseId}"
            }
        }
        return cases
    }
}
