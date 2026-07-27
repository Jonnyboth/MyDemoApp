# Runner Headless — Manual de Setup y Portabilidad

Este manual explica cómo dejar funcionando el runner headless en una máquina nueva o con un dispositivo distinto. El proyecto está pensado para ser exportable; toda la configuración volátil (UDID, paquete de app, versión Android) vive en archivos puntuales que se editan sin tocar el código.

---

## 1. Prerrequisitos

El runner es multiplataforma (Android + Web). La tabla marca qué necesita cada plataforma — un proyecto solo-Web puede saltarse la fila Android y viceversa.

| Herramienta | Versión recomendada | Verificación | Plataforma |
|-------------|---------------------|---------------|------------|
| Java JDK | 17+ (probado con 21) | `java -version` | Ambas |
| Gradle | 8.9+ | `gradle -v` o usa el wrapper del proyecto | Ambas |
| Node.js | 18+ | `node -v` | Ambas (tooling auxiliar) |
| Appium | 2.x | `appium -v` | Android |
| Driver `uiautomator2` | 4.x | `appium driver list --installed` | Android |
| Android SDK + `platform-tools` | API 33+ | `adb version` | Android |
| adb autorizado al device | — | `adb devices` debe mostrar `device`, no `unauthorized` | Android |
| Chrome/Chromium/Firefox | cualquier reciente | `google-chrome --version` | Web |

Para Web no hace falta instalar chromedriver/geckodriver: Selenium Manager (incluido desde Selenium 4.6, ya usado por este runner) resuelve y descarga el driver correcto según el navegador detectado.

### Instalación rápida en macOS

```bash
brew install openjdk@21 gradle node
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
npm install -g appium
appium driver install uiautomator2
```

> Si ya usaste Katalon Studio en esta máquina, hay un Android SDK en `~/.katalon/tools/android_sdk` que el runner detecta automáticamente.

---

## 2. Archivos que tocar al cambiar de entorno

Hay **solo tres** archivos con datos específicos del entorno. Todo lo demás es portable.

### 2.1 `runner/config/runner.yml`

```yaml
appium:
  url: http://localhost:4723           # cambia solo si Appium corre en otro host/puerto
  newCommandTimeout: 300

device:
  udid: R5CY111XY3E                    # ← cambiar: serial del adb (adb devices)
  platformName: Android
  platformVersion: "14"                # ← cambiar: ver Settings > About phone
  automationName: UiAutomator2
  appPackage: com.tuempresa.app      # ← cambiar si pruebas otra app
  appActivity: com.tuempresa.app.MainActivity   # ← cambiar para otra app
  noReset: true
  fullReset: false
  autoGrantPermissions: true

runner:
  reportDir: runner/reports
  screenshotOnFailure: true
  retryOnFailure: 0
  defaultTimeout: 15
```

Cómo obtener cada dato:

| Campo | Comando |
|-------|---------|
| `udid` | `adb devices` (segunda columna) |
| `platformVersion` | `adb shell getprop ro.build.version.release` |
| `appPackage` | `adb shell dumpsys window | grep mCurrentFocus` con la app abierta |
| `appActivity` | `adb shell dumpsys activity activities | grep -i mResumedActivity` |

### 2.2 `Profiles/default.glbl`

Las GlobalVariables que consume Katalon **y** el runner (vía `KatalonRunner`):

```xml
<GlobalVariableEntity>
   <initValue>'com.tuempresa.app'</initValue>     <!-- G_AppBundleID -->
   <name>G_AppBundleID</name>
</GlobalVariableEntity>
...
<GlobalVariableEntity>
   <initValue>'R5CY111XY3E'</initValue>             <!-- G_DevicesName -->
   <name>G_DevicesName</name>
</GlobalVariableEntity>
```

> **Importante:** `runner.yml > device.udid` y `default.glbl > G_DevicesName` deben coincidir. Si no, los keywords que invocan `adb -s <udid>` (ej: `AppLauncherPage.launchTuEmpresaViaAdb`) apuntarán a otro device.

