package runner

/**
 * Carga y filtra TestCases del proyecto Katalon.
 *
 * Mapeo de rutas:
 *   Test Cases/android/TC_Foo.tc
 *     → Scripts/android/TC_Foo/Script*.groovy
 */
class TestCaseLoader {

    private final File projectRoot

    TestCaseLoader(File projectRoot) {
        this.projectRoot = projectRoot
    }

    /** Carga un test case específico por su ruta relativa (sin extensión .tc). */
    TestCase load(String relativePath) {
        // Acepta con o sin prefijo "Test Cases/"
        String cleanPath = relativePath
            .replaceFirst(/^Test Cases\//, '')
            .replaceFirst(/\.tc$/, '')

        File tcFile = new File(projectRoot, "Test Cases/${cleanPath}.tc")
        if (!tcFile.exists()) {
            throw new FileNotFoundException(
                "Test case no encontrado: Test Cases/${cleanPath}.tc\n" +
                "Buscado en: ${tcFile.absolutePath}"
            )
        }
        return parseTestCase(tcFile)
    }

    /** Carga todos los test cases del proyecto. */
    List<TestCase> loadAll() {
        List<TestCase> cases = []
        File tcDir = new File(projectRoot, 'Test Cases')
        if (!tcDir.exists()) return cases

        tcDir.eachFileRecurse { File file ->
            if (file.name.endsWith('.tc')) {
                try {
                    cases << parseTestCase(file)
                } catch (Exception e) {
                    println "[Runner] Advertencia: no se pudo cargar ${file.name}: ${e.message}"
                }
            }
        }
        return cases
    }

    /** Filtra test cases que tengan el tag dado. */
    List<TestCase> loadByTag(String tag) {
        loadAll().findAll { tc -> tc.tags?.contains(tag) }
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private TestCase parseTestCase(File tcFile) {
        def xml = new XmlSlurper().parse(tcFile)

        // Ruta relativa a "Test Cases/"
        String tcRelPath = projectRoot.toPath()
            .resolve('Test Cases')
            .relativize(tcFile.toPath())
            .toString()
            .replaceAll(/\.tc$/, '')

        // Los Scripts viven en: Scripts/<misma ruta relativa>/Script*.groovy
        File scriptDir = new File(projectRoot, "Scripts/${tcRelPath}")
        File scriptFile = null
        if (scriptDir.exists()) {
            scriptFile = scriptDir.listFiles()
                ?.find { it.name.startsWith('Script') && it.name.endsWith('.groovy') }
        }

        // Tags: la XML viene como "smoke,turbo,android" en <tag>
        String rawTag = xml.tag?.text()?.trim() ?: ''
        List<String> tags = rawTag
            ? rawTag.split(',').collect { it.trim() }.findAll { it }
            : []

        return new TestCase(
            name:        xml.name?.text()?.trim() ?: tcFile.name,
            path:        tcRelPath,
            description: xml.description?.text()?.trim() ?: '',
            tags:        tags,
            scriptFile:  scriptFile,
            platform:    derivePlatform(tcRelPath)
        )
    }

    /**
     * Deriva la plataforma del primer segmento de la ruta relativa, siguiendo
     * la misma convención ya usada por Keywords (com.tuempresa.page.android/.ios/.common)
     * y Object Repository (android/, ios/, web/):
     *   android/TC_Foo             → ANDROID
     *   web/checkout/TC_Bar        → WEB
     *   ios/TC_Baz                 → IOS
     *   cualquier otro prefijo     → UNKNOWN (no se inicializa ningún driver)
     */
    static Platform derivePlatform(String relativePath) {
        String first = relativePath.split('/')[0]?.toLowerCase()
        switch (first) {
            case 'android': return Platform.ANDROID
            case 'ios':     return Platform.IOS
            case 'web':     return Platform.WEB
            default:        return Platform.UNKNOWN
        }
    }
}
