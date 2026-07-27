# Guía de Instalación — Katalon Headless Runner

Instala el runner en **cualquier proyecto Katalon Studio** en 4 pasos.

---

## Prerequisitos

El runner es multiplataforma (Android + Web) — instala solo lo que tu proyecto use.

```bash
# Java 11+ y Gradle 7+ (siempre requeridos)
java -version
gradle -version
brew install gradle   # macOS si no está instalado
```

**Si tu proyecto tiene test cases Android** (`Test Cases/android/...`):
```bash
# Appium Server 2.x corriendo en localhost:4723
appium --version
appium server &       # si no está corriendo

# UiAutomator2 driver instalado en Appium
appium driver list --installed

# Dispositivo Android conectado
adb devices           # debe mostrar el serial del dispositivo
```

**Si tu proyecto tiene test cases Web** (`Test Cases/web/...`):
```bash
# Un navegador Chrome/Chromium/Firefox instalado en el host.
# Selenium Manager (incluido) resuelve y descarga el driver correcto solo —
# no hace falta instalar chromedriver/geckodriver a mano.
google-chrome --version   # o chromium/firefox, según runner.yml > web.browser
```

---

## Paso 1 — Copiar la carpeta runner/

Copia la carpeta `runner/` completa al directorio raíz de tu proyecto Katalon (al mismo nivel que `Keywords/`, `Scripts/`, `Object Repository/`):

```bash
# Desde el directorio raíz de tu nuevo proyecto:
cp -r /ruta/al/proyecto-fuente/runner ./runner
```

Estructura esperada después de copiar:
```
mi-proyecto-katalon/
├── Keywords/
├── Object Repository/
├── Scripts/
├── Test Cases/
├── Profiles/
└── runner/          ← recién copiado
    ├── run.sh
    ├── build.gradle
    ├── settings.gradle
    ├── config/
    │   └── runner.yml
    └── src/
```

---

## Paso 2 — Configurar runner.yml

Edita `runner/config/runner.yml` con los datos de tu dispositivo y app:

```yaml
appium:
  url: http://localhost:4723        # URL de tu servidor Appium
  newCommandTimeout: 300            # timeout de sesión en segundos

device:
  udid: TU_SERIAL_AQUI              # obtener con: adb devices
  platformName: Android
  platformVersion: "14"             # versión Android del dispositivo
  automationName: UiAutomator2
  appPackage: com.tu.app            # package de tu app
  appActivity: com.tu.app.MainActivity  # activity principal
  noReset: true                     # true = mantiene estado entre runs
  fullReset: false
  autoGrantPermissions: true

runner:
  reportDir: runner/reports         # relativo a la raíz del proyecto
  screenshotOnFailure: true
  retryOnFailure: 0
  defaultTimeout: 15                # segundos para waits de elementos
```

Para obtener los valores necesarios:
```bash
# Serial del dispositivo
adb devices

# Package y Activity de la app
adb shell dumpsys package com.tu.app | grep -E "Activity|versionName"

# O si tienes la app instalada:
adb shell dumpsys window windows | grep mCurrentFocus
```

---

## Paso 3 — Verificar Perfiles Katalon

El runner lee `Profiles/default.glbl` para cargar GlobalVariables. Verifica que el archivo exista:

```bash
ls Profiles/default.glbl
```

Si no existe, crea uno mínimo:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<GlobalVariableEntities>
   <description></description>
   <name>default</name>
   <defaultProfile>true</defaultProfile>
   <globalVariableEntities>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'android'</initValue>
         <name>G_Platform</name>
      </GlobalVariableEntity>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'TU_SERIAL_AQUI'</initValue>
         <name>G_DevicesName</name>
      </GlobalVariableEntity>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'com.tu.app'</initValue>
         <name>G_AppBundleID</name>
      </GlobalVariableEntity>
   </globalVariableEntities>
</GlobalVariableEntities>
```

> **Nota:** Los valores en `runner.yml` (device.udid y device.appPackage) sobreescriben automáticamente `G_DevicesName` y `G_AppBundleID` del .glbl en cada ejecución.

---

## Paso 4 — Primer build y ejecución

```bash
# Desde la raíz del proyecto
cd runner/
bash run.sh list         # verifica que encuentra tus test cases

# Si hay error de Gradle no encontrado:
export PATH="$HOME/.gradle/wrapper/dists/gradle-8.9-bin/*/gradle-8.9/bin:$PATH"
# O instalar: brew install gradle

cd ..
bash runner/run.sh TC_NombreDelTest   # corre un test específico
```

El primer run tarda ~30s en compilar. Los siguientes son inmediatos si no hay cambios en los fuentes.

---

## Troubleshooting

### "gradle no encontrado"
```bash
brew install gradle          # macOS
# O usar el wrapper de Gradle si el proyecto tiene gradlew:
./gradlew shadowJar
```

### "Appium server not running"
```bash
appium server --port 4723 &
# Esperar a que muestre: "Appium REST http interface listener started on 0.0.0.0:4723"
```

### "No devices found" / "adb not found"
```bash
# Verificar que ANDROID_HOME apunta al SDK:
export ANDROID_HOME=$HOME/.katalon/tools/android_sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH
adb devices
```

### "ClassNotFoundException: com.tuempresa.steps.SomeSteps"
El runner busca Keywords en `Keywords/`. Verifica que las clases Groovy estén en rutas que coincidan con el package:
- `com.tuempresa.page.Foo` → `Keywords/com/tuempresa/page/Foo.groovy`
- `com.mi.steps.Bar` → `Keywords/com/mi/steps/Bar.groovy`

### "findTestObject: file not found"
La ruta en `findTestObject('Object Repository/android/Pantalla/elemento')` debe coincidir exactamente con la ubicación del archivo `.rs` (sin extensión).

### El test pasa pero no encuentra elementos
Verificar en `runner.yml`:
- `udid` correcto (`adb devices`)
- `appPackage` exacto
- Appium corriendo y UiAutomator2 driver instalado:
  ```bash
  appium driver install uiautomator2
  ```

---

## Integración CI/CD

El runner genera `runner/reports/test-results.xml` en formato JUnit estándar — compatible con GitHub Actions, Jenkins, GitLab CI:

```yaml
# GitHub Actions ejemplo
- name: Run Katalon tests
  run: bash runner/run.sh run --tag smoke

- name: Publish test results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: runner/reports/
```

```groovy
// Jenkinsfile
stage('Test') {
    sh 'bash runner/run.sh run --tag regression'
    junit 'runner/reports/test-results.xml'
}
```
