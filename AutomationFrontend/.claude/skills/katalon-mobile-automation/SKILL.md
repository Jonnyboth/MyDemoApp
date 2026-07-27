---
name: katalon-mobile-automation
description: >
  WORKFLOW SKILL — Create, structure, and maintain mobile test automation projects using Katalon Studio (free license) with 3-Layer Page Object Model (POM) methodology for Android and iOS apps. USE FOR: creating new test cases (smoke, E2E, functional); creating Object Repository .rs files with correct MobileElementEntity XML format (android/ and ios/ subfolders); writing Groovy Page classes (com.tuempresa.page.*), Steps classes with @Keyword (com.tuempresa.steps.*), and orchestrator Scripts; designing cross-platform keywords using GlobalVariable.G_Platform; capturing UI elements from real Android/iOS devices using mobile-mcp; running and validating test flows on a real device via mobile-mcp BEFORE writing automation code; debugging locator errors like "Name is null" or "MobileLocatorStrategy"; structuring test suites; analyzing video evidence of test runs by extracting frames and reviewing them visually. ALSO USE FOR: any request mentioning Katalon, .rs files, Object Repository, MobileElementEntity, Mobile.tap, Mobile.scrollToText, Groovy test scripts, Appium-based mobile testing, POM page/steps pattern, GlobalVariable, or "corre el flujo", "valida el flujo", "prueba el flujo" with MCP, or "te comparto el video", "mira el video", "tengo la evidencia en video", "analiza el video", "video del test", "grabación del test", "evidencia en mp4", "evidencia en video". DO NOT USE FOR: web automation (WebUI.*), API testing, or non-Katalon frameworks.
---

# Katalon Mobile Automation — POM 3-Layer Methodology

This skill creates and maintains mobile test cases in Katalon Studio (free license) using a **3-Layer Page Object Model (POM)** for Android and iOS apps.

