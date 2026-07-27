# Reglas de compatibilidad Katalon Studio / Groovy — R-K4, R-K5, R-K6, R-K6.1

> Copia canónica. `qa-debugger` referencia este archivo en vez de duplicarlo.
> Reglas derivadas de fallos reales en producción.

## R-K4 — Sintaxis Groovy conservadora

El editor de Katalon Studio usa un compilador Eclipse-Groovy más estricto que el runner
standalone (`runner/`). Una clase que pasa en `bash runner/run.sh` puede fallar al
compilarse en Katalon Studio, generando cascada de errores "unable to resolve class" en
todos los Test Cases que la usan.

**Prohibido en archivos Page/Steps/Script:**
- Slashy regex `/pattern/` → usar `java.util.regex.Pattern.compile("...")` o
  `Pattern.compile(...).matcher(s)`.
- Cast a arrays primitivos `as int[]` → usar `List` y `Integer` boxed.
- Cadenas largas con `intdiv` y aritmética compleja → desglosar en variables intermedias
  con tipos explícitos.
- Em-dashes (`—`), comillas tipográficas (`" " ' '`) en código (string literals de UI
  están bien).
- Tabs y espacios mezclados en el mismo archivo.

## R-K5 — Validación dual (manual, opcional en el pipeline autónomo)

> Ver nota en `qa-test-creator/manifest.yaml → mandatory_validation.katalon_studio_gui_gate`:
> este gate NO bloquea el loop autónomo del Orquestador (requiere un humano frente al IDE
> gráfico de Katalon Studio, que ni siquiera está instalado en el entorno headless del
> runner). Se documenta aquí como buena práctica si el usuario trabaja con Katalon Studio
> Desktop en su propia máquina.

Validación dual recomendada antes de dar una tarea por completada en ese contexto:
1. Runner headless: `bash runner/run.sh <TC>` debe imprimir `✓ [PASSED ] <TC>`.
2. Katalon Studio: abrir el TC en el editor; el Problems panel debe mostrar **0 errors,
   0 warnings** sobre archivos del feature.

Si (1) pasa pero (2) falla, el TC no está listo para uso en la UI de Katalon. Endurecer
la sintaxis (R-K4) hasta que ambos pasen.

## R-K6 — Si una Page nueva rompe la compilación en Katalon, inline en Steps

Si un archivo Page nuevo introduce un import `com.tuempresa.*` que el editor de Katalon no
resuelve (aunque el `.class` exista), el camino correcto es **eliminar la Page y
consolidar su lógica dentro del Steps** que la usaba. Cross-imports frágiles entre
archivos nuevos producen cascada de "unable to resolve class".

Antes de reintentar compilar tras el cambio, borrar artefactos viejos:
```bash
rm -f "bin/keyword/com/tuempresa/<paquete>/<Clase>.class" \
      "bin/keyword/com/tuempresa/<paquete>/<Clase>.groovy"
```

Excepción: reusar clases existentes que ya compilan (`GeantPage`, etc.) es seguro — la
regla solo aplica a Pages **nuevas** introducidas en la misma entrega.

## R-K6.1 — Prohibido borrar `bin/keyword/` en bloque

**Jamás ejecutar:**
```bash
rm -rf bin/keyword              # ❌ PROHIBIDO
rm -rf bin/keyword/com          # ❌ PROHIBIDO
rm -rf bin/keyword/com/tuempresa    # ❌ PROHIBIDO
rm -rf bin/listener bin/groovy  # ❌ PROHIBIDO
```
Solo `rm -f` por archivo de clase específico (`.class` + `.groovy` del mismo nombre),
nunca con `-r`, nunca a nivel de directorio.

**Razón:** `Libs/CustomKeywords.groovy` es autogenerado y referencia TODAS las clases
Steps por FQN. Si se borra `bin/keyword/` entero, el classloader del editor de Katalon no
resuelve ninguna clase → cascada de "unable to resolve class" en cada línea de
`Libs/CustomKeywords.groovy`. Katalon no recompila automáticamente al detectar el
directorio vacío — requiere `Project → Clean` manual.

**Recuperación si el directorio quedó corrupto:**
1. `bash runner/rebuild-keywords.sh` desde la raíz — usa el groovyc embebido en
   `runner-all.jar` para regenerar `bin/keyword/`, `bin/listener/`, `bin/groovy/`.
2. Pedir `Project → Refresh (F5)` en Katalon Studio.
3. Si no funciona, escalar para `Project → Clean…`.
