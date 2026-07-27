# Exploración Guiada — extensión de `skill_qa_engineer`

> Capacidad opcional para documentar el comportamiento real de la app **antes** de diseñar
> Casos de Prueba, cuando el requerimiento es ambiguo o no hay evidencia suficiente para
> escribir pasos verificables. Usa las MCP declaradas en `.mcp.json` de este proyecto.

## Selección de herramienta según plataforma

```
{plataforma}
├── Android / iOS (app nativa/híbrida)  →  mobile-mcp   (mcp__mobile-mcp__mobile_*)
│                                           — o appium-mcp (mcp__appium-mcp__*) si el usuario
│                                             lo pide explícitamente / necesita control de sesión
│                                             Appium más fino (drivers, capabilities, etc.)
├── Web                                  →  playwright-mcp (mcp__playwright-mcp__browser_*)
│                                           o aisquare-playwright si el usuario lo pide explícitamente
└── No especificada                      →  Pregunta al usuario antes de iniciar
```

> **Estado de `mobile-mcp`**: ✅ configurado y **verificado en vivo** — `mobile_list_available_devices`
> respondió correctamente (`{"devices":[]}`, sin dispositivo/emulador activo en ese momento,
> lo cual es una condición normal de entorno, no un error de conexión). El servidor completo
> expone estas herramientas: `mobile_list_available_devices`, `mobile_launch_app`,
> `mobile_terminate_app`, `mobile_list_apps`, `mobile_install_app`, `mobile_uninstall_app`,
> `mobile_take_screenshot`, `mobile_save_screenshot`, `mobile_start_screen_recording`,
> `mobile_stop_screen_recording`, `mobile_list_elements_on_screen`,
> `mobile_click_on_screen_at_coordinates`, `mobile_double_tap_on_screen`,
> `mobile_long_press_on_screen_at_coordinates`, `mobile_swipe_on_screen`, `mobile_type_keys`,
> `mobile_press_button`, `mobile_open_url`, `mobile_get_screen_size`, `mobile_get_orientation`,
> `mobile_set_orientation`, `mobile_get_crash`, `mobile_list_crashes`.

## mobile-mcp — Exploración móvil (Android / iOS) — herramienta preferida

1. `mobile_list_available_devices` → confirmar el dispositivo/emulador activo. Si devuelve
   `{"devices":[]}`, no hay ningún dispositivo/emulador corriendo — pide al usuario que
   levante uno antes de continuar (no es un error de la MCP).
2. `mobile_list_apps` → verificar si la app objetivo ya está instalada; `mobile_install_app`
   si hace falta instalarla primero.
3. `mobile_launch_app` → abrir la app objetivo (`appPackage`/bundle id).
4. `mobile_take_screenshot` → capturar el estado inicial de la pantalla.
5. `mobile_list_elements_on_screen` → obtener el árbol de elementos con nombres/tipos exactos.
6. `mobile_click_on_screen_at_coordinates` / `mobile_double_tap_on_screen` /
   `mobile_long_press_on_screen_at_coordinates` → interactuar con el elemento localizado
   según el gesto que requiera el flujo.
7. `mobile_type_keys` → ingresar texto cuando aplique.
8. `mobile_swipe_on_screen` → scroll/navegación cuando el elemento no es visible.
9. `mobile_open_url` → probar deep links directamente cuando el AC lo requiera.
10. Repetir captura (`mobile_take_screenshot`) antes y después de cada interacción relevante.
11. `mobile_press_button` → botones de sistema (atrás, home) cuando el flujo lo requiera.
12. `mobile_get_orientation` / `mobile_set_orientation` → validar comportamiento en
    landscape/portrait cuando el AC lo pida.
13. `mobile_list_crashes` / `mobile_get_crash` → si la app crashea durante la exploración,
    captura el detalle del crash como evidencia técnica antes de reportarlo con
    [bug_report.md](bug_report.md).
14. `mobile_start_screen_recording` / `mobile_stop_screen_recording` → grabación en vídeo
    para flujos largos o intermitentes difíciles de documentar solo con screenshots.