### 2.3 Variables de entorno (opcional pero recomendado)

Para evitar editar archivos en máquinas dinámicas (CI, devs rotando devices):

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"       # o ~/.katalon/tools/android_sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

En CI puedes sobrescribir el UDID en tiempo de ejecución sin tocar `runner.yml`:

```bash
export KATALON_DEVICE_UDID="$(adb devices | awk 'NR==2{print $1}')"
# (el runner lee runner.yml; este export es referencia para tu pipeline)
```

---

## 3. Pasos para dejar todo corriendo

### 3.1 Verificar el dispositivo

```bash
adb devices
# Debe mostrar:
# List of devices attached
# <SERIAL>  device
```

Si aparece `unauthorized` → desbloquea el device y acepta el diálogo "Allow USB debugging".

### 3.2 Arrancar Appium

Hay un helper en `runner/start-appium.sh` que resuelve `ANDROID_HOME` automáticamente:

```bash
# Background (recomendado)
bash runner/start-appium.sh --bg
# Foreground (ver logs en vivo)
bash runner/start-appium.sh
# Detenerlo
bash runner/start-appium.sh --stop
```

Variables opcionales:

```bash
APPIUM_PORT=4723           # debe coincidir con runner.yml > appium.url
APPIUM_BASE_PATH=/         # el runner concatena tal cual el url del YAML
```

> Si prefieres lanzarlo a mano: `ANDROID_HOME=... appium -p 4723 --base-path / --log-level info`

### 3.3 Ejecutar tests

```bash
# Un solo TC (resuelve dependencias setUp automáticamente)
bash runner/run.sh TestData0SearchStoreHome

# Listar todos los TCs disponibles
bash runner/run.sh list

# Reporte JUnit XML
cat runner/reports/test-results.xml
```

Criterio de éxito: la salida termina con `✓ [PASSED ] <TC>`.

---

## 4. Troubleshooting

### "Could not start a new session ... ANDROID_HOME nor ANDROID_SDK_ROOT"
Appium se lanzó sin las variables de entorno del SDK. Detén y vuelve a lanzar con el helper: `bash runner/start-appium.sh --stop && bash runner/start-appium.sh --bg`.

### "Connection refused" / "ClosedChannelException" al iniciar tests
No hay Appium escuchando en el puerto de `runner.yml`. Verifica:
```bash
lsof -iTCP:4723 -sTCP:LISTEN
```
Si vacío → `bash runner/start-appium.sh --bg`.

### `uiautomator dump` muere con "Killed"
Hay múltiples instancias de Appium o de UIAutomator2 sosteniendo el bridge. Limpia:
```bash
pkill -9 -f appium
bash runner/start-appium.sh --bg
```

### Test falla en setUp con "Elemento no encontrado en 20s: lbl_inicioTab.rs"
La app TuEmpresa usa Jetpack Compose y el bottom-nav **no** expone los labels en la jerarquía de vistas Android. El locator de `lbl_inicioTab.rs` apunta a un texto que solo aparece en el Home real (`text="Beneficios Pro"`). Si TuEmpresa rebranda esa card, actualiza el atributo `text` en `Object Repository/android/Home/lbl_inicioTab.rs` con un texto Home-only verificado con `uiautomator dump` (ej.: `Turbo`, `Súper`, otra card vertical).

Otros indicadores **incorrectos** (ya descartados): `content-desc="Inicio"` (no existe en Compose), `text="¿Qué quieres hoy?"` (aparece también en verticales como Restaurantes).

### Test pasa openApp pero falla en pasos posteriores con cart no vacío
`noReset: true` preserva la sesión del usuario y la canasta es server-side. Pasos como STEP 10 (`agregarProductoDesdeDetalle → verifyBasketFooterVisible`) pueden fallar si la canasta ya tenía ese producto. Para limpiar:
- Manualmente en device: abrir TuEmpresa → ir a Canasta → vaciar.
- O cambiar momentáneamente `noReset: false` y `fullReset: true` en `runner.yml` para una corrida limpia (cierra sesión).

