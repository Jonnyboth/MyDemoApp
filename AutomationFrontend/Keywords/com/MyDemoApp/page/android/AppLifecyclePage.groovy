package com.MyDemoApp.page.android

import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import MyDemoApp.utils.SmartWaitPage

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

/**
 * Ciclo de vida de la app MyDemoApp (Android): cerrar + borrar datos (cache,
 * sesion, carrito, preferencias) a nivel de sistema Android + reabrir, para
 * dejarla en un estado base limpio antes de cada test. No importa
 * `internal.GlobalVariable` -- Katalon puede compilar este Keyword antes que
 * `internal/GlobalVariable.groovy` y lanzar ClassNotFoundException; por eso
 * se accede a las variables globales via reflection en runtime (ver
 * globalVar()) en vez de un import estatico arriba.
 */
public class AppLifecyclePage {

    private static final String MENU_ICON = 'Object Repository/android/Menu/btn_menuIcon'

    private ProductsPage productsPage = new ProductsPage()

    /**
     * Cierra la app (mata el proceso), borra TODOS sus datos via
     * `adb shell pm clear` (cache, base de datos local, SharedPreferences,
     * sesion, carrito -- todo lo que un TC anterior pudo haber dejado) y la
     * vuelve a abrir desde cero. `pm clear` deja la app en el mismo estado
     * que una instalacion recien hecha, asi que no hace falta detectar sesion
     * activa ni items en el carrito via UI (a diferencia de un logout/vaciado
     * manual, esto no depende de que la UI del TC anterior haya quedado en un
     * estado navegable). closeApplication() exige una sesion Appium ya
     * activa -- si este es el primer test de la corrida (sin driver
     * todavia), lanza "No driver found"; en ese caso no hay nada que cerrar,
     * asi que el fallo se ignora.
     */
    void restartApp() {
        try {
            Mobile.closeApplication()
        } catch (Exception e) {
            // Sin sesion previa (primer test de la corrida) -- no hay app que cerrar.
        }
        clearAppData()
        Mobile.startExistingApplication(appBundleId())
        SmartWaitPage.waitVisible(findTestObject(MENU_ICON), SmartWaitPage.MEDIUM)
        productsPage.ensureOnProductsScreen()
    }

    // -- Privado -----------------------------------------------------------

    /**
     * Borra cache, base de datos, SharedPreferences y sesion de la app a
     * nivel de sistema Android (`pm clear`), usando el mismo patron
     * ProcessBuilder que MyDemoApp.utils.VideoRecorderUtil para invocar adb.
     * A diferencia de la grabacion de video (best-effort), un fallo aca SI
     * debe frenar el test: si no se puede garantizar el estado base limpio,
     * seguir corriendo daria resultados falsos.
     */
    private void clearAppData() {
        String pkg = appBundleId()
        String adbPath = resolveAdbExecutable()
        List<String> command = [adbPath, '-s', deviceUdid(), 'shell', 'pm', 'clear', pkg]

        Process process
        try {
            ProcessBuilder pb = new ProcessBuilder(command)
            pb.redirectErrorStream(true)
            process = pb.start()
        } catch (IOException e) {
            throw new RuntimeException(
                "No se encontro el ejecutable 'adb' ('${adbPath}'). Katalon Studio no " +
                "siempre hereda el PATH de la terminal (sobre todo en Windows), asi que " +
                "agregar adb al PATH del usuario puede no alcanzar. Define la variable de " +
                "entorno de sistema ANDROID_HOME o ANDROID_SDK_ROOT apuntando a tu Android " +
                "SDK (la carpeta que contiene 'platform-tools/') y reinicia Katalon Studio. " +
                "Causa original: ${e.message}", e)
        }

        String output = process.inputStream.text.trim()
        int exitCode = process.waitFor()
        if (exitCode != 0 || !output.contains('Success')) {
            throw new RuntimeException(
                "No se pudieron borrar los datos de '${pkg}' (adb pm clear). " +
                "exit=${exitCode}, output='${output}'. Verifica que el dispositivo " +
                "'${deviceUdid()}' este conectado (adb devices).")
        }
    }

    /**
     * Resuelve la ruta del ejecutable adb sin depender de que el proceso de
     * Katalon Studio herede el PATH de la terminal (en Windows, la app GUI
     * suele arrancar con un entorno distinto al del shell del usuario).
     * Prioriza ANDROID_HOME / ANDROID_SDK_ROOT (variables de entorno de
     * sistema, visibles para cualquier proceso) y solo si ninguna esta
     * definida cae a confiar en el PATH, que es lo que ya funcionaba en el
     * runner headless (Linux/WSL).
     */
    private String resolveAdbExecutable() {
        boolean isWindows = System.getProperty('os.name')?.toLowerCase()?.contains('windows')
        String adbFileName = isWindows ? 'adb.exe' : 'adb'
        for (String envVar : ['ANDROID_HOME', 'ANDROID_SDK_ROOT']) {
            String sdkHome = System.getenv(envVar)
            if (!sdkHome) {
                continue
            }
            File candidate = new File(sdkHome, "platform-tools/${adbFileName}")
            if (candidate.exists()) {
                return candidate.absolutePath
            }
        }
        return 'adb'
    }

    private String appBundleId() {
        globalVar('G_AppBundleID')
    }

    private String deviceUdid() {
        globalVar('G_DevicesName')
    }

    /**
     * internal.GlobalVariable tiene forma distinta segun donde corra el test:
     * - Katalon Studio real: clase regenerada por el IDE con campos estaticos
     *   publicos (`public static Object G_NombreVariable`).
     * - Runner headless propio: Map interno expuesto solo via getAll().
     * Se prueba primero el campo (entorno real) y se cae a getAll() si no
     * existe (runner propio), para que el mismo Keyword funcione en ambos.
     */
    private String globalVar(String name) {
        Class globalVariableClass = Class.forName('internal.GlobalVariable')
        try {
            return globalVariableClass.getField(name).get(null)
        } catch (NoSuchFieldException ignored) {
            Map<String, Object> vars = globalVariableClass.getMethod('getAll').invoke(null)
            return vars.get(name)
        }
    }
}
