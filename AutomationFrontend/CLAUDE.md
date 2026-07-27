# AutomationFrontend — contexto para agentes

## Qué automatiza este proyecto

El objetivo activo de este proyecto es la **app Android nativa** (Katalon Studio +
Appium): `Keywords/`, `Scripts/`, `Test Cases/`, `Object Repository/`, `Profiles/`,
`runner/`. Todo el trabajo de automatización que se pide en este repo — nuevos test
cases, page objects, steps, debugging — es sobre **Android**, salvo que el usuario diga
explícitamente lo contrario.

## `WebKeywords/` — scaffold inactivo, no lo expandas sin que te lo pidan

`WebKeywords/` (Playwright + Node.js) es un scaffold de automatización **web**
(navegador), separado y sin relación con la app Android. Existe únicamente porque se
pidió explícitamente en una conversación puntual, como plantilla lista por si algún día
hay un frontend web real que probar (ej. una web companion de la app). Hoy **no prueba
nada real** y no debe crecer solo.

**Regla dura para cualquier agente/skill que trabaje en este repo:**

- No crees, edites ni expandas archivos dentro de `WebKeywords/` (nuevas Page, Steps o
  specs) a menos que el usuario pida explícitamente automatización **web** en ese mismo
  turno de conversación.
- No agregues dependencias de automatización web nuevas a `package.json`
  (`playwright`, `cypress`, `puppeteer`, `webdriverio`, `selenium-webdriver`,
  `testcafe`, `nightwatch`, `taiko`, etc.) sin esa misma confirmación explícita.
  `@playwright/test` ya está instalado desde antes de esta regla — no lo repitas como
  excusa para tocar más cosas de `WebKeywords/`.
- Si un pedido es ambiguo ("automatiza el login", "crea un test para X") y no dice
  "web"/"navegador"/"browser", asume **Android/Katalon**. No infieras web por
  comodidad ni "para dejarlo listo".
- Si genuinamente el usuario pide algo web, sí puedes trabajar en `WebKeywords/` —
  sigue el mismo patrón de 3 capas que ya existe ahí (`pages/` → `steps/` → `tests/`,
  ver `eslint.config.js` para las reglas de frontera entre capas).

## Blindaje mecánico

El pre-commit de Husky (`.husky/pre-commit` → `.husky/scripts/check-web-scope-guard.sh`)
bloquea el commit si `package.json` agrega una dependencia de automatización web que no
estaba ya en el último commit. Esto es una red de seguridad mecánica, **no reemplaza**
esta regla — un agente puede violarla igual escribiendo código dentro de `WebKeywords/`
sin agregar dependencias nuevas. Lee esta sección antes de tocar `WebKeywords/`.
