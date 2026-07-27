# Katalon Headless Runner

Ejecuta tests de Katalon Studio en **dispositivos reales sin abrir la UI** de Katalon. Compila los scripts Groovy del proyecto en tiempo real y los corre contra Appium.

---

## ¿Qué hace?

Toma el proyecto Katalon existente tal como está (Keywords, Scripts, Object Repository, Profiles) y ejecuta los tests desde terminal — sin licencia Enterprise, sin interfaz gráfica, sin plugins adicionales.

```
bash runner/run.sh TC_NombreDelTest
bash runner/run.sh list
```

---

## Arquitectura en 5 piezas

```
run.sh
  └─► java -jar runner-all.jar
          │
          ├─ KatalonRunner         — Parsea CLI, orquesta todo
          ├─ TestCaseLoader        — Encuentra Script.groovy a partir del .tc
          ├─ ScriptExecutor        — Compila on-demand con GroovyClassLoader
          │     └─ Bridges Katalon — Simulan la API de Katalon en JVM standalone
          │          ├─ MobileBuiltInKeywords   → 21 keywords Appium (tap/scroll/etc.)
          │          ├─ ObjectRepository        → findTestObject() lee .rs XML
          │          ├─ GlobalVariable          → lee Profiles/default.glbl
          │          ├─ CustomKeywords          → routing dinámico a Steps classes
          │          ├─ WebUiBuiltInKeywords    → callTestCase() recursivo
          │          └─ KeywordUtil             → logInfo/markFailed/markFailedAndStop
          ├─ AppiumDriverManager   — Sesión Appium singleton (UiAutomator2)
          └─ ReportGenerator       — JUnit XML + resumen en consola
```

---

## Comandos

| Comando | Descripción |
|---------|-------------|
| `bash runner/run.sh TC_NombreTest` | Corre un test específico por nombre |
| `bash runner/run.sh list` | Lista todos los test cases disponibles |
| `bash runner/run.sh run --case android/TC_Foo` | Corre por ruta relativa |
| `bash runner/run.sh run --tag smoke` | Corre todos los tests con tag `smoke` |
| `bash runner/run.sh run --all` | Corre todos los tests del proyecto |
| `bash runner/run.sh run --no-appium TC_Foo` | Dry-run sin conectar a Appium |

---

## Salida

```
╔══════════════════════════════════════════════════╗
║       Katalon Headless Runner  v1.0.0            ║
╚══════════════════════════════════════════════════╝
[Runner] Proyecto: /ruta/al/proyecto
[Runner] Config:   runner/config/runner.yml

Ejecutando: TC_MiTest
  [STEP]   Paso 1: Tap Card Restaurante
  [INFO]   ✅ Card encontrado
  [FAILED] Elemento no visible: btn_irACanasta.rs

════════════════════════════════════════════════════
 RESULTADOS
════════════════════════════════════════════════════
 ✓ [PASSED ] TC_Login (8543ms)
 ✗ [FAILED ] TC_Checkout (45210ms)
────────────────────────────────────────────────────
 Total: 2  |  ✓ 1  ✗ 1  ○ 0  |  53.7s
════════════════════════════════════════════════════
```

- `runner/reports/test-results.xml` — JUnit XML (compatible CI/CD)
- `runner/reports/screenshot_YYYYMMDD_HHMMSS.png` — capturas en cada fallo

---

## Bridges: qué simula del API de Katalon

| API Katalon | Implementación en runner |
|-------------|--------------------------|
| `Mobile.tap(obj, timeout)` | `driver.findElement(by).click()` |
| `Mobile.tapAtPosition(x, y)` | W3C PointerInput sequence |
| `Mobile.scrollToText(text)` | `UiScrollable.scrollIntoView` |
| `Mobile.scrollToTop()` | `UiScrollable.flingToBeginning(10)` |
| `Mobile.waitForElementPresent(obj, t)` | `WebDriverWait + presenceOfElement` |
| `Mobile.waitForElementNotPresent(obj, t)` | `WebDriverWait + invisibilityOf` |
| `Mobile.getElementAttribute(obj, attr)` | `element.getAttribute(attr)` |
| `Mobile.takeScreenshot()` | `OutputType.FILE` → `reports/screenshot_*.png` |
| `Mobile.pressBack()` | `AndroidKey.BACK` |
| `Mobile.swipe(x1,y1,x2,y2)` | W3C PointerInput swipe |
| `Mobile.delay(s)` | `Thread.sleep(s*1000)` |
| `findTestObject('path')` | Parsea `path.rs` XML → TestObject |
| `GlobalVariable.G_Foo` | Lee `Profiles/default.glbl` |
| `CustomKeywords.'pkg.Class.method'()` | Reflección sobre Keywords compilados |
| `callTestCase(tc, params)` | Recursión del ScriptExecutor |
| `KeywordUtil.logInfo/markFailed` | `println` + acumulador de fallos |

---

## resolveLocator — prioridad de locators

Cuando `Mobile.tap(obj)` necesita encontrar el elemento en pantalla, el bridge evalúa en este orden:

1. `selectorMethod` dinámico (si se seteó en código) → mapea a By
2. `ANDROID_UI_AUTOMATOR` del .rs → `AppiumBy.androidUIAutomator()`
3. `ACCESSIBILITY` → `AppiumBy.accessibilityId()`
4. `ID` → `By.id()`
5. `ATTRIBUTES` / `XPATH` → `By.xpath()`

---

## Prerequisitos

| Requisito | Versión mínima |
|-----------|---------------|
| Java | 11+ |
| Gradle | 7+ (o Gradle Wrapper) |
| Appium Server | 2.x |
| Appium driver: UiAutomator2 | 2.x |
| Android SDK / adb | cualquier versión reciente |
| Dispositivo Android | conectado vía USB, `adb devices` visible |

---

## Archivos del runner

```
runner/
├── run.sh                    ← Entrada CLI (detecta rebuild automático)
├── build.gradle              ← Gradle: groovy-all + appium java-client + snakeyaml
├── settings.gradle           ← rootProject.name = 'katalon-runner'
├── config/
│   └── runner.yml            ← Configuración del dispositivo y Appium
├── src/main/groovy/
│   ├── runner/               ← Núcleo del runner
│   ├── com/kms/katalon/core/ ← Bridges de API Katalon
│   ├── internal/             ← GlobalVariable bridge
│   └── CustomKeywords.groovy ← Routing dinámico de keywords
└── reports/                  ← JUnit XML + screenshots (generados)
```

---

## Rebuild automático

El script `run.sh` detecta si algún `.groovy` fue modificado después del último build y recompila automáticamente antes de ejecutar. No es necesario compilar manualmente.

```bash
# Forzar rebuild manual si es necesario:
cd runner/
gradle shadowJar
```
