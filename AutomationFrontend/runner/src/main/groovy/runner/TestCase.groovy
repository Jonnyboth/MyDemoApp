package runner

/**
 * Representa un test case cargado del proyecto Katalon.
 * Combina la metadata del .tc XML con el archivo .groovy ejecutable.
 */
class TestCase {
    String name
    String path         // relativo a 'Test Cases/', sin extensión
    String description
    List<String> tags   // ej: ['smoke', 'turbo', 'android']
    File   scriptFile   // Scripts/.../Script*.groovy

    /**
     * Plataforma derivada del primer segmento de 'path' (ej. "android/TC_Foo" → ANDROID).
     * Determina qué DriverManager (Appium o Selenium) se inicializa antes de correr
     * este test case. Ver TestCaseLoader.derivePlatform() y KatalonRunner.
     */
    Platform platform
}

enum Platform {
    ANDROID, IOS, WEB, UNKNOWN
}