### appActivity incorrecta — Appium hangs en "waiting for ..."
Si `runner.yml > device.appActivity` apunta a un activity que no existe en la app, Appium se cuelga esperándola y termina con timeout. Para TuEmpresa la actividad real es `com.tuempresa.discovery.onboarding.impl.activities.MainEntryActivity`. Verifica en cualquier momento con la app abierta:
```bash
adb -s <UDID> shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity"
```

Para inspeccionar la jerarquía actual:
```bash
adb -s <UDID> shell uiautomator dump /sdcard/ui.xml
adb -s <UDID> pull /sdcard/ui.xml /tmp/ui.xml
grep -oE 'text="[^"]*"' /tmp/ui.xml | sort -u
```

### "Killed" al lanzar Appium en macOS
macOS firma binarios; si instalas Appium vía `npm -g` la primera ejecución puede ser bloqueada por Gatekeeper. Ejecuta una vez en foreground (`bash runner/start-appium.sh`) para ver el diálogo de permiso y aprobar.

### El device no se desbloquea solo entre runs
Configura el device en modo desarrollador:
- `Settings > Developer options > Stay awake` = ON
- `Settings > Developer options > USB debugging` = ON
- (Samsung) `Settings > Developer options > Disable adb authorization timeout` = ON

### El runner reporta PASSED pero la app quedó en un estado raro
`noReset: true` mantiene el estado entre runs (intencional para reusar setUp). Si necesitas reset limpio:
```yaml
device:
  noReset: false
  fullReset: true
```
o manualmente: `adb shell pm clear com.tuempresa.app`.

---

## 5. Exportar el proyecto a otra máquina

Lo que **NO** se debe versionar (ya está en `.gitignore` esperado):
- `runner/build/` (artefactos de gradle)
- `runner/reports/` (capturas y XMLs locales)
- `bin/`, `Libs/cache/` (cache de Katalon)
- `*.iml`, `.idea/`, `.gradle/`

Lo que **SÍ** se versiona y debe viajar con el proyecto:
- `runner/src/`, `runner/build.gradle`, `runner/config/runner.yml`
- `runner/run.sh`, `runner/start-appium.sh`, `runner/SETUP.md`
- `Test Cases/`, `Scripts/`, `Keywords/`, `Object Repository/`
- `Profiles/default.glbl`
- `.github/agents/`

Checklist para una máquina nueva:
1. `git clone <repo>`
2. Instalar prerrequisitos (sección 1).
3. Conectar device, `adb devices` → copiar el serial.
4. Editar `runner/config/runner.yml`: `device.udid` + `platformVersion`.
5. Editar `Profiles/default.glbl`: `G_DevicesName` con el mismo serial.
6. `bash runner/start-appium.sh --bg`
7. `bash runner/run.sh openApp` para validar el setup.

Si paso 7 pasa → el entorno está listo. Si falla → ir a la sección Troubleshooting.

---

## 6. Estructura mínima del runner

```
runner/
├── build.gradle              # gradle shadowJar — empaqueta el runner-all.jar
├── config/
│   └── runner.yml            # ← configuración por entorno (editable)
├── src/main/groovy/runner/
│   ├── KatalonRunner.groovy        # entry point, descubre TCs y los ejecuta
│   ├── AppiumDriverManager.groovy  # crea sesión Appium con caps de runner.yml
│   ├── TestCaseLoader.groovy       # lee .tc y resuelve dependencias setUp/tearDown
│   └── ...
├── run.sh                    # build + ejecutar
├── start-appium.sh           # helper para arrancar Appium con env correcto
└── SETUP.md                  # este archivo
```

El JAR auto-empaquetado (`build/libs/runner-all.jar`) es lo único que ejecuta el runner — gradle lo regenera automáticamente si detecta cambios en `src/`.