15. `mobile_save_screenshot` → guardar evidencia con nombre descriptivo de los hallazgos clave.
16. `mobile_terminate_app` / `mobile_uninstall_app` → limpieza al finalizar, si el flujo lo
    requiere (ej. probar una instalación limpia en la siguiente iteración).
17. Anotar mensajes de error/validación **verbatim**, tal como se muestran en pantalla.

## appium-mcp — Alternativa ya configurada (control de sesión Appium más fino)

Útil cuando se necesita manejar capabilities/drivers específicos que `mobile-mcp` no expone.
Sigue las instrucciones propias del servidor: establece sesión antes de interactuar.

1. `select_device` → elegir/confirmar el dispositivo o emulador activo.
2. `appium_session_management` (`action: create`) → abrir la sesión de automatización.
3. `appium_screenshot` → capturar el estado inicial de la pantalla.
4. `appium_get_page_source` → obtener el árbol de elementos con nombres/tipos exactos.
5. `appium_find_element` (preferir `accessibility id` o `id` sobre XPath largo) → localizar
   el elemento a interactuar.
6. `appium_gesture` → tap/swipe/drag sobre el elemento localizado.
7. `appium_mobile_keyboard` / `appium_set_value` → ingresar texto cuando aplique.
8. Repetir captura (`appium_screenshot`) antes y después de cada interacción relevante.
9. `appium_mobile_press_key` → botones de sistema (atrás, home) cuando el flujo lo requiera.
10. Anotar mensajes de error/validación **verbatim**, tal como se muestran en pantalla.

Si necesitas instalar/verificar el entorno Appium local, usa primero `appium_skills` (doctor/
smoke test) antes de asumir comandos — no inventes pasos de instalación manual.

## playwright-mcp / aisquare-playwright — Exploración web

1. `browser_navigate` → URL del ambiente correspondiente.
2. `browser_take_screenshot` → estado inicial.
3. `browser_snapshot` → árbol de accesibilidad con nombres/roles exactos de cada elemento.
4. `browser_click` / `browser_type` / `browser_fill_form` → interactuar con la pantalla.
5. `browser_wait_for` → esperar estados de carga antes de capturar el resultado.
6. `browser_console_messages` → capturar errores de JavaScript.
7. `browser_network_requests` → identificar llamadas a API y sus respuestas.
8. Registrar mensajes de error/validación **verbatim** y reglas de negocio observadas.

## Qué hacer con los hallazgos

La exploración **enriquece el CÓMO** se prueba un escenario (nombres reales de elementos,
textos exactos de error, datos de frontera observados) — **nunca determina el QUÉ** se
prueba cuando existe un ticket de Jira con criterios de aceptación explícitos; en ese caso
los ACs siempre mandan sobre lo observado.

Persiste los hallazgos como notas dentro de la conversación o, si el usuario lo pide para
reutilización futura (o si el hallazgo es lo bastante extenso como para cubrir un módulo o
pantalla completa), en un archivo Markdown bajo la estructura de carpetas por tipo de
dispositivo → app/página → módulo → sub-módulo definida en
[references/exploration-doc-structure.md](references/exploration-doc-structure.md) (documenta
módulos, funciones principales, nombres reales de componentes y flujos; crea las carpetas si
no existen; nunca dupliques un archivo para la misma pantalla — edítalo). Esta documentación es
el insumo obligatorio que [audit_test_case.md](audit_test_case.md) consulta antes de crear un
Caso de Prueba relacionado (Regla 11 de
[references/formatting-rules.md](references/formatting-rules.md)). Si el usuario solo pide
"explora y créame los casos de prueba" sin pedir documentación reutilizable, pasa directo a
diseñar los TCs con [formatting-rules.md](references/formatting-rules.md) — la documentación en
`.md` sigue siendo opcional en ese caso puntual, no un bloqueo.

## Restricciones

- En modo exploración no se crean Casos de Prueba ni se reportan bugs — solo se documenta.
- Si se detecta un comportamiento sospechoso durante la exploración, anótalo y sugiere usar
  [bug_report.md](bug_report.md) para formalizarlo — no lo reportes automáticamente sin
  confirmación del usuario.
- No inventes nombres de elementos ni mensajes de error — todo debe salir de una captura o
  snapshot real de la sesión de exploración.
