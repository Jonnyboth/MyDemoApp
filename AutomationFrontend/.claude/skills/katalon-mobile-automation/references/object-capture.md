# Object Capture Reference — Katalon Mobile Automation

This reference covers advanced techniques for capturing UI elements from real Android and iOS devices.

## ⚡ Fast Capture: UIAutomator adb dump (MÉTODO PRIMARIO)

> **Usar este método siempre que adb esté disponible.** Es mucho más rápido y confiable que `mobile_list_elements_on_screen` — retorna el árbol COMPLETO de UI en una sola llamada.

### ¿Por qué es mejor?

| Criterio | UIAutomator dump | mobile_list_elements_on_screen |
|---|---|---|
| Velocidad | ~1-2 segundos | 5-15 segundos |
| Fiabilidad | Alta (adb directo) | Media (depende de MCP) |
| Cobertura | Árbol completo del viewport | Solo nodos activos expuestos |
| Atributos | Todos: resource-id, text, content-desc, class, bounds, clickable, enabled | Parcial: según implementación MCP |
| Fallo MCP | No afecta | Puede fallar con "Cannot read properties of undefined" |

### Procedimiento de captura

```bash
# Paso 1 — Conectar y confirmar dispositivo
adb devices
# Salida esperada: <deviceId>  device

# Paso 2 — Navegar a la pantalla objetivo (usar mobile-mcp para interacción)
# Ejemplo: mobile_launch_app, mobile_click_on_screen_at_coordinates, mobile_swipe_on_screen

# Paso 3 — Capturar árbol UI completo (UN SOLO COMANDO)
adb -s <deviceId> shell uiautomator dump /sdcard/uidump.xml && \
adb -s <deviceId> pull /sdcard/uidump.xml /tmp/uidump_<pantalla>.xml && \
cat /tmp/uidump_<pantalla>.xml

# Resultado: XML completo con TODOS los nodos de la pantalla
```

### Estructura XML resultante

```xml
<?xml version="1.0" encoding="UTF-8"?>
<hierarchy rotation="0">
  <node index="0" text="" resource-id="" class="android.widget.FrameLayout"
        package="com.tuempresa.app" content-desc=""
        clickable="false" enabled="true" bounds="[0,0][1080,2340]">
    <node index="0" text="Geant" resource-id="com.tuempresa.app:id/storeName"
          class="android.widget.TextView" content-desc=""
          clickable="true" enabled="true" bounds="[54,1910][586,1969]">
    </node>
    <node index="1" text="" resource-id="com.tuempresa.app:id/searchBar"
          class="android.widget.EditText" content-desc="Buscar en Geant"
          clickable="true" enabled="true" bounds="[0,120][1080,220]">
    </node>
  </node>
</hierarchy>
```

### Parsing: extraer locators del dump

Para cada nodo `<node>` relevante, extraer:
| Atributo XML | Uso en .rs |
|---|---|
| `resource-id` | Locator primario (ID y ATTRIBUTES XPath) |
| `text` | Locator secundario (TEXT en ATTRIBUTES XPath) |
| `content-desc` | Locator terciario (ACCESSIBILITY) |
| `class` | Tipo de widget para XPath |
| `bounds` | `[x1,y1][x2,y2]` → centro = `((x1+x2)/2, (y1+y2)/2)` |
| `clickable` | Incluir si `clickable="true"` O si tiene `resource-id` de TuEmpresa |

### Captura multi-sección (pantallas con scroll)

```bash
# Sección 1 (viewport inicial)
adb -s <deviceId> shell uiautomator dump /sdcard/sec1.xml
adb -s <deviceId> pull /sdcard/sec1.xml /tmp/sec1.xml

# Scroll (via mobile-mcp)
# mobile_swipe_on_screen: startX=540, startY=1500, endX=540, endY=700

# Sección 2
adb -s <deviceId> shell uiautomator dump /sdcard/sec2.xml
adb -s <deviceId> pull /sdcard/sec2.xml /tmp/sec2.xml

# Repetir hasta fin de pantalla
```

---

## Standard Capture Workflow (mobile-mcp — FALLBACK)

