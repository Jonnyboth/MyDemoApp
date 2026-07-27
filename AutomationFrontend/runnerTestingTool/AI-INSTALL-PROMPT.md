# Prompt de Instalación para IA — Katalon Headless Runner

Copia este prompt completo y dáselo a Claude (u otro agente de IA) para instalar el runner en un nuevo proyecto Katalon.

---

## INSTRUCCIONES DE USO

1. Abre Claude Code en el **proyecto Katalon de destino** (donde quieres instalar el runner)
2. Copia el bloque `PROMPT` que aparece abajo
3. Reemplaza los valores entre `<< >>` con los datos reales de tu proyecto
4. Pégalo en el chat de Claude

---

## PROMPT

```
Quiero instalar el Katalon Headless Runner en este proyecto. 
El runner es una herramienta que permite ejecutar tests de Katalon desde terminal sin abrir la UI, 
compilando los scripts Groovy on-demand contra Appium.

## Datos del proyecto destino

- App package: <<com.tu.empresa.app>>
- App activity: <<com.tu.empresa.app.MainActivity>>
- App name: <<NombreApp>>
- Serial del dispositivo (adb devices): <<SERIAL_DEL_DISPOSITIVO>>
- Versión Android: <<14>>
- Package base de Keywords: <<com.tuempresa.steps>> (ej: com.acme.steps)

## Datos del proyecto fuente (donde ya está el runner)

- Ruta absoluta del proyecto fuente: <<"/Users/usuario/KatalonProyecto/testAndroid">>

## Tarea

Instala el Katalon Headless Runner en este proyecto siguiendo estos pasos en orden:

### PASO 1 — Copiar la carpeta runner/
Lee todos los archivos de `<<ruta_proyecto_fuente>>/runner/` y créalos en este proyecto 
en la misma estructura relativa. La carpeta runner/ debe quedar al mismo nivel que Keywords/, Scripts/, etc.

Archivos a copiar (leer del fuente y escribir en destino):
- runner/run.sh
- runner/build.gradle  
- runner/settings.gradle
- runner/config/runner.yml
- runner/src/main/groovy/runner/KatalonRunner.groovy
- runner/src/main/groovy/runner/TestCaseLoader.groovy
- runner/src/main/groovy/runner/ScriptExecutor.groovy
- runner/src/main/groovy/runner/ReportGenerator.groovy
- runner/src/main/groovy/runner/AppiumDriverManager.groovy
- runner/src/main/groovy/runner/ObjectRepositoryParser.groovy
- runner/src/main/groovy/runner/GlobalVariableLoader.groovy
- runner/src/main/groovy/runner/TestCase.groovy
- runner/src/main/groovy/runner/TestResult.groovy
- runner/src/main/groovy/CustomKeywords.groovy
- runner/src/main/groovy/internal/GlobalVariable.groovy
- runner/src/main/groovy/com/kms/katalon/core/annotation/Keyword.groovy
- runner/src/main/groovy/com/kms/katalon/core/model/FailureHandling.groovy
- runner/src/main/groovy/com/kms/katalon/core/testcase/TestCaseEntity.groovy
- runner/src/main/groovy/com/kms/katalon/core/testcase/TestCaseFactory.groovy
- runner/src/main/groovy/com/kms/katalon/core/testobject/TestObject.groovy
- runner/src/main/groovy/com/kms/katalon/core/testobject/SelectorMethod.groovy
- runner/src/main/groovy/com/kms/katalon/core/testobject/ObjectRepository.groovy
- runner/src/main/groovy/com/kms/katalon/core/util/KeywordUtil.groovy
- runner/src/main/groovy/com/kms/katalon/core/mobile/keyword/MobileBuiltInKeywords.groovy
- runner/src/main/groovy/com/kms/katalon/core/webui/keyword/WebUiBuiltInKeywords.groovy

### PASO 2 — Configurar runner/config/runner.yml
Después de copiar, edita runner/config/runner.yml con los datos reales del dispositivo:

```yaml
appium:
  url: http://localhost:4723
  newCommandTimeout: 300