## Table of Contents
1. [Core Concepts](#core-concepts)
2. [Project Architecture — 3 Layers](#project-architecture)
3. [Cross-Platform with GlobalVariable](#cross-platform)
4. [Flow Validation with mobile-mcp](#flow-validation-with-mobile-mcp)
5. [Video Evidence Analysis](#video-evidence-analysis)
6. [Test Planning, Analysis, and Exploration Workflow](#test-planning-analysis-and-exploration-workflow)
7. [Creating a Test Case — Step by Step](#creating-a-test-case)
8. [Object Repository — .rs File Format](#object-repository-rs-format)
9. [Layer 1 — Page Classes](#layer-1-page-classes)
10. [Layer 2 — Steps Classes (@Keyword)](#layer-2-steps-classes)
11. [Layer 3 — Script (Orchestrator)](#layer-3-script)
12. [Common Errors & Fixes](#common-errors)

---

## Core Concepts

- **Language**: Always communicate with the user in Spanish when running MCP validations, reporting errors, asking for clarification, or requesting information. Technical content (code, XML, logs, error messages) remains in English.
- **Katalon free license**: All features used must work without Enterprise license.
- **3-Layer POM**: UI elements live in Object Repository → interaction logic lives in Page classes → keywords exposed via Steps classes → tests orchestrate Steps only.
- **Platform**: Android (real device + emulator) and iOS (real device + simulator). Objects are `MobileElementEntity`.
- **Appium underneath**: Katalon mobile uses Appium — locator strategies: ATTRIBUTES, ACCESSIBILITY, CLASS_NAME, ANDROID_UI_AUTOMATOR, XPATH, ID.
- **Cross-platform**: One keyword codebase for both platforms via `GlobalVariable.G_Platform`.
- **Device-agnostic by default**: Automated tests must be executable on any compatible mobile device/resolution, not tied to one model.
- **Coordinate interaction policy**: Coordinates are a valid interaction strategy alongside locators. When elements are not accessible via UIAutomator (e.g., Jetpack Compose UI), use `tapAtPosition()` with coordinates scaled to the active device via `DeviceResolutionPage`. Reference resolution is SM-S928B (1080×2340). Never hardcode raw coordinates — always scale through `DeviceResolutionPage.scaleX/scaleY`.
- **Device resolution caching**: `DeviceResolutionPage` (in `page/common/`) detects the active device via `GlobalVariable.G_DevicesName`, caches its resolution, and recalculates automatically when a different device is detected. Use it in every Page class that performs coordinate-based interactions.

---

## Utility Classes Library

All utility classes live at `Keywords/tuempresa/utils/` and are available as `CustomKeywords` in any test or keyword file.

### 1. SmartWaitPage — Standardized Waits
**Package:** `tuempresa.utils.SmartWaitPage`
**Purpose:** Replace all `Mobile.delay()` calls with intent-revealing wait methods.

| Method | Description | Use when |
|---|---|---|
| `waitVisible(obj, timeout)` | Wait for element + fail if timeout | Before any critical assertion |
| `waitVisibleOptional(obj, timeout)` | Wait for element + return boolean | Optional elements, branch conditions |
| `waitGone(spinnerObj, timeout)` | Wait until spinner disappears | After triggering network operation |
| `floorPause()` | Fixed 1s pause | Post-tap animation buffer only |
| `tapPause()` | Fixed 0.35s pause | Between rapid counter taps only |

**Constants:** `SHORT=5s`, `MEDIUM=15s`, `LONG=30s`, `FLOOR=1s`

```groovy
import tuempresa.utils.SmartWaitPage
SmartWaitPage.waitVisible(findTestObject('Object Repository/android/Home/lbl_inicioTab'), SmartWaitPage.MEDIUM)
```

---

### 2. LocatorHelper — Self-Healing Locators
**Package:** `tuempresa.utils.LocatorHelper`
**Purpose:** Runtime fallback chain when primary locator strategy fails.

**Priority order:** ACCESSIBILITY → ANDROID_UI_AUTOMATOR → ATTRIBUTES

```groovy
import tuempresa.utils.LocatorHelper

TestObject el = LocatorHelper.findWithFallback(
    'content-desc-value',                     // ACCESSIBILITY (content-desc)
    'new UiSelector().resourceId("...")',      // ANDROID_UI_AUTOMATOR
    '//*[@content-desc="content-desc-value"]' // ATTRIBUTES (XPath)
)
```

**Log output:** `✅ LocatorHelper resolved via ACCESSIBILITY: 'add'`

**When to use:** Elements in the critical purchase path (add-to-cart, checkout, payment, order tracking).

---

### 3. ScreenshotPage — Visual Regression Testing
**Package:** `tuempresa.utils.ScreenshotPage`
**Purpose:** Pixel-diff comparison against stored baselines. Detects UI layout regressions.

**How it works:**
1. First call → auto-saves baseline image (logs "Baseline saved")
2. Subsequent calls → compares current screen, fails if diff > threshold (default 2%)
3. After intentional UI change → call `updateBaseline()` and commit new baseline

```groovy
import tuempresa.utils.ScreenshotPage
ScreenshotPage.captureAndCompare('home_screen')          // compare or create baseline
ScreenshotPage.captureAndCompare('home_screen', 0.05)    // custom 5% threshold
ScreenshotPage.updateBaseline('home_screen')             // refresh after intentional change
```

**Baseline files:** `Include/resources/baseline-screenshots/`
**Device:** SM-S928B (1080×2340) — baselines are device-specific.

---

### 4. VisualLocatorPage — AI Visual Element Detection
**Package:** `tuempresa.utils.VisualLocatorPage`
**Purpose:** Find UI elements by visual appearance when XML-based locators are unavailable.
**Plugin:** `test-ai-classifier` v4.0.2 (installed, CPU backend, ARM64 macOS compatible)

```groovy
import tuempresa.utils.VisualLocatorPage

TestObject el = VisualLocatorPage.findByVisual('add_to_cart_button', 10)
if (el != null) Mobile.tap(el, 5)

// Or tap directly:
boolean tapped = VisualLocatorPage.tapByVisual('checkout_button', 10)
```

**Available labels (training images already provided):**
- `shopping_cart_icon` — Cart icon/bar
- `checkout_button` — "Continuar" / "Ir a canasta" buttons
- `add_to_cart_button` — Green "+" button

**Adding new labels:**
1. Create `Include/resources/classifier-labels/<label>/`
2. Add 3–5 PNG crops of the element: `sample_01.png`, `sample_02.png`, ...
3. Use `VisualLocatorPage.findByVisual('<label>')` in tests

**When to use:** Promotional banners, dynamic elements, elements with no stable XML attributes. **Always last resort** after XML strategies.

---

### Capture Strategy Decision Matrix

| Scenario | Primary Tool | Fallback |
|---|---|---|
| Capturing .rs for new screen | `adb uiautomator dump` | `mobile_list_elements_on_screen` |
| **Validating element is interactive** | **Empirical tap via `mobile_click_on_screen_at_coordinates` + dump diff** | Screenshot manual comparison |
| Verifying correct screen state | `ScreenshotPage.captureAndCompare()` | Screenshot + manual check |
| Element fails at runtime | `LocatorHelper.findWithFallback()` | `VisualLocatorPage.findByVisual()` |
| Dynamic/promotional element | `VisualLocatorPage.findByVisual()` | Manual coordinate tap |
| Timing/flakiness issue | `SmartWaitPage.waitVisible()` | Increase timeout constant |

### Empirical Locator Validation Protocol

This protocol is executed by **BMO-Explorer** in CAPTURE MODE before writing any `.rs` for interactive elements. It prevents generating locators for decorative or non-interactive nodes.

**When to apply:** Every element with `clickable="true"` in the UIAutomator dump.
**Skip for:** `clickable="false"` elements → mark as `tap_validated: false` automatically.

**Protocol steps:**
1. **Pre-tap dump**: `adb shell uiautomator dump` → save as `pre_<element>.xml`
2. **Calculate center from bounds**: `bounds="[x1,y1][x2,y2]"` → `centerX=(x1+x2)/2`, `centerY=(y1+y2)/2`
3. **Execute tap**: `mobile_click_on_screen_at_coordinates(device, centerX, centerY)` → wait 1.5s
4. **Post-tap dump**: `adb shell uiautomator dump` → save as `post_<element>.xml`
5. **Evaluate**: compare root XML nodes:
   - Different root nodes or activity → `tap_validated: true` ✅
   - Identical XML → `tap_validated: false` ❌
6. **Return to origin**: `mobile_press_button(device, "back")`

**Convert bounds to base 1080×2340:**
```
base_x = round(centerX * 1080 / device_width)
base_y = round(centerY * 2340 / device_height)
```
These base values become constants in the Page class, scaled at runtime via `DeviceResolutionPage.scaleX/scaleY`.

**Output — enriched component record (add to context file):**

| .rs | resource-id | content-desc | bounds (real) | base_x | base_y | tap_validated | primary_strategy | fallback |
|---|---|---|---|---|---|---|---|---|
| btn_foo | com.tuempresa.app:id/foo | Foo | [100,200][300,400] | 200 | 913 | ✅ true | ACCESSIBILITY | ANDROID_UI_AUTOMATOR |

**How BMO-TestCreator consumes `tap_validated`:**
- `true` → populate ACCESSIBILITY + ANDROID_UI_AUTOMATOR + ATTRIBUTES; use `LocatorHelper.findWithFallback()` for critical path elements
- `false` → populate ATTRIBUTES only; no `findWithFallback()`
- `COMPOSE` → use `tapAtPosition` with `DeviceResolutionPage.scaleX/scaleY(base_x, base_y)`

**Path mirroring rule (canonical):** Script path always mirrors the Test Case path:
- `Test Cases/android/X/Y.tc` → `Scripts/android/X/Y/Script<timestamp>.groovy`
- The `run-id` from the orchestrator is **never** used as a script folder name.

---

## Project Architecture

```
TestMobile/
├── Keywords/
│   └── com/tuempresa/
│       ├── page/                        ← CAPA 1: Interacción UI pura
│       │   ├── common/                  ← Reutilizable en Android e iOS
│       │   │   ├── HomePage.groovy      ← Acciones del Home (cross-platform)
│       │   │   └── UtilsPage.groovy     ← scroll, validateElements
│       │   ├── android/                 ← Específico de Android
│       │   │   └── <Pantalla>Page.groovy
│       │   └── ios/                     ← Específico de iOS
│       │       └── <Pantalla>Page.groovy
│       └── steps/                       ← CAPA 2: @Keyword públicos
│           ├── common/
│           │   └── HomeSteps.groovy
│           ├── android/
│           │   └── <Pantalla>Steps.groovy
│           └── ios/
│               └── <Pantalla>Steps.groovy
│
├── Object Repository/
│   ├── android/                         ← Objetos Android (MobileElementEntity)
│   │   ├── Home/
│   │   │   ├── btn_account.rs
│   │   │   ├── btn_help.rs
│   │   │   ├── btn_selectRestVertical.rs
│   │   │   └── lbl_topBarTitleRescueScreen.rs
│   │   └── <Pantalla>/
│   │       └── <elemento>.rs
│   └── ios/                             ← Objetos iOS (MobileElementEntity)
│       ├── Home/
│       │   ├── btn_account.rs
│       │   ├── btn_help.rs
│       │   ├── btn_selectRestVertical.rs
│       │   ├── lbl_topBarTitleRescueScreen.rs
│       │   └── btn_closeRescueScreen.rs
│       └── <Pantalla>/
│           └── <elemento>.rs
│
├── Scripts/                             ← CAPA 3: Orquestador (solo CustomKeywords)
│   ├── android/
│   │   └── <TICKET-ID>/
│   │       └── Script<timestamp>.groovy
│   ├── ios/
│   │   └── <TICKET-ID>/
│   │       └── Script<timestamp>.groovy
│   └── common/
│
├── Test Cases/
│   ├── android/
│   │   └── <TICKET-ID>.tc
│   ├── ios/
│   └── common/
│
├── Test Suites/
│   └── smoke/
│       ├── android/
│       └── ios/
│
└── Profiles/
    └── default.glbl                     ← GlobalVariables (G_Platform, G_AppBundleID…)
```

### ⚠️ Convención de nomenclatura de Test Case (OBLIGATORIA — no negociable)

Todo `<TICKET-ID>` usado arriba (nombre del `.tc`, carpeta bajo `Scripts/`, `<name>` en el
XML del Test Case) debe seguir este patrón exacto:

```
<KEY_PROYECTO>-TC-<ID>-<validacionOCasoDePrueba>
```

| Parte | Significado | Ejemplo |
|---|---|---|
| `KEY_PROYECTO` | Clave del proyecto en el sistema de tickets (Jira u otro), en mayúsculas | `SIM` |
| `TC-<ID>` | Identificador del test case tal como aparece en el ticket/historia | `TC-4` |
| `<validacionOCasoDePrueba>` | Qué valida el caso, en camelCase, sin espacios ni acentos, descriptivo y específico | `loginExitoso` |

**Ejemplo completo:** `SIM-TC-4-loginExitoso`

Este mismo nombre completo se usa de forma consistente en los tres lugares que deben
espejarse: `Test Cases/<plataforma>/<subfolder>/SIM-TC-4-loginExitoso.tc`,
`Scripts/<plataforma>/<subfolder>/SIM-TC-4-loginExitoso/Script<timestamp>.groovy`, y
`<name>SIM-TC-4-loginExitoso</name>` dentro del `.tc`.

❌ **Incorrecto:** usar solo el ticket (`SIM-TC-4` sin sufijo de validación), nombres
genéricos (`Test1`, `LoginTest`), o inconsistencias entre el nombre de carpeta y el
`<name>` del `.tc`.
✅ **Correcto:** `SIM-TC-4-loginExitoso`, `SIM-TC-12-loginCredencialesInvalidas`,
`TBS-TC-7-agregarProductoAlCarrito`.

Si el ticket real no tiene ID numérico todavía (caso exploratorio sin ticket creado),
preguntar al usuario por la clave de proyecto e ID antes de nombrar los archivos — nunca
inventar un ID.

### Layer responsibilities — strict rules

| Capa | Puede hacer | NO puede hacer |
|------|-------------|----------------|
| **Page** | `Mobile.*`, `findTestObject()`, `DeviceResolutionPage.scaleX/Y/Point()`, lógica UI | `@Keyword`, llamar Steps |
| **Steps** | `@Keyword`, instanciar Page, manejar errores con `KeywordUtil` | `Mobile.*` directamente, `findTestObject()` |
| **Script** | `Mobile.startExistingApplication()`, `Mobile.comment()`, `Mobile.closeApplication()`, `CustomKeywords.'..Steps..'()` | `Mobile.tap()`, `findTestObject()` directo |

---

## Cross-Platform with GlobalVariable

`GlobalVariable.G_Platform` is set to `"android"` or `"ios"` in `Profiles/default.glbl`. Page classes in `page/common/` use it to build dynamic object paths:

```groovy
// Works for BOTH platforms — path changes based on the profile
String platform = GlobalVariable.G_Platform   // "android" or "ios"
def btn = findTestObject("Object Repository/${platform}/Home/btn_account")
// Android → Object Repository/android/Home/btn_account.rs  (XPATH with android.widget.)
// iOS     → Object Repository/ios/Home/btn_account.rs      (XPATH with XCUIElementType)
```

**Available GlobalVariables** (configured in `Profiles/default.glbl`):

| Variable | Description | Default |
|----------|-------------|---------|
| `G_Platform` | `'android'` or `'ios'` | `'android'` |
| `G_AppBundleID` | App package/bundle ID | `'com.tuempresa.app'` |
| `G_AutomationName` | Appium automation name | `'UiAutomator2'` |
| `G_User` | Test user identifier | `''` |
| `G_DevicesName` | Device name as in adb/instruments | `''` |

---

## Flow Validation with mobile-mcp

When the user asks to **run or validate a test flow** (phrases like "corre el flujo", "valida el flujo con el MCP", "prueba el flujo"), execute the entire flow on the real device using mobile-mcp **before** writing any automation code.

### When to apply
- User says "corre el flujo" / "valida primero" / "prueba el flujo"
- A new test is requested and the flow hasn't been validated in the current session
- An existing test is failing and the user wants to re-validate

### Full Validation Workflow

**Phase 1 — Confirm device and app state**
```
1. mobile_list_available_devices()
   → Confirm device is connected. Note the deviceId.

2. mobile_take_screenshot(device="<deviceId>")
   → See current screen state.

3. If app is not open:
   mobile_launch_app(device="<deviceId>", packageName="com.tuempresa.app")
   mobile_take_screenshot(device="<deviceId>")
```

**Phase 2 — Execute each flow step and capture elements**

For EVERY step:
```
a. mobile_take_screenshot()             → document current state
b. UIAutomator dump (primary — 1-2s vs 5-15s of mobile_list_elements):
     adb -s <deviceId> shell uiautomator dump /sdcard/step<N>.xml
     adb -s <deviceId> pull /sdcard/step<N>.xml /tmp/step<N>.xml
     cat /tmp/step<N>.xml
   → Fallback only if adb unavailable: mobile_list_elements_on_screen()
c. Try locator-driven interaction first (id/accessibility/text/xpath);
   use coordinate tap only if there is no stable exposed element
d. mobile_take_screenshot()             → confirm navigation succeeded
```

If scrolling is needed:
```
mobile_swipe_on_screen(device, startX, startY, endX, endY)
→ Scroll down: startY=1500, endY=700
→ Scroll right: startX=800, endX=400
```

**Phase 3 — Report findings before automating**

After completing the flow, report:
```
✅ Flujo validado exitosamente en <Device>

Elementos capturados:
| Paso | .rs sugerido       | class                        | text / resource-id       | coords    |
|------|--------------------|------------------------------|--------------------------|-----------|
| 1    | btn_account        | android.widget.ImageView     | resource-id: account_tab | (540,950) |
| 2    | btn_help           | android.widget.TextView      | text: Ayuda              | (200,400) |

Notas:
- Paso 3 requirió scroll — usar waitForElementPresent + scrollToElement en Page
- Modal "Pídelos de nuevo" apareció — manejar con try/catch en skipScreensHome()
```

**Phase 4 — Proceed to automation** (only after Phase 3)

1. Create `.rs` files with exact element data
2. Create or update Page class
3. Create or update Steps class with `@Keyword`
4. Create Script calling only `CustomKeywords.'...'()`
5. Create `.tc` Test Case descriptor

### mobile-mcp Tool Reference

| Action | Tool |
|--------|------|
| List connected devices | `mobile_list_available_devices()` |
| Take screenshot | `mobile_take_screenshot(device)` |
| **List elements (FAST — PRIMARY)** | `adb -s <id> shell uiautomator dump /sdcard/d.xml && adb pull /sdcard/d.xml /tmp/d.xml && cat /tmp/d.xml` |
| List elements (FALLBACK — slow) | `mobile_list_elements_on_screen(device)` |
| Launch app by package | `mobile_launch_app(device, packageName)` |
| Emergency tap by coordinates (last resort) | `mobile_click_on_screen_at_coordinates(device, x, y)` |
| Swipe (scroll) | `mobile_swipe_on_screen(device, startX, startY, endX, endY)` |
| Type text | `mobile_type_keys(device, text)` |
| Press back | `mobile_press_button(device, button="back")` |

### Coordinate interaction policy

Coordinates are a valid interaction strategy when UIAutomator does not expose an accessible element (e.g., Jetpack Compose UI). Follow this protocol:

1. **Always scale** — never use raw pixel values. All coordinates are expressed in the reference resolution (1080×2340, SM-S928B) and scaled at runtime via `DeviceResolutionPage.scaleX/scaleY`.
2. **Capture bounds from dump** — extract `bounds` from UIAutomator dump to calculate center X/Y in the reference resolution, then store those as constants in the Page class.
3. **Document the element** — add a comment explaining which UI element the coordinates target and why locator-based interaction is not available.
4. **Validate on device** — confirm tap works before adding to the Page class. Screenshot evidence recommended.

When coordinates are used, report:
- Screen/context and which element is targeted
- Bounds from UIAutomator dump (or MCP screenshot measurement)
- Why locator strategy is not available (e.g., Compose, clickable=false)
- Constants defined in the Page class + DeviceResolutionPage scaling call used

### Element data mapping (mobile-mcp → .rs file)

| mobile_list_elements_on_screen field | Maps to .rs property |
|--------------------------------------|----------------------|
| `type` | `class` |
| `text` | `text` |
| `label` | `content-desc` / `name` (iOS) |
| `identifier` | `resource-id` |

---

## Video Evidence Analysis

When the user shares a video file (MP4, MOV, AVI, etc.) as evidence of a test run, use the `extract_video_frames.py` script to convert it into analyzable frames, then review each frame visually.

### When to apply
Activate this workflow when the user says any of these (or similar):
- "te comparto el video" / "tengo el video del test" / "mira el video"
- "evidencia en video" / "evidencia en mp4" / "grabación del test"
- "analiza este video" / "revisa el video" / "el test falló, aquí el video"
- Shares a `.mp4`, `.mov`, `.avi`, `.mkv`, or `.webm` file path

### Step-by-Step Workflow

**Step 1 — Extract frames from the video**

Run the extraction script with the video path the user provided:

```bash
python3 ".claude/skills/katalon-mobile-automation/extract_video_frames.py" \
  "<ruta_del_video_que_compartió_el_usuario>" \
  --fps 1 \
  --output /tmp/video_frames_evidence
```

Ruta relativa a la raíz del proyecto (`AutomationFrontend/`). Ajustar solo si se invoca
desde otro directorio de trabajo.

Adjust `--fps` based on context:
| Situación | `--fps` recomendado |
|-----------|---------------------|
| Video corto (< 30s) | `2` — más frames, más detalle |
| Video normal (30s–2min) | `1` — 1 frame/segundo (default) |
| Video largo (> 2min) | `0.5` — 1 frame cada 2 segundos |
| Solo quieres los momentos clave | `--keyframes` en vez de `--fps` |
| Máximo N frames uniformes | `--max 20` |

**Step 2 — Confirm extraction success**

The script prints the list of extracted frame paths. Confirm the number of frames extracted and the output folder. If 0 frames were extracted, check:
- The file path is correct and the file exists
- ffmpeg is installed and on PATH (`ffmpeg -version`)
- The video is not corrupted

**Step 3 — Analyze frames visually**

Use the `view_image` tool to review key frames. Load frames in chronological order to understand the test flow:

```
view_image("/tmp/video_frames_evidence/frame_0001.png")  ← estado inicial
view_image("/tmp/video_frames_evidence/frame_0005.png")  ← paso intermedio
view_image("/tmp/video_frames_evidence/frame_0012.png")  ← punto de fallo
```

For short videos, review all frames. For long videos, focus on:
1. First 3 frames — app initial state
2. Frames around visible errors (UI freezes, wrong screens)
3. Last 3 frames — final state (passed/failed)

**Step 4 — Report findings**

After analysis, report:
```
📹 Análisis de evidencia en video

Duración analizada: Xs (N frames)

Observaciones:
- Frame 0003: La app muestra el QA Launcher correctamente ✅
- Frame 0007: El botón 'Continúa con Google' aparece pero no responde ❌
- Frame 0010: Pantalla se congela en la cuenta de Google — posible timeout

Causa probable: [diagnóstico]
Acción sugerida: [fix o siguiente paso]
```

**Step 5 — Cross-reference with code if needed**

If the video reveals a failing step, trace back to the corresponding Page or Steps class and check the locator / timeout values.

### Script location and options

```
.claude/skills/katalon-mobile-automation/extract_video_frames.py
```

Full options reference:
```
usage: extract_video_frames.py <video> [--fps N] [--output DIR] [--keyframes] [--max N] [--quality 1-9]

  --fps N        Frames por segundo (default: 1.0)
  --output DIR   Carpeta de salida (default: /tmp/video_frames_<nombre>)
  --keyframes    Solo keyframes del video (ignora --fps)
  --max N        Máximo N frames, distribuidos uniformemente
  --quality 1-9  Compresión PNG (1=mínima compresión, 9=máxima — default: 3)
```

### Quick command (copy-paste ready)

```bash
python3 ".claude/skills/katalon-mobile-automation/extract_video_frames.py" "RUTA_VIDEO" --fps 1
```

Replace `RUTA_VIDEO` with the path the user provides.

---

## Test Planning, Analysis, and Exploration Workflow

Use this section BEFORE implementing a new test case. It converts a user request into a stable automation plan and reduces flaky locators.

### When to apply
- User asks to create a new test (E2E, smoke, functional)
- User gives a business flow but not exact technical selectors
- The app has dynamic content (carousels, optional popups, personalized cards)

### Phase 1 — Requirement decomposition (what to automate)

Convert the request into explicit checkpoints:
1. Entry point (e.g., open app + confirm Home loaded)
2. Interruptions (e.g., Learning/Promoción/location popups)
3. Dynamic navigation (vertical/horizontal scroll containers)
4. Target interaction (e.g., select store in carousel)
5. End-state validation (e.g., store header loaded)

Output format (recommended):
```
Flujo objetivo:
- Paso 1: Home listo
- Paso 2: cerrar popup opcional si aparece
- Paso 3: entrar a Súper
- Paso 4: encontrar Geant en carrusel
- Paso 5: validar header de Geant

Fuera de alcance:
- No abrir menú de más opciones (...)
```

### Phase 2 — Live exploration with MCP (how to locate)

Run exploration on real device before coding:
1. Capture screenshot of each transition
2. Dump `mobile_list_elements_on_screen()` on each key screen
3. Build candidate selectors per step:
   - Priority 1: `resource-id`
   - Priority 2: `content-desc` / accessibility label
   - Priority 3: `text + class`
4. Mark unstable controls (no id/desc) as fallback-only

Recommended evidence per step:
```
- Screen name
- Candidate object name (Katalon naming convention)
- class / text / identifier / label
- Stability note (stable, medium, fallback)
```

### Phase 3 — Selector decision matrix (stability first)

For each object, store one primary selector and one fallback strategy.

Example matrix:
| Object | Primary | Fallback | Stability | Note |
|--------|---------|----------|-----------|------|
| btn_SuperCard | text='Súper' + class | parent card by id + child text | Medium | card id repeats |
| item_Geant | resource-id `textView_store_name` + text `Geant` | horizontal swipe + text | High | resilient in carousel |
| btn_ClosePopup | accessibility `close` | coordinate tap (optional only) | Low | popup not always exposed |

### Phase 4 — Automation plan (what files to create)

Before writing code, publish a concrete plan in this order:
1. Object Repository list (`.rs` files by screen)
2. Page methods (UI logic only)
3. Steps methods (`@Keyword` wrappers)
4. Script orchestration (only `CustomKeywords` + allowed `Mobile.*`)
5. `.tc` descriptor

Template:
```
Plan:
1) OR: android/Home/*, android/Super/*, android/Geant/*
2) Page: dismissOptionalPopupIfPresent, goToSuperFromHome, selectGeant...
3) Steps: prepareHome..., tapSuperCard, selectGeantStore...
4) Script: OpenStoreGeant flow
5) TestCase: OpenStoreGeant.tc
```

### Phase 5 — Scope control (avoid over-automation)

If user excludes a step (example: "no ejecutar Más opciones"), remove it from:
- Page methods
- Steps keywords
- Script sequence
- Acceptance criteria

Never keep excluded actions as active steps in the first version.

### Phase 6 — Definition of Done for new test creation

A test creation task is done only when ALL are true:
1. Flow was validated live with MCP (or video evidence analyzed)
2. `.rs` objects include valid `locatorStrategy` and `platform`
3. POM layering is respected (Page/Steps/Script boundaries)
4. Script is aligned to approved scope
5. Test case `.tc` exists and matches script intent

### Practical lessons learned (from real TuEmpresa flow)

- Cards may share the same identifier (e.g., repeated card ids in Home); disambiguate with visible text.
- Carousel targets are usually more stable via item name id + text than by raw coordinates.
- Popups can be intermittent and partially inaccessible; always handle with OPTIONAL strategy.
- Header validation should use a deterministic label present only after target screen load.

### Mandatory response format (for every new test request)

When planning a new test, ALWAYS answer using this exact structure before coding.

#### 1) Scope summary
```
Objetivo del test:
- <what the test validates>

En alcance:
- <step 1>
- <step 2>

Fuera de alcance:
- <explicit exclusions requested by user>
```

#### 2) Exploration evidence checklist
```
Checklist de exploración MCP:
- [ ] Device detectado y app abierta
- [ ] Screenshot por pantalla clave
- [ ] Dump de elementos por pantalla clave
- [ ] Interrupciones detectadas (popup/modal) y estrategia OPTIONAL definida
- [ ] Contenedor de scroll vertical identificado
- [ ] Contenedor/carrusel horizontal identificado (si aplica)
- [ ] Elemento target validado (tap/navegación)
- [ ] Evidencia de pantalla final esperada
```

#### 3) Technical object map (required table)
Use one row per object to be created in Object Repository:

| Screen | Suggested .rs name | class | resource-id | text | content-desc/label | Primary locator | Fallback locator | Stability |
|--------|---------------------|-------|-------------|------|--------------------|-----------------|------------------|-----------|
| Home | btn_SuperCard | android.widget.TextView |  | Súper |  | text+class | parent+child text | Medium |

Stability scale:
- High: resource-id or accessibility-id unique and deterministic
- Medium: text + class or contextual xpath
- Coordinates: valid strategy when UIAutomator does not expose the element (Jetpack Compose, clickable=false); must use `DeviceResolutionPage.scaleX/scaleY` with base 1080×2340

#### 4) File creation plan (required)
```
Archivos a crear/actualizar:
1. Object Repository:
   - android/<Screen>/<object>.rs
2. Page layer:
   - Keywords/com/tuempresa/page/android/<Screen>Page.groovy
3. Steps layer:
   - Keywords/com/tuempresa/steps/android/<Screen>Steps.groovy
4. Script layer:
   - Scripts/android/<TEST-ID>/Script<timestamp>.groovy
5. Test Case descriptor:
   - Test Cases/android/<TEST-ID>.tc
```

#### 5) Acceptance criteria (required)
```
Criterios de aceptación:
- [ ] El flujo llega al estado final esperado
- [ ] No usa pasos fuera de alcance
- [ ] Selectores críticos con prioridad resource-id/accessibility
- [ ] Si usa `tapAtPosition`: coordenadas en base 1080×2340 escaladas con `DeviceResolutionPage.scaleX/scaleY`
- [ ] Popups opcionales manejados sin flaky failures
- [ ] Respeta reglas Page/Steps/Script
- [ ] Flujo validado en al menos 2 perfiles de dispositivo/resolución
```

#### 6) Ready-to-implement decision
```
Estado: READY / NOT READY
Bloqueos:
- <if any>
Siguiente acción:
- <implement now / gather missing evidence>
```

If any required section is missing, do not proceed to implementation.

---

## Creating a Test Case

### Step 0 — Validate the flow live on device (if not already done)
Execute the [Flow Validation with mobile-mcp](#flow-validation-with-mobile-mcp) workflow. Only proceed after element data is captured.

### Step 1 — Create Object Repository files
One `.rs` file per UI element. Organize by platform → screen:
- `Object Repository/android/<Screen>/<element>.rs`
- `Object Repository/ios/<Screen>/<element>.rs`

See [Object Repository .rs Format](#object-repository-rs-format).

### Step 2 — Create the Page class

**Android-specific** → `Keywords/com/tuempresa/page/android/<Screen>Page.groovy`
**iOS-specific** → `Keywords/com/tuempresa/page/ios/<Screen>Page.groovy`
**Cross-platform** → `Keywords/com/tuempresa/page/common/<Screen>Page.groovy`

### Step 3 — Create the Steps class (@Keyword)

**Same platform split** → `Keywords/com/tuempresa/steps/android/<Screen>Steps.groovy`

All methods must have `@Keyword`. Only delegate to Page — no `Mobile.*` calls here.

### Step 4 — Create the Script
Folder: **debe espejarse desde el TC** → `Scripts/android/<subfolder-del-TC>/<TC-Name>/Script<timestamp>.groovy`

⚠️ **Path mirroring obligatorio:** Si el TC está en `Test Cases/android/TurboStore/TBS-TC-7-agregarProductoAlCarrito.tc`, el script DEBE estar en `Scripts/android/TurboStore/TBS-TC-7-agregarProductoAlCarrito/Script<timestamp>.groovy`. Usar el run-id del pipeline como nombre de carpeta es un error — siempre usar el nombre del TC (que debe cumplir la [convención de nomenclatura obligatoria](#project-architecture)).

The script ONLY calls `CustomKeywords`, `Mobile.startExistingApplication()`, `Mobile.comment()`, `Mobile.closeApplication()`.

### Step 5 — Create the Test Case .tc file

⚠️ El `<name>` DEBE seguir la [convención de nomenclatura obligatoria](#project-architecture)
`<KEY_PROYECTO>-TC-<ID>-<validacionOCasoDePrueba>` — ver ejemplo `SIM-TC-4-loginExitoso`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>Brief description of what this test validates</description>
   <name>SIM-TC-4-loginExitoso</name>
   <tag></tag>
   <comment></comment>
   <recordOption>OTHER</recordOption>
   <testCaseGuid>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</testCaseGuid>
</TestCaseEntity>
```

### Step 6 — Refresh in Katalon Studio
Press `F5` to see the new files.

---

## Object Repository — .rs Format

**Critical**: Always use `<MobileElementEntity>`, NEVER `<WebElementEntity>`. Missing `<locatorStrategy>` causes: `Name is null at MobileLocatorStrategy.valueOf`.

### Mandatory fields checklist
- `<MobileElementEntity>` root tag ✅
- `<elementGuidId>` (any UUID) ✅
- `<selectorMethod>BASIC</selectorMethod>` ✅
- `<smartLocatorEnabled>false</smartLocatorEnabled>` ✅
- `<locator>` (primary XPath) ✅
- `<locatorCollection>` with all strategy entries ✅
- **`<locatorStrategy>ATTRIBUTES</locatorStrategy>`** ✅ ← most common crash cause
- `<platform>ANDROID</platform>` or `<platform>IOS</platform>` ✅

### Android .rs template (resource-id)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Description of element purpose</description>
   <name>btn_exampleName</name>
   <tag></tag>
   <elementGuidId>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.view.View</value>
      <webElementGuid>uuid-1</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>the_resource_id_from_device</value>
      <webElementGuid>uuid-2</webElementGuid>
   </webElementProperties>
   <locator>//android.view.View[@resource-id="the_resource_id_from_device"]</locator>
   <locatorCollection>
      <entry><key>ID</key><value>the_resource_id_from_device</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>//android.view.View[@resource-id="the_resource_id_from_device"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value></value></entry>
      <entry><key>ATTRIBUTES</key><value>//android.view.View[@resource-id="the_resource_id_from_device"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value></value></entry>
      <entry><key>CLASS_NAME</key><value>android.view.View</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
```

### iOS .rs template (name / accessibility)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Description of element purpose</description>
   <name>btn_exampleName</name>
   <tag></tag>
   <elementGuidId>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>XCUIElementTypeButton</value>
      <webElementGuid>uuid-1</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>name</name>
      <type>Main</type>
      <value>element_accessibility_id</value>
      <webElementGuid>uuid-2</webElementGuid>
   </webElementProperties>
   <locator>//XCUIElementTypeButton[@name="element_accessibility_id"]</locator>
   <locatorCollection>
      <entry><key>ID</key><value></value></entry>
      <entry><key>NAME</key><value>element_accessibility_id</value></entry>
      <entry><key>XPATH</key><value>//XCUIElementTypeButton[@name="element_accessibility_id"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value>element_accessibility_id</value></entry>
      <entry><key>ATTRIBUTES</key><value>//XCUIElementTypeButton[@name="element_accessibility_id"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value>name == "element_accessibility_id"</value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value></value></entry>
      <entry><key>CLASS_NAME</key><value>XCUIElementTypeButton</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value>**/XCUIElementTypeButton[`name == "element_accessibility_id"`]</value></entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>IOS</platform>
</MobileElementEntity>
```

### Locator strategy priority (best → fragile)
1. `resource-id` (Android) / `name`/`accessibility-id` (iOS) — most stable
2. `text` + `class` — reliable for labels and buttons with visible text
3. `content-desc` — good for icon-only buttons
4. `XPATH` absolute path — fragile, last resort

### Naming conventions for .rs files

| Prefix | Type | Example |
|--------|------|---------|
| `btn_` | Button / Tappable | `btn_continue.rs` |
| `lbl_` | Label / Static text | `lbl_orderTotal.rs` |
| `input_` | Text input field | `input_cardNumber.rs` |
| `chk_` | Checkbox / Toggle | `chk_agreeTerms.rs` |
| `img_` | Image | `img_productPhoto.rs` |
| `txt_` | Text / Title | `txt_mainTitle.rs` |

---

## Layer 1 — Page Classes

Page classes encapsulate raw UI interactions. They live in `Keywords/com/tuempresa/page/`.

### Rules
- NO `@Keyword` annotation
- Use `Mobile.*` and `findTestObject()` freely
- Return `Map` from validation methods (use `UtilsPage.validateElements()`)
- Always use `FailureHandling.OPTIONAL` for uncertain elements, `STOP_ON_FAILURE` for critical ones
- Take screenshots on failures: `Mobile.takeScreenshot()`

### Template — new Page class

```groovy
package com.tuempresa.page.android   // or .ios or .common

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.tuempresa.page.common.UtilsPage
import internal.GlobalVariable
import tuempresa.utils.SmartWaitPage

public class ExamplePage {

    UtilsPage utilsPage = new UtilsPage()

    // Simple tap action
    public void tapContinueButton() {
        def btn = findTestObject('Object Repository/android/Example/btn_continue')
        def nextElement = findTestObject('Object Repository/android/Example/lbl_title')
        Mobile.tap(btn, SmartWaitPage.SHORT)
        SmartWaitPage.waitVisible(nextElement, SmartWaitPage.MEDIUM)   // ← always prefer over Mobile.delay()
    }

    // Screen validation — returns Map with success/present/missing
    public Map validateExampleScreen() {
        Map<String, TestObject> elements = [
            "Título pantalla":  findTestObject('Object Repository/android/Example/lbl_title'),
            "Botón Continuar":  findTestObject('Object Repository/android/Example/btn_continue')
        ]
        return utilsPage.validateElements(elements)
    }
}
```

### UtilsPage — shared utilities (already created)

```groovy
// Scroll down until element is visible (max 10 attempts, platform-aware)
utilsPage.scrollToElement(TestObject testObject)

// Scroll looking for an element by its visible text
utilsPage.scrollToElement(String elementName)

// Validate multiple elements at once
// Returns: [success: bool, present: List, missing: List, details: Map]
utilsPage.validateElements(Map<String, TestObject> elements)
```

---

## Layer 2 — Steps Classes

Steps classes expose Page actions as Katalon Custom Keywords. They live in `Keywords/com/tuempresa/steps/`.

### Rules
- ALL methods MUST have `@Keyword`
- ONLY delegate to the corresponding Page class — no `Mobile.*` here
- Handle errors gracefully: `KeywordUtil.markFailed()` / `KeywordUtil.markWarning()`
- Calling convention from scripts: `CustomKeywords.'com.tuempresa.steps.android.ExampleSteps.methodName'()`
- **NO sobrecargas (@Keyword) con mismo nombre**: Katalon Studio regenera `Libs/CustomKeywords.groovy` y produce `def static "x.y.method"` duplicado → Eclipse JDT no resuelve clases en cascada (95+ errores en Problems panel). Si necesitas variantes, usa nombres distintos (`openTargetApp`, `openTargetAppWith`).
- **NO `import internal.GlobalVariable` en Keywords**: cuando Katalon compila el Keyword antes que `internal/GlobalVariable.groovy`, lanza `NoClassDefFoundError: internal/GlobalVariable` al ejecutar desde el IDE. Resuelve por reflexión: `Class.forName('internal.GlobalVariable').getDeclaredField(name).get(null)`. Ver `AppLauncherSteps.openTargetApp()` como referencia.

### Logging de aserciones — patrón obligatorio

Cualquier Steps que haga un `assert*` (validación crítica) debe usar `tuempresa.utils.AssertLogger` para imprimir banners ASCII visibles en el Log Viewer de Katalon. Esto estandariza el output entre todos los TCs y permite escanear los logs a simple vista.

```groovy
import tuempresa.utils.AssertLogger
...
@Keyword
def assertSomeCondition() {
    AssertLogger.start('N', 'descripción corta del assert')
    def payload = SomePage.awaitOrFetch(...)
    AssertLogger.pass('N', 'descripción corta', [
        // Atributos relevantes — pares clave→valor cualesquiera, se imprimen como tabla.
        adToken   : payload?.adToken,
        source    : payload?.source,
        productId : payload?.productId,
        price     : payload?.price
    ])
}
```

`pass()` invoca `KeywordUtil.markPassed()` internamente, así que el step queda ✅ en el reporte. Para eventos con listas (`items[]` de conversion, productos de carrito, etc.) usar `AssertLogger.logItems('header', listaDeMaps, ['clave1','clave2'])`. Referencia completa: ver `Keywords/com/tuempresa/steps/android/EventTrackerSteps.groovy`.

### Marcador visible en el árbol de ejecución (Scripts)

El árbol de ejecución de Katalon muestra cada `Mobile.comment(...)` como una fila propia. Para que las ASERCIONES sean **identificables a simple vista** entre decenas de filas STEP, los Scripts (.tc) DEBEN preceder cada llamada a un `assert*` keyword con un comentario con el siguiente formato fijo:

```groovy
Mobile.comment('🔎🔎🔎  ASSERT N  🔎🔎🔎  descripción corta')
CustomKeywords.'com.tuempresa.steps.android.SomeSteps.assertSomething'()
```

Donde `N` es el número del assert dentro del TC y la descripción corta dice qué se está validando (ej.: `rads-tracker render (Banner)`). Los 3 lupa emojis al frente y atrás convierten la fila en un divisor visual fuerte. NO usar este marcador para STEPs normales (que mantienen el formato `STEP N: ...` para distinguirse).

Las filas en el árbol quedan así:
- `STEP 2: Tocar banner Compose → LandingPromo` ← step UI normal
- `🔎🔎🔎 ASSERT 3 🔎🔎🔎 rads-tracker click (Banner)` ← marca de assert (visible al instante)
- `com.tuempresa.steps.android.EventTrackerSteps.assertClickBannerEvent() (0.014s)` ← la ejecución del assert

Referencia: ver Scripts `TestData0SearchStoreHome/Script1742688000000.groovy` y `TestData0SearchStoreCorridor/Script1779600000000.groovy`.

### Template — new Steps class

```groovy
package com.tuempresa.steps.android   // or .ios or .common

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.util.KeywordUtil
import com.tuempresa.page.android.ExamplePage

public class ExampleSteps {

    ExamplePage examplePage = new ExamplePage()

    @Keyword
    def tapContinueButton() {
        examplePage.tapContinueButton()
    }

    @Keyword
    def validateExampleScreen() {
        Map results = examplePage.validateExampleScreen()
        if (results.success) {
            Mobile.comment("Pantalla validada correctamente ✅")
        } else {
            Mobile.comment("Elementos faltantes: ${results.missing} ❌")
        }
    }
}
```

---

## Layer 3 — Script

Scripts are pure orchestrators. They live in `Scripts/android/<TICKET-ID>/Script<timestamp>.groovy`.

### ⚠️ Regla de path mirroring (CRÍTICA — nunca omitir)

El path del script debe **espejarse exactamente** desde la ubicación del Test Case. Katalon vincula un `.tc` con su script buscando la carpeta de igual nombre bajo `Scripts/`. El nombre del TC (y por tanto el de la carpeta del script) debe cumplir la [convención de nomenclatura obligatoria](#project-architecture) `<KEY_PROYECTO>-TC-<ID>-<validacionOCasoDePrueba>`.

| Test Case path | Script path correcto |
|----------------|---------------------|
| `Test Cases/android/Login/SIM-TC-4-loginExitoso.tc` | `Scripts/android/Login/SIM-TC-4-loginExitoso/Script<timestamp>.groovy` |
| `Test Cases/android/TurboStore/TBS-TC-7-agregarProductoAlCarrito.tc` | `Scripts/android/TurboStore/TBS-TC-7-agregarProductoAlCarrito/Script<timestamp>.groovy` |
| `Test Cases/android/Geant/GNT-TC-3-verificarHeaderVisible.tc` | `Scripts/android/Geant/GNT-TC-3-verificarHeaderVisible/Script<timestamp>.groovy` |

**Regla:** `Test Cases/<platform>/<subfolder>/<TC_Name>.tc` → `Scripts/<platform>/<subfolder>/<TC_Name>/Script<timestamp>.groovy`

❌ **Incorrecto:** `Scripts/android/<run-id>/Script<timestamp>.groovy` (usa el run-id del pipeline en lugar del nombre del TC)  
✅ **Correcto:** `Scripts/android/<subfolder-del-TC>/<nombre-del-TC>/Script<timestamp>.groovy`

> Si el script se crea en la ruta incorrecta, el Test Case aparece vacío en Katalon Studio (pestaña Manual en blanco) y **no puede ejecutarse**. El script correcto debe crearse ANTES de agregar código, no después.

### Rules
- ONLY call `CustomKeywords.'com.tuempresa.steps.*'()`
- ONLY direct `Mobile.*` calls allowed: `startExistingApplication`, `closeApplication`, `comment`
- Use `Mobile.comment()` to document every step — these appear in the test log
- Each script = one test case (maps 1:1 with a `.tc` file)

### Script template

```groovy
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import internal.GlobalVariable as GlobalVariable

// ─── PRECONDICIÓN ─────────────────────────────────────────────
Mobile.comment('Inicia la app de TuEmpresa')
Mobile.startExistingApplication(GlobalVariable.G_AppBundleID)

Mobile.comment('Cierra pantallas intermedias')
CustomKeywords.'com.tuempresa.steps.common.HomeSteps.skipScreensHome'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('Navega al Help Center')
CustomKeywords.'com.tuempresa.steps.common.HomeSteps.goToHelpCenter'()

Mobile.comment('Selecciona la primera orden')
CustomKeywords.'com.tuempresa.steps.android.HelpCenterSteps.selectFirstOrder'()

// ... resto del flujo ...

// ─── POSTCONDICIÓN ────────────────────────────────────────────
Mobile.closeApplication()
```

---

## Common Errors & Fixes

### `Name is null at MobileLocatorStrategy.valueOf`
**Cause**: Missing `<locatorStrategy>` in `.rs` or using `<WebElementEntity>` instead of `<MobileElementEntity>`.
**Fix**: Add `<locatorStrategy>ATTRIBUTES</locatorStrategy>` and `<platform>ANDROID</platform>`.

### Element tap hits wrong element
**Cause**: Multiple elements match the locator (same `text` in list + header).
**Fix**: Add `resource-id` property to narrow the match. Check duplicates with `mobile_list_elements_on_screen`.

### `Mobile.tap` does nothing
**Cause**: `timeout=0` on slow-loading screen.
**Fix**: Use `timeout=10` and add `Mobile.waitForElementPresent(..., 5)` before tap.

### `CustomKeywords` method not found at runtime
**Cause**: Method in Page class lacks `@Keyword`, or it's in Steps but called with wrong package path.
**Fix**: `@Keyword` must be in Steps class. Verify the fully-qualified path: `'com.tuempresa.steps.android.ExampleSteps.methodName'`.

### GlobalVariable is null at runtime
**Cause**: Variable not defined in `Profiles/default.glbl`, or `GlobalVariable.groovy` not regenerated.
**Fix**: Add the `<GlobalVariableEntity>` entry in `default.glbl`, then reload the project in Katalon Studio.

### `Mobile.delay()` abuse causing flaky tests
**Cause**: Using fixed delays instead of element-based waits.
**Fix**: Replace `Mobile.delay(N)` with `Mobile.waitForElementPresent(element, N, FailureHandling.OPTIONAL)`. Use `delay` only after confirmed navigation to give the screen time to settle.

### `MissingMethodException: No signature of method: ScriptXXX.findTestCase()`
**Cause**: `findTestCase()` was called without a static import. It is NOT a global method — it belongs to `TestCaseFactory`.
**Fix**: Always use a static import at the top of any script that calls another test case:
```groovy
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
```
Then call it directly:
```groovy
WebUI.callTestCase(findTestCase('Test Cases/android/openApp'), [:], FailureHandling.STOP_ON_FAILURE)
```
**Anti-pattern** (do NOT do this):
```groovy
// ❌ findTestCase() sin import estático — MissingMethodException
WebUI.callTestCase(findTestCase('Test Cases/android/openApp'), [:])

// ❌ import no-estático — funciona pero NO es el estándar del SKILL
import com.kms.katalon.core.testcase.TestCaseFactory
WebUI.callTestCase(TestCaseFactory.findTestCase('Test Cases/android/openApp'), [:])
```

---

## Modular Test Reuse — callTestCase

Usa `callTestCase` cuando un script necesita reutilizar otro test case ya existente como bloque de setup.

### Cuándo usarlo
- El script necesita ejecutar `openApp` (login/startup) antes de su flujo propio.
- Se quiere reutilizar un flujo de precondición sin copiar código.

### Import obligatorio
```groovy
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
```

### Patrón correcto
```groovy
// Delegar startup al test openApp
Mobile.comment('Abrir TuEmpresa (delega a Test Cases/android/openApp)')
WebUI.callTestCase(findTestCase('Test Cases/android/openApp'), [:], FailureHandling.STOP_ON_FAILURE)

// Continuar con el flujo propio
CustomKeywords.'com.tuempresa.steps.android.SuperSteps.prepareHomeAndDismissPopup'()
```

### Regla de scope
- `callTestCase` solo se usa en Capa 3 (Script).
- Nunca llamar `callTestCase` desde Page ni Steps — viola la separación de capas.
- El test llamado debe existir en `Test Cases/` con su `.tc` y su Script correspondiente.
