package com.kms.katalon.core.webui.keyword

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCaseEntity
import com.kms.katalon.core.testobject.SelectorMethod
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.OutputType
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import runner.WebDriverManager

import java.time.Duration

/**
 * Bridge de WebUiBuiltInKeywords de Katalon → Selenium 4.19 (WebDriver).
 *
 * Espejo de MobileBuiltInKeywords pero para el lado Web (Chrome/Firefox/Edge).
 * La sesión del navegador vive en WebDriverManager, independiente de la sesión
 * Appium — un mismo run puede mezclar test cases android/ y web/ sin que
 * ninguna de las dos plataformas fuerce a la otra a iniciar sesión.
 *
 * ScriptExecutor inyecta el `testCaseRunner` Closure antes de ejecutar scripts
 * (igual que antes — callTestCase() no cambia de comportamiento).
 */
class WebUiBuiltInKeywords {

    /**
     * Inyectado por ScriptExecutor.init():
     *   { String path, Map params, FailureHandling fh -> ... }
     */
    static Closure testCaseRunner

    // ── Sub-test case (sin cambios de comportamiento) ──────────────────────────

    /** Ejecuta otro test case como un paso del test actual. Equivale al "Call Test Case" de Katalon Studio. */
    static void callTestCase(TestCaseEntity testCase, Map params,
                              FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        if (!testCaseRunner) {
            throw new IllegalStateException(
                "WebUI.callTestCase: testCaseRunner no inicializado. " +
                "ScriptExecutor debe inicializarse antes de ejecutar tests."
            )
        }
        KeywordUtil.logInfo("callTestCase → ${testCase.path}")
        testCaseRunner(testCase.path, params ?: [:], fh)
    }

    // ── Browser lifecycle ────────────────────────────────────────────────────

    static void openBrowser(String url = '') {
        KeywordUtil.logInfo("openBrowser(${url ?: '<blank>'})")
        if (url) driver().get(url)
    }

    static void navigateToUrl(String url) {
        KeywordUtil.logInfo("navigateToUrl → ${url}")
        driver().get(url)
    }

    static void closeBrowser() {
        WebDriverManager.quit()
    }

    static void back() {
        driver().navigate().back()
    }

    static void refresh() {
        driver().navigate().refresh()
    }

    // ── Element interaction ──────────────────────────────────────────────────