device:
  udid: <<SERIAL_DEL_DISPOSITIVO>>
  platformName: Android
  platformVersion: "<<14>>"
  automationName: UiAutomator2
  appPackage: <<com.tu.empresa.app>>
  appActivity: <<com.tu.empresa.app.MainActivity>>
  noReset: true
  fullReset: false
  autoGrantPermissions: true

runner:
  reportDir: runner/reports
  screenshotOnFailure: true
  retryOnFailure: 0
  defaultTimeout: 15
```

### PASO 3 — Verificar Profiles/default.glbl
Verifica que exista `Profiles/default.glbl` en el proyecto. Si no existe, créalo con estas variables mínimas:

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
         <initValue>'<<SERIAL_DEL_DISPOSITIVO>>'</initValue>
         <name>G_DevicesName</name>
      </GlobalVariableEntity>
      <GlobalVariableEntity>
         <description></description>
         <initValue>'<<com.tu.empresa.app>>'</initValue>
         <name>G_AppBundleID</name>
      </GlobalVariableEntity>
   </globalVariableEntities>
</GlobalVariableEntities>
```

### PASO 4 — Verificar permisos de ejecución
Asegúrate de que run.sh tiene permisos de ejecución:
```bash
chmod +x runner/run.sh
```

### PASO 5 — Primer test de instalación
Ejecuta el siguiente comando para verificar que el runner detecta los test cases del proyecto:
```bash
cd "$(pwd)"
bash runner/run.sh list
```

Si todo está correcto, deberías ver una lista de los test cases disponibles en el proyecto.

### PASO 6 — Ejecutar el primer test
```bash
bash runner/run.sh <<TC_NombreDelTest>>
```

### Criterios de éxito
- `bash runner/run.sh list` muestra los test cases del proyecto
- El primer run compila sin errores de Gradle  
- El test ejecuta y muestra el resultado en consola (`✓ PASSED` o `✗ FAILED`)
- Se genera `runner/reports/test-results.xml`

### Si hay errores
- "gradle not found" → instalar con `brew install gradle` (macOS) o `sdk install gradle` (Linux)
- "Appium not running" → ejecutar `appium server --port 4723 &` antes del test
- "Device not found" → verificar con `adb devices` y actualizar runner.yml
- "ClassNotFoundException" → verificar que los packages en los Groovy files coincidan con la estructura de carpetas en Keywords/

Por favor, reporta el resultado de cada paso antes de continuar con el siguiente.
```

---

## Notas para el instalador IA

El runner tiene estas dependencias críticas que deben estar correctas:

### 1. Estructura de Test Cases
El runner busca scripts en esta relación:
- `Test Cases/android/TC_Foo.tc` → busca `Scripts/android/TC_Foo/Script*.groovy`
- `Test Cases/android/sub/TC_Bar.tc` → busca `Scripts/android/sub/TC_Bar/Script*.groovy`

### 2. Package de Keywords
Los Keywords deben estar en carpetas que espején su package:
```
Keywords/com/tuempresa/page/FooPage.groovy    → package com.tuempresa.page
Keywords/com/tuempresa/steps/FooSteps.groovy  → package com.tuempresa.steps
```

### 3. Object Repository
Los `.rs` files deben estar en `Object Repository/` y referenciados como:
```groovy
findTestObject('Object Repository/android/Pantalla/elemento')
// → lee Object Repository/android/Pantalla/elemento.rs
```

### 4. Bridges que NO se simulan (no soportados)
Estas APIs de Katalon no tienen implementación en el runner:
- `WebUI.*` (solo `callTestCase` está implementado)
- `WS.*` (API testing)
- `DatabaseKeywords.*`
- Plugins de Katalon
- `DriverFactory.*`
- Testops integration

Si tu proyecto usa estas APIs, el test fallará con `MissingMethodException`.

---

## Versión rápida del prompt (para proyectos simples)

Si el proyecto fuente ya está accesible para el agente:

```
Instala el Katalon Headless Runner desde <<ruta_fuente>>/runner/ 
en este proyecto. Copia toda la carpeta runner/ aquí, luego actualiza 
runner/config/runner.yml con:
  - udid: <<SERIAL>>
  - appPackage: <<com.tu.app>>
  - appActivity: <<com.tu.app.MainActivity>>
  - platformVersion: "<<VERSION>>"

Finalmente ejecuta: bash runner/run.sh list
para confirmar que funciona.
```
