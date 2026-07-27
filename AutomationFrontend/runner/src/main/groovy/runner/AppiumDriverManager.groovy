package runner

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.remote.DesiredCapabilities

import java.time.Duration

/**
 * Gestiona la sesión de Appium como singleton por hilo de ejecución.
 *
 * Uso:
 *   AppiumDriverManager.init(config)    ← llamado por KatalonRunner
 *   AppiumDriverManager.getDriver()     ← llamado por MobileBuiltInKeywords
 *   AppiumDriverManager.quit()          ← llamado al terminar
 */
class AppiumDriverManager {

    private static AndroidDriver driver

    /**
     * Inicializa la sesión Appium usando la configuración de runner.yml.
     * @param config Map con secciones 'appium' y 'device'
     */
    static void init(Map<String, Object> config) {
        if (driver) {
            println "[Runner] Sesión Appium ya activa — reutilizando"
            return
        }

        Map appiumCfg = (config.appium ?: [:]) as Map
        Map deviceCfg = (config.device ?: [:]) as Map

        String url = appiumCfg.url ?: 'http://localhost:4723'

        UiAutomator2Options options = new UiAutomator2Options()

        // Device
        if (deviceCfg.udid)            options.setUdid(deviceCfg.udid.toString())
        if (deviceCfg.platformVersion) options.setPlatformVersion(deviceCfg.platformVersion.toString())
        if (deviceCfg.appPackage)      options.setAppPackage(deviceCfg.appPackage.toString())
        if (deviceCfg.appActivity)     options.setAppActivity(deviceCfg.appActivity.toString())

        // Comportamiento de sesión
        boolean noReset    = deviceCfg.noReset   != null ? deviceCfg.noReset    as boolean : true
        boolean fullReset  = deviceCfg.fullReset  != null ? deviceCfg.fullReset  as boolean : false
        boolean autoGrant  = deviceCfg.autoGrantPermissions != null ? deviceCfg.autoGrantPermissions as boolean : true

        options.setNoReset(noReset)
        options.setFullReset(fullReset)
        if (autoGrant) options.setCapability('autoGrantPermissions', true)

        // Timeout de comandos
        int cmdTimeout = (appiumCfg.newCommandTimeout ?: 300) as int
        options.setNewCommandTimeout(Duration.ofSeconds(cmdTimeout))

        println "[Runner] Conectando a Appium: ${url}"
        println "[Runner] Dispositivo: ${deviceCfg.udid ?: 'default'} | App: ${deviceCfg.appPackage ?: '?'}"

        driver = new AndroidDriver(new URL(url), options)
        println "[Runner] Sesión iniciada: ${driver.sessionId}"
    }

    static AndroidDriver getDriver() {
        if (!driver) {
            throw new IllegalStateException(
                "AppiumDriverManager no inicializado. " +
                "Llama a AppiumDriverManager.init(config) antes de ejecutar tests."
            )
        }
        return driver
    }

    static void quit() {
        if (driver) {
            try {
                driver.quit()
                println "[Runner] Sesión Appium cerrada"
            } catch (Exception e) {
                println "[Runner] Error cerrando sesión: ${e.message}"
            } finally {
                driver = null
            }
        }
    }

    static boolean isActive() {
        driver != null
    }
}
