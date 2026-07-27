# Integración del Runner con Agentes IA

Cómo hacer que tus agentes de automatización (BMO-TestCreator, QA-Automatizador, etc.) ejecuten el runner automáticamente después de crear scripts, y cómo integrar el runner en pipelines de IA para nuevos proyectos.

---

## Regla de oro

> **Un script de automatización no está terminado hasta que el runner lo pasa.**

El runner es el mecanismo de verificación E2E. Sin él, el agente puede crear código sintácticamente correcto pero que falla en dispositivo real por locators erróneos, timings incorrectos o diferencias de API.

---

## Cómo agregar el runner al agente BMO-TestCreator

### Sección a agregar en el `.agent.md` del creador de tests

```markdown
### Runner headless — Validación E2E obligatoria

Después de crear todos los archivos de automatización, ejecutar el runner:

```bash
cd "/ruta/absoluta/al/proyecto"
bash runner/run.sh <TC_NAME>
```

**<TC_NAME>** = nombre exacto del Test Case (sin extensión ni ruta).
Ejemplo: `TC_CatalogoToppingsHappyPath`

**Criterio de éxito:** La salida debe contener `✓ [PASSED ] <TC_NAME>`.

**Si FAILED:**
1. Leer la línea `[FAILED]` y el `[STEP]` anterior para ubicar el punto de falla.
2. Leer el screenshot de error (path aparece como `Screenshot: .../reports/screenshot_*.png`).
3. Aplicar fix mínimo en el archivo indicado.
4. Re-ejecutar el runner.
5. Iterar máximo 3 veces; si persiste → escalar al usuario.

**El agente NO puede reportar tarea completada si el runner no ha pasado.**
```

---

## Cómo agregar el runner al orquestador QA-Automatizador

### Nueva fase a insertar después de TestCreator en el pipeline

```markdown
### FASE RUNNER — Verificación E2E (OBLIGATORIA)

**Comando:**
```bash
bash runner/run.sh <TC_NAME>
```

**Si PASSED:**
- Actualizar estado: `Runner: passed`
- Continuar a reporte final

**Si FAILED:**
- Leer log completo → extraer: step fallido, archivo afectado, path del screenshot
- Invocar BMO-Debugger con el log de error
- Después del fix → re-ejecutar runner (máx. 3 ciclos)
- Si después de 3 ciclos sigue fallando → escalar al usuario con diagnóstico

**Prompt para BMO-Debugger:**
```
Test fallido: <TC_NAME>
Error del runner:
<pegar líneas [FAILED] y [STEP] anterior>
Screenshot: <path>
Archivo afectado: <deducir del stack>

Tarea:
1. Leer el archivo afectado.
2. Diagnosticar causa raíz (screenshot MCP o UIAutomator dump si es necesario).
3. Aplicar fix mínimo — NO refactorizar.
4. Reportar el cambio exacto aplicado.
```
```

---

## Prompt completo para instalar el runner + configurar agentes en un proyecto nuevo

Dar este prompt a Claude Code en el **proyecto destino**:

---

```
Quiero integrar el Katalon Headless Runner en este proyecto de automatización Katalon.
El runner es un framework headless que compila y ejecuta tests Groovy de Katalon contra Appium
sin necesidad de abrir Katalon Studio.

## Datos del entorno

- Proyecto fuente (donde ya existe el runner): <<RUTA_PROYECTO_FUENTE>>
- Dispositivo Android (adb devices): <<SERIAL>>
- App package: <<com.tuempresa.app>>
- App activity: <<com.tuempresa.app.MainActivity>>
- Versión Android: <<14>>

## Tareas a realizar en orden

### 1. Instalar el runner
Ejecutar el script de instalación automática:
```bash
bash "<<RUTA_PROYECTO_FUENTE>>/runnerTestingTool/install.sh" \
  --source "<<RUTA_PROYECTO_FUENTE>>" \
  --target "$(pwd)" \
  --udid "<<SERIAL>>" \
  --package "<<com.tuempresa.app>>" \
  --activity "<<com.tuempresa.app.MainActivity>>"
```

Si no tienes el script, instalar manualmente:
1. Copiar `<<RUTA_PROYECTO_FUENTE>>/runner/` a este proyecto
2. Editar `runner/config/runner.yml` con los datos del dispositivo
3. `chmod +x runner/run.sh`

### 2. Verificar instalación
```bash
bash runner/run.sh list
```
Debe mostrar los test cases disponibles.

### 3. Configurar agentes IA para usar el runner

Si este proyecto tiene archivos `.agent.md` en `.github/agents/`, añadir la siguiente regla
en el agente que crea tests (ej: BMO-TestCreator.agent.md o similar):

```markdown
### Validación con Runner headless (obligatoria)

Después de crear cualquier test case, ejecutar:
```bash
bash runner/run.sh <TC_NAME>
```
El agente no puede reportar éxito hasta que el runner muestre `✓ [PASSED]`.
Si falla, aplicar fixes iterativos (máx. 3 ciclos) antes de escalar.
```

Si el proyecto tiene un orquestador (QA-Automatizador.agent.md o similar), añadir
una fase "Runner" entre TestCreator y el reporte final, con la misma regla.

### 4. Primer test de validación
Elegir el test más simple del proyecto y ejecutarlo:
```bash
bash runner/run.sh <<TC_NombreTest>>
```

Reportar el resultado completo (PASSED/FAILED + tiempo de ejecución).

### Criterios de éxito
- `bash runner/run.sh list` muestra los tests del proyecto ✅
- El primer test ejecuta (aunque falle por locators incorrectos está bien) ✅
- Los archivos `.agent.md` tienen la regla del runner integrada ✅
```

---

## Cómo leer el output del runner para debug

```
[STEP]   Paso 6: Tap "Ir a pagar" en barra del store         ← paso en curso
[INFO]   Screenshot: .../reports/screenshot_20260413_171657.png  ← screenshot
[FAILED] Element not visible after 15s: btn_irACanasta.rs    ← AQUÍ ESTÁ EL ERROR
[STEP]   Tap barra "Ir a canasta"...                         ← siguiente intento
✗ [FAILED ] TC_MiTest (45210ms)                             ← resultado final
```

Patrón de lectura:
1. Buscar `[FAILED]` → esa línea tiene el mensaje de error exacto
2. Leer el `[STEP]` justo antes → ese es el paso que falló
3. Abrir el screenshot mencionado → ver qué estaba en pantalla al momento del fallo
4. Leer el archivo del elemento (`btn_irACanasta.rs`) → verificar locator

---

## Variables de entorno para CI/CD

```bash
# Sobrescribir udid del runner.yml en CI (para dispositivos dinámicos):
export KATALON_DEVICE_UDID=$(adb devices | awk 'NR==2{print $1}')

# El runner lee runner.yml — para CI, también se puede pasar como argumento:
bash runner/run.sh run --case android/TC_MiTest

# Generar reporte en directorio personalizado:
bash runner/run.sh run --report /tmp/test-results TC_MiTest
```

---

## Estructura de archivos generados por el runner

```
runner/reports/
├── test-results.xml              ← JUnit XML (para CI/CD)
├── screenshot_20260413_171657.png  ← Capturas en cada fallo o takeScreenshot()
├── screenshot_20260413_171744.png
└── ...
```

El `test-results.xml` es compatible con:
- GitHub Actions (actions/upload-artifact + test-reporter)
- Jenkins (junit plugin)
- GitLab CI (junit: runner/reports/test-results.xml)
- Azure DevOps (PublishTestResults task)
