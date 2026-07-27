package com.kms.katalon.core.mobile.keyword

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.SelectorMethod
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import internal.GlobalVariable
import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.nativekey.AndroidKey
import io.appium.java_client.android.nativekey.KeyEvent
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.OutputType
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import runner.AppiumDriverManager

import java.time.Duration

/**
 * Bridge de MobileBuiltInKeywords de Katalon → Appium Java Client 9.x
 *
 * Implementa los 22 métodos usados en el proyecto.
 * Todos los métodos son estáticos para replicar el uso como:
 *   import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
 *   Mobile.tap(obj, 5)
 */
class MobileBuiltInKeywords {

    // ── App lifecycle ────────────────────────────────────────────────────────

    static void startExistingApplication(String bundleId,
                                         FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        try {
            KeywordUtil.logInfo("Activando app: ${bundleId}")
            driver().activateApp(bundleId)
        } catch (Exception e) {
            handleFailure("No se pudo activar la app '${bundleId}': ${e.message}", fh, e)
        }
    }

    static void closeApplication() {
        try {
            String bundleId = safeGetVar('G_AppBundleID', '')
            KeywordUtil.logInfo("Cerrando app: ${bundleId}")
            driver().terminateApp(bundleId)
        } catch (Exception e) {
            KeywordUtil.logInfo("closeApplication: ${e.message}")
        }
    }

    // ── Element interaction ──────────────────────────────────────────────────