    static void click(TestObject obj, FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, 10, fh)
        if (el) {
            el.click()
            KeywordUtil.logInfo("click → ${obj}")
        }
    }

    static void setText(TestObject obj, String text, FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, 10, fh)
        if (el) {
            el.clear()
            el.sendKeys(text)
            KeywordUtil.logInfo("setText → ${obj} = '${text}'")
        }
    }

    static String getText(TestObject obj, FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, 10, fh)
        return el?.text
    }

    static void scrollToElement(TestObject obj, FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        WebElement el = findWithWait(obj, 10, fh)
        if (el) {
            ((org.openqa.selenium.JavascriptExecutor) driver())
                .executeScript('arguments[0].scrollIntoView({block: "center"});', el)
            KeywordUtil.logInfo("scrollToElement → ${obj}")
        }
    }

    // ── Wait / verify ────────────────────────────────────────────────────────

    static boolean waitForElementVisible(TestObject obj, int timeoutSec,
                                          FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        try {
            By by = resolveLocator(obj)
            new WebDriverWait(driver(), Duration.ofSeconds(timeoutSec))
                .ignoring(NoSuchElementException)
                .ignoring(StaleElementReferenceException)
                .until(ExpectedConditions.visibilityOfElementLocated(by))
            return true
        } catch (TimeoutException e) {
            return handleWaitTimeout("Elemento no visible en ${timeoutSec}s: ${obj}", fh)
        }
    }

    static boolean waitForElementNotVisible(TestObject obj, int timeoutSec,
                                             FailureHandling fh = FailureHandling.OPTIONAL) {
        try {
            By by = resolveLocator(obj)
            new WebDriverWait(driver(), Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.invisibilityOfElementLocated(by))
            return true
        } catch (TimeoutException e) {
            return handleWaitTimeout("Elemento sigue visible después de ${timeoutSec}s: ${obj}", fh)
        }
    }

    static boolean verifyElementText(TestObject obj, String expectedText,
                                      FailureHandling fh = FailureHandling.STOP_ON_FAILURE) {
        String actual = getText(obj, fh)
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

    static boolean verifyTextPresent(String text, boolean isRegex = false) {
        try {
            String source = driver().pageSource
            return isRegex ? (source =~ text).find() : source.contains(text)
        } catch (Exception e) {
            KeywordUtil.logInfo("verifyTextPresent: ${e.message}")
            return false
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    static void comment(String message) { KeywordUtil.logInfo("[WebUI] ${message}") }

    static void delay(Number seconds) { Thread.sleep((seconds.doubleValue() * 1000).longValue()) }

    static void takeScreenshot() {
        try {
            File src  = ((TakesScreenshot) driver()).getScreenshotAs(OutputType.FILE)
            String ts = new java.text.SimpleDateFormat('yyyyMMdd_HHmmss_SSS').format(new Date())
            File dir  = new File(System.getProperty('runner.reportDir',
                System.getProperty('runner.projectDir', '.') + '/runner/reports'))
            dir.mkdirs()
            File dest = new File(dir, "screenshot_web_${ts}.png")
            src.renameTo(dest)
            KeywordUtil.logInfo("Screenshot: ${dest.absolutePath}")
        } catch (Exception e) {
            KeywordUtil.logInfo("takeScreenshot falló: ${e.message}")
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Privados — resolución de localizadores y helpers
    // ════════════════════════════════════════════════════════════════════════

    private static WebDriver driver() {
        WebDriverManager.getDriver()
    }

    /**
     * Resuelve el localizador Selenium de un TestObject.
     * Prioridad: ID > CSS > XPATH > NAME > CLASS_NAME
     *
     * Soporta TestObjects dinámicos (selectorMethod/selectorValue explícitos)
     * y cargados desde .rs XML (Object Repository/web/**), donde se espera
     * que el .rs tenga entradas bajo esas mismas claves (ID, CSS, XPATH, NAME,
     * CLASS_NAME) en su locatorCollection/selectorCollection.
     */
    static By resolveLocator(TestObject obj) {
        if (obj.selectorMethod != null && obj.selectorValue) {
            switch (obj.selectorMethod) {
                case SelectorMethod.ID:         return By.id(obj.selectorValue)
                case SelectorMethod.CSS:        return By.cssSelector(obj.selectorValue)
                case SelectorMethod.ATTRIBUTES:
                case SelectorMethod.XPATH:      return By.xpath(obj.selectorValue)
                case SelectorMethod.NAME:       return By.name(obj.selectorValue)
                case SelectorMethod.CLASS_NAME: return By.className(obj.selectorValue)
            }
        }

        Map<String, String> locs = obj.locators ?: [:]

        String id = locs['ID']
        if (id)    return By.id(id)

        String css = locs['CSS']
        if (css)   return By.cssSelector(css)

        String xpath = locs['XPATH'] ?: locs['ATTRIBUTES']
        if (xpath) return By.xpath(xpath)

        String name = locs['NAME']
        if (name)  return By.name(name)

        String className = locs['CLASS_NAME']
        if (className) return By.className(className)

        throw new RuntimeException(
            "No se encontró un localizador Web válido para: ${obj.objectId}\n" +
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

    private static boolean handleWaitTimeout(String msg, FailureHandling fh) {
        switch (fh) {
            case FailureHandling.STOP_ON_FAILURE:
                throw new AssertionError(msg)
            case FailureHandling.CONTINUE_ON_FAILURE:
                KeywordUtil.markFailed(msg)
                return false
            default: // OPTIONAL
                return false
        }
    }
}
