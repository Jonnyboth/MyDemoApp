package runner

import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions

import java.time.Duration

/**
 * Gestiona la sesión de Selenium (navegador) como singleton por hilo de ejecución.
 * Espejo de AppiumDriverManager pero para el lado Web.
 *
 * Uso:
 *   WebDriverManager.init(config)    ← llamado por KatalonRunner (perezoso, solo si hay TCs web)
 *   WebDriverManager.getDriver()     ← llamado por WebUiBuiltInKeywords
 *   WebDriverManager.quit()          ← llamado al terminar
 */
class WebDriverManager {

    private static WebDriver driver

    /**
     * Inicializa la sesión del navegador usando la sección 'web' de runner.yml.
     * @param config Map con la sección 'web' (browser, headless, windowSize, pageLoadTimeout, binaryPath)
     */
    static void init(Map<String, Object> config) {
        if (driver) {
            println "[Runner] Sesión Web ya activa — reutilizando"
            return
        }

        Map webCfg = (config?.web ?: [:]) as Map
        String browser   = (webCfg.browser ?: 'chrome').toString().toLowerCase()
        boolean headless = webCfg.headless != null ? webCfg.headless as boolean : true
        String binaryPath = webCfg.binaryPath?.toString()
        String userAgent  = webCfg.userAgent?.toString()

        println "[Runner] Iniciando navegador: ${browser} (headless=${headless})"

        driver = buildDriver(browser, headless, binaryPath, userAgent)

        int pageLoadTimeout = (webCfg.pageLoadTimeout ?: 30) as int
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout))

        String windowSize = webCfg.windowSize?.toString()
        if (windowSize) {
            def (w, h) = windowSize.split('[,x]').collect { it.trim() as int }
            driver.manage().window().setSize(new Dimension(w, h))
        } else if (!headless) {
            driver.manage().window().maximize()
        }

        println "[Runner] Sesión Web iniciada (${browser})"
    }

    static WebDriver getDriver() {
        if (!driver) {
            throw new IllegalStateException(
                "WebDriverManager no inicializado. " +
                "Llama a WebDriverManager.init(config) antes de ejecutar tests web."
            )
        }
        return driver
    }

    static void quit() {
        if (driver) {
            try {
                driver.quit()
                println "[Runner] Sesión Web cerrada"
            } catch (Exception e) {
                println "[Runner] Error cerrando sesión Web: ${e.message}"
            } finally {
                driver = null
            }
        }
    }

    static boolean isActive() {
        driver != null
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    /**
     * binaryPath permite apuntar a un binario de navegador ya presente en el host
     * (ej. el Chromium cacheado por Playwright en entornos sandboxed sin Chrome
     * del sistema) en vez de depender de que Selenium Manager descargue uno.
     * Si no se especifica, Selenium Manager (incluido desde Selenium 4.6) resuelve
     * y descarga el driver/binario correcto automáticamente — no requiere
     * instalar chromedriver/geckodriver a mano.
     *
     * userAgent (opcional, solo aplica en modo headless): el modo `--headless=new` de Chrome
     * reporta por defecto un User-Agent con el literal "HeadlessChrome/<version>" — se confirmó
     * empíricamente (ver diagnóstico del run QA-20260710-mercadolibre-camara-carrito) que al menos
     * un sitio real (mercadolibre.com.co) bloquea ese User-Agent a nivel de servidor y devuelve una
     * página de error genérica ("Hubo un error accediendo a esta página...") en vez de la página
     * real, de forma 100% reproducible (6/6 intentos con el UA por defecto, 0/0 fallos tras fijar un
     * User-Agent de Chrome de escritorio normal vía `web.userAgent` en runner.yml). No es un
     * bypass de seguridad: mismo binario, mismos flags de sandboxing — solo evita que el propio
     * modo headless de Chrome se auto-delate en el header User-Agent.
     */
    private static WebDriver buildDriver(String browser, boolean headless, String binaryPath, String userAgent) {
        switch (browser) {
            case 'firefox':
                FirefoxOptions ffOptions = new FirefoxOptions()
                if (headless) ffOptions.addArguments('-headless')
                if (binaryPath) ffOptions.setBinary(binaryPath)
                return new FirefoxDriver(ffOptions)

            case 'edge':
                EdgeOptions edgeOptions = new EdgeOptions()
                if (headless) edgeOptions.addArguments('--headless=new')
                if (binaryPath) edgeOptions.setBinary(binaryPath)
                if (headless && userAgent) edgeOptions.addArguments("--user-agent=${userAgent}".toString())
                return new EdgeDriver(edgeOptions)

            case 'chrome':
            default:
                ChromeOptions chromeOptions = new ChromeOptions()
                if (headless) chromeOptions.addArguments('--headless=new')
                chromeOptions.addArguments('--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu')
                if (binaryPath) chromeOptions.setBinary(binaryPath)
                if (headless && userAgent) chromeOptions.addArguments("--user-agent=${userAgent}".toString())
                return new ChromeDriver(chromeOptions)
        }
    }
}