    static void tap(TestObject obj, int timeoutSec,
                    FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, timeoutSec, fh)
        if (el) {
            el.click()
            KeywordUtil.logInfo("tap → ${obj}")
        }
    }

    static void tapAtPosition(int x, int y) {
        KeywordUtil.logInfo("tapAtPosition(${x}, ${y})")
        PointerInput finger   = new PointerInput(PointerInput.Kind.TOUCH, 'finger')
        Sequence    sequence  = new Sequence(finger, 0)
        sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        sequence.addAction(new Pause(finger, Duration.ofMillis(100)))
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
        driver().perform([sequence])
    }

    static void tapAndHold(TestObject obj, int holdDurationSec, int timeoutSec,
                            FailureHandling fh = FailureHandling.OPTIONAL) {
        WebElement el = findWithWait(obj, timeoutSec, fh)
        if (!el) return

        int[] center = elementCenter(el)
        PointerInput finger  = new PointerInput(PointerInput.Kind.TOUCH, 'finger')
        Sequence    sequence = new Sequence(finger, 0)
        sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center[0], center[1]))
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        sequence.addAction(new Pause(finger, Duration.ofSeconds(holdDurationSec)))
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
        driver().perform([sequence])
        KeywordUtil.logInfo("tapAndHold(${holdDurationSec}s) → ${obj}")
    }

    static void setText(TestObject obj, String text, int timeoutSec,
                        FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, timeoutSec, fh)
        if (el) {
            el.clear()
            el.sendKeys(text)
            KeywordUtil.logInfo("setText → ${obj} = '${text}'")
        }
    }

    // ── Wait / verify ────────────────────────────────────────────────────────

    static boolean waitForElementPresent(TestObject obj, int timeoutSec,
                                         FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        try {
            By by = resolveLocator(obj)
            new WebDriverWait(driver(), Duration.ofSeconds(timeoutSec))
                .ignoring(NoSuchElementException)
                .ignoring(StaleElementReferenceException)
                .until(ExpectedConditions.presenceOfElementLocated(by))
            return true
        } catch (TimeoutException e) {
            switch (fh) {
                case FailureHandling.STOP_ON_FAILURE:
                    throw new AssertionError("Elemento no encontrado en ${timeoutSec}s: ${obj}")
                case FailureHandling.CONTINUE_ON_FAILURE:
                    KeywordUtil.markFailed("Elemento no encontrado: ${obj}")
                    return false
                default: // OPTIONAL
                    return false
            }
        }
    }

    static boolean waitForElementNotPresent(TestObject obj, int timeoutSec,
                                             FailureHandling fh = FailureHandling.OPTIONAL) {
        try {
            By by = resolveLocator(obj)
            new WebDriverWait(driver(), Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.invisibilityOfElementLocated(by))
            return true
        } catch (TimeoutException e) {
            switch (fh) {
                case FailureHandling.STOP_ON_FAILURE:
                    throw new AssertionError("Elemento sigue presente después de ${timeoutSec}s: ${obj}")
                case FailureHandling.CONTINUE_ON_FAILURE:
                    KeywordUtil.markFailed("Elemento sigue presente: ${obj}")
                    return false
                default: // OPTIONAL
                    return false
            }
        }
    }

    static boolean verifyElementText(TestObject obj, String expectedText,
                                     FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        String actual = getText(obj, 10, fh)
        if (actual == expectedText) return true

        String msg = "Texto incorrecto. Esperado: '${expectedText}', Actual: '${actual}'"
        switch (fh) {
            case FailureHandling.STOP_ON_FAILURE:
                throw new AssertionError(msg)
            case FailureHandling.CONTINUE_ON_FAILURE:
                KeywordUtil.markFailed(msg)
                return false
            default:
                KeywordUtil.logInfo("verifyElementText (OPTIONAL) fallido: ${msg}")
                return false
        }
    }

    // ── Scroll & navigation ──────────────────────────────────────────────────

    /**
     * Hace fling al tope del primer scrollable container visible.
     * Usa UIAutomator UiScrollable.flingToBeginning() — opera DENTRO del contenedor
     * sin salir de la pantalla actual. Silencioso si no hay scrollable.
     */
    static void scrollToTop() {
        try {
            String uiAuto = "new UiScrollable(new UiSelector().scrollable(true).instance(0)).flingToBeginning(10)"
            driver().findElement(AppiumBy.androidUIAutomator(uiAuto))
            KeywordUtil.logInfo("scrollToTop completado (flingToBeginning)")
        } catch (Exception e) {
            KeywordUtil.logInfo("scrollToTop: ${e.message} (no bloqueante)")
        }
    }

    static void scrollToText(String text, FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        try {
            String uiAutomator = "new UiScrollable(new UiSelector().scrollable(true))" +
                ".scrollIntoView(new UiSelector().textContains(\"${text}\"))"
            driver().findElement(AppiumBy.androidUIAutomator(uiAutomator))
            KeywordUtil.logInfo("scrollToText → '${text}'")
        } catch (Exception e) {
            switch (fh) {
                case FailureHandling.STOP_ON_FAILURE:
                    throw new AssertionError("No se pudo hacer scroll hasta '${text}': ${e.message}")
                case FailureHandling.CONTINUE_ON_FAILURE:
                    KeywordUtil.markFailed("scrollToText fallido: '${text}'")
                    break
                default: // OPTIONAL — silencioso
                    KeywordUtil.logInfo("scrollToText (OPTIONAL) no encontró: '${text}'")
            }
        }
    }

    static void swipe(int startX, int startY, int endX, int endY) {
        KeywordUtil.logInfo("swipe (${startX},${startY}) → (${endX},${endY})")
        PointerInput finger  = new PointerInput(PointerInput.Kind.TOUCH, 'finger')
        Sequence    sequence = new Sequence(finger, 0)
        sequence.addAction(finger.createPointerMove(Duration.ZERO,        PointerInput.Origin.viewport(), startX, startY))
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
        sequence.addAction(new Pause(finger, Duration.ofMillis(300)))
        sequence.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY))
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
        driver().perform([sequence])
    }

    static void pressBack() {
        KeywordUtil.logInfo("pressBack()")
        driver().pressKey(new KeyEvent(AndroidKey.BACK))
    }

    // ── Device info ──────────────────────────────────────────────────────────

    static int getDeviceWidth() {
        driver().manage().window().getSize().width
    }

    static int getDeviceHeight() {
        driver().manage().window().getSize().height
    }

    static String getDeviceOS() {
        safeGetVar('G_Platform', 'android')
    }

    // ── Attributes & text ────────────────────────────────────────────────────

    static String getElementAttribute(TestObject obj, String attributeName,
                                       int timeoutSec = 10) {
        WebElement el = findWithWait(obj, timeoutSec, FailureHandling.STOP_ON_FAILURE)
        try {
            return el?.getAttribute(attributeName)
        } catch (StaleElementReferenceException e) {
            // Compose puede re-renderizar el elemento justo después de encontrarlo — reintentar una vez
            // Usar timeout corto (2s) para no bloquear el flujo: si no está, caerá al fallback del llamador
            KeywordUtil.logInfo("getElementAttribute: StaleElement en getAttribute — refetching (2s)")
            el = findWithWait(obj, 2, FailureHandling.STOP_ON_FAILURE)
            return el?.getAttribute(attributeName)
        }
    }

    static String getText(TestObject obj,
                          int timeoutSec = 10,
                          FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, timeoutSec, fh)
        return el?.text
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    static void delay(Number seconds) {
        Thread.sleep((seconds.doubleValue() * 1000).longValue())
    }

    static void comment(String message) {
        println "  [STEP]    ${message}"
    }

    static void takeScreenshot() {
        try {
            File src  = ((TakesScreenshot) driver()).getScreenshotAs(OutputType.FILE)
            String ts = new java.text.SimpleDateFormat('yyyyMMdd_HHmmss_SSS').format(new Date())
            File   dir = new File(System.getProperty('runner.reportDir',
                System.getProperty('runner.projectDir', '.') + '/runner/reports'))
            dir.mkdirs()
            File dest = new File(dir, "screenshot_${ts}.png")
            src.renameTo(dest)
            KeywordUtil.logInfo("Screenshot: ${dest.absolutePath}")
        } catch (Exception e) {
            KeywordUtil.logInfo("takeScreenshot falló: ${e.message}")
        }
    }

    static void hideKeyboard() {
        try {
            driver().hideKeyboard()
        } catch (Exception e) {
            KeywordUtil.logInfo("hideKeyboard: ${e.message} (ignorado)")
        }
    }

    static void setMobileCapability(String key, Object value) {
        // Las capabilities se configuran antes de iniciar la sesión.
        // En modo headless este método es no-op.
        KeywordUtil.logInfo("[setMobileCapability] ${key}=${value} (ignorado en runner)")
    }

    // ════════════════════════════════════════════════════════════════════════
    // Privados — resolución de localizadores y helpers
    // ════════════════════════════════════════════════════════════════════════

    private static AndroidDriver driver() {
        AppiumDriverManager.getDriver()
    }

    /**
     * Resuelve el localizador Appium de un TestObject.
     * Prioridad: ANDROID_UI_AUTOMATOR > ACCESSIBILITY > ID > ATTRIBUTES > XPATH
     *
     * Soporta TestObjects dinámicos (de LocatorHelper) y cargados desde .rs XML.
     */
    static By resolveLocator(TestObject obj) {
        // TestObject dinámico con selectorMethod/selectorValue explícitos
        if (obj.selectorMethod != null && obj.selectorValue) {
            switch (obj.selectorMethod) {
                case SelectorMethod.ACCESSIBILITY:
                    return AppiumBy.accessibilityId(obj.selectorValue)
                case SelectorMethod.ANDROID_UI_AUTOMATOR:
                    return AppiumBy.androidUIAutomator(obj.selectorValue)
                case SelectorMethod.ATTRIBUTES:
                case SelectorMethod.XPATH:
                    return AppiumBy.xpath(obj.selectorValue)
                case SelectorMethod.ID:
                    return AppiumBy.id(obj.selectorValue)
                case SelectorMethod.CLASS_NAME:
                    return By.className(obj.selectorValue)
            }
        }

        // TestObject cargado desde .rs XML — usa locators map con params resueltos
        Map<String, String> locs   = obj.locators ?: [:]
        Map<String, String> params = obj.params   ?: [:]

        String uiAuto = resolve(locs['ANDROID_UI_AUTOMATOR'], params)
        if (uiAuto)  return AppiumBy.androidUIAutomator(uiAuto)

        String access = resolve(locs['ACCESSIBILITY'], params)
        if (access)  return AppiumBy.accessibilityId(access)

        String id = resolve(locs['ID'], params)
        if (id)      return AppiumBy.id(id)

        // ATTRIBUTES y XPATH son XPath en Appium
        String attrs = resolve(locs['ATTRIBUTES'], params)
        if (attrs)   return AppiumBy.xpath(attrs)

        String xpath = resolve(locs['XPATH'], params)
        if (xpath)   return AppiumBy.xpath(xpath)

        throw new RuntimeException(
            "No se encontró un localizador válido para: ${obj.objectId}\n" +
            "Localizadores disponibles: ${locs}"
        )
    }

    private static WebElement findWithWait(TestObject obj, int timeoutSec, FailureHandling fh) {
        try {
            By by = resolveLocator(obj)
            return new WebDriverWait(driver(), Duration.ofSeconds(timeoutSec))
                .ignoring(NoSuchElementException)
                .ignoring(StaleElementReferenceException)
                .until(ExpectedConditions.presenceOfElementLocated(by))
        } catch (TimeoutException e) {
            switch (fh) {
                case FailureHandling.STOP_ON_FAILURE:
                    throw new AssertionError("Elemento no encontrado en ${timeoutSec}s: ${obj}")
                case FailureHandling.CONTINUE_ON_FAILURE:
                    KeywordUtil.markFailed("Elemento no encontrado: ${obj}")
                    return null
                default: // OPTIONAL
                    return null
            }
        }
    }

    private static void handleFailure(String msg, FailureHandling fh, Exception cause = null) {
        switch (fh) {
            case FailureHandling.STOP_ON_FAILURE:
                throw new AssertionError(msg, cause)
            case FailureHandling.CONTINUE_ON_FAILURE:
                KeywordUtil.markFailed(msg)
                break
            default:
                KeywordUtil.logInfo("OPTIONAL: ${msg}")
        }
    }

    /** Interpola variables de parámetro en la cadena del localizador. */
    private static String resolve(String template, Map<String, String> params) {
        if (!template?.trim()) return null
        String result = template
        params?.each { k, v ->
            result = result
                .replace("\${${k}}", v ?: '')
                .replace("(${k})", v ?: '')
        }
        return result.trim() ?: null
    }

    /** Centro de un elemento para gestos táctiles. */
    private static int[] elementCenter(WebElement el) {
        def rect = el.getRect()
        [rect.x + rect.width / 2, rect.y + rect.height / 2] as int[]
    }

    private static String safeGetVar(String name, String defaultValue) {
        try {
            return GlobalVariable."${name}"?.toString() ?: defaultValue
        } catch (MissingPropertyException e) {
            return defaultValue
        }
    }
}