Usar solo si `adb` no está disponible.

### 1. Navigate to the target screen
Always be on the exact screen before capturing. Take a screenshot to confirm:
```
mobile_take_screenshot(device="<deviceId>")
```

### 2. List all elements
```
mobile_list_elements_on_screen(device="<deviceId>")
```

Each element in the response has:
```json
{
  "type": "android.widget.TextView",
  "text": "Geant",
  "label": "",
  "identifier": "com.app:id/storeName",
  "coordinates": {
    "x": 475, "y": 1969,
    "width": 532, "height": 59
  }
}
```

---

## Choosing the Best Locator

### Priority order (most stable first)

| Strategy | When to use | Example .rs property |
|---|---|---|
| `resource-id` | Always use when available | `<name>resource-id</name>` |
| `text` + `class` | Static labels, buttons | `<name>text</name>` |
| `content-desc` | Icon buttons, images | `<name>content-desc</name>` |
| `ANDROID_UI_AUTOMATOR` | Complex conditions | Only in locatorCollection |
| Absolute XPATH | Last resort | Only in locatorCollection |

### Red flags — avoid these locators
- `index` — breaks when order changes
- `bounds` / `x` / `y` coordinates — breaks on different screen sizes
- Absolute XPath with many `/` levels — breaks with UI updates

## Handling Duplicate Elements

If two elements share the same `text` (e.g., "Geant" in favorites AND in supermercados list):

1. Use `resource-id` to differentiate:
   ```xml
   <webElementProperties>
      <name>resource-id</name>
      <value>com.tuempresa.app:id/storeName</value>
   </webElementProperties>
   ```

2. Or ATTRIBUTES XPath combining text + resource-id:
   ```
   //*[@class='android.widget.TextView' and @text='Geant' and @resource-id='com.tuempresa.app:id/storeName']
   ```

## Scrolling to Find Off-Screen Elements

Elements not visible don't appear in dumps. Scroll first:

### Vertical scroll (via adb)
```bash
adb -s <deviceId> shell input swipe 540 1500 540 700 600
# Swipe UP (scroll down): startY > endY
# Swipe DOWN (scroll up): startY < endY
```

### Navigate back (via adb)
```bash
adb -s <deviceId> shell input keyevent KEYCODE_BACK
```

## Coordinate System

UIAutomator dump returns coordinates in **physical pixels** matching the display resolution.

For Samsung SM-S928B (Android 16): Screen 1080 × 2340 physical pixels.

Use coordinates directly in `adb shell input swipe` and `Mobile.swipe()`.

## Capturing Elements Inside Lists / RecyclerViews

For elements inside scrollable lists:
1. Scroll until the element is fully visible
2. Capture with UIAutomator dump
3. Use `resource-id` of the container + `text` of the label

Example — Géant store card:
- Container: `android.view.ViewGroup` with `content-desc="121607"`
- Label: `resource-id="com.tuempresa.app:id/storeName"` + `text="Geant"`
- Best locator: `resource-id=com.tuempresa.app:id/storeName` + `text=Geant`

## Handling Modals and Toasts

```groovy
try {
    Mobile.tap(findTestObject('FeatureName/buttonDismissModal'), 2)
} catch (Exception e) {
    // Modal not present, continue
}
```

## iOS-Specific Capture Notes

On iOS, element properties differ:
- `type` → `XCUIElementType` prefix
- Use `name` (accessibility identifier) instead of `resource-id`
- Set `<platform>IOS</platform>` in the `.rs` file
- Use `IOS_PREDICATE_STRING` in locatorCollection

```xml
<locatorCollection>
   <entry>
      <key>IOS_PREDICATE_STRING</key>
      <value>label == "Súper" AND type == "XCUIElementTypeStaticText"</value>
   </entry>
   <entry>
      <key>ACCESSIBILITY</key>
      <value>Súper</value>
   </entry>
   <entry><key>XPATH</key></entry>
</locatorCollection>
<locatorStrategy>IOS_PREDICATE_STRING</locatorStrategy>
<platform>IOS</platform>
```
