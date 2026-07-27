# AI_USAGE.md — Bitácora de Co-Pilotaje e IA

Documentación transparente de cómo se usó IA agéntica para construir y operar este
repositorio (prueba técnica AVL Mobility Solutions).

## Herramientas utilizadas

| Herramienta | Rol en el flujo |
|---|---|
| **Claude Code (Sonnet 5 / Anthropic)** | Agente principal en IDE. Diseñó la arquitectura del repo, generó código (Groovy/Katalon, Python/pytest), documentación y corrió comandos de instalación/ejecución. |
| **Sistema de Skills propio (`.prompts/`, `.claude/skills/`)** | No es una herramienta externa sino la capa de orquestación construida sobre Claude: cada módulo tiene un `super_agent_*.md` (Scrum Master AI) que activa skills especializadas por fase (PM, QA, Dev, automatización). |
| **MCP `aio-tests-mcp` / Atlassian** | Creación y consulta de Historias de Usuario y Casos de Prueba directamente en Jira/AIO Tests desde el agente, sin salir del editor. |
| **MCP `mobile-mcp` / `appium-mcp`** | Exploración e inspección de la app Android (SauceLabs My Demo App) sobre el emulador real para mapear selectores estables antes de escribir código. |
| **MCP `playwright-mcp`** | Exploración web puntual (fuera del foco principal, que es Android). |

## Arquitectura agéntica del proyecto

El repo no usa un único prompt de IA: cada módulo tiene su propio **orquestador** que
seguí explícitamente durante el desarrollo:

- `FuncionalQaPm/.prompts/super_agent_Qa_PM.md` — gestión de backlog (BDD) y QA
  funcional/exploratoria (Jira + AIO Tests).
- `AutomationFrontend/.claude/agents/qa-orchestrator.md` — pipeline fijo de 5 pasos
  (Planificar → Explorar → Crear Tests → Correr Tests → Validar) para automatización
  Android/Katalon.
- `AutomationBackend/.prompts/super_agent_automation_backend.md` — diseño → construcción
  → ejecución (gate obligatorio) de pruebas de API con `pytest`.

Cada orquestador tiene una regla dura: si una fase requiere una skill que no está
implementada, **se detiene y avisa** en vez de simular su comportamiento. Esto evitó que
el agente inventara convenciones de código que aún no existían.

## Casos de uso específicos

- **Boilerplate de automatización mobile:** generación de la arquitectura Katalon POM
  de 3 capas (Object Repository `.rs`, `Keywords/com/.../page`, `Keywords/com/.../steps`,
  `Scripts`, `.tc`) a partir de un plan aprobado sobre selectores capturados vía
  UIAutomator/`mobile-mcp`.
- **Selectores complejos:** priorización de `resource-id`/`accessibilityId` sobre XPath
  absoluto, resueltos con evidencia real de dispositivo (no inventados).
- **Esquemas JSON / contratos de API:** generación de `pydantic`/`jsonschema` a partir de
  las respuestas reales de DummyJSON y JSONPlaceholder, con validación de SLA (<1.5s).
- **Generación de HUs y bugs en Jira/AIO Tests:** redacción de historias en formato BDD y
  reporte de defectos con evidencia adjunta, vía `skill_project_manager`/`skill_qa_engineer`.
- **Reportes:** generación del dashboard HTML autocontenido (`core/html_report.py`) y del
  reporter de consola de `AutomationBackend`, sin plugins de terceros.

## Ejemplos de prompts clave

### 1. Automatización mobile real (Login, proyecto SIM)

```
@FuncionalQaPm/.prompts/super_agent_Qa_PM.md
@AutomationFrontend/.claude/agents/qa-orchestrator.md

Automatiza los siguientes TCs:
OS: Android
Ticket_HU: [SIM-TC-4, SIM-TC-5, SIM-TC-6]
TCs_list: [loginExitoso, loginCuentaBloqueada, loginUsernameObligatorio]
key_proyect: SIM
Precondiciones:
- Dispositivo Android Emulator - qa_android:5554
- Keywords y funciones de login ya existentes (reutilizar, no duplicar)
```

**Resultado:** el orquestador corrió el pipeline completo (Planificar → Explorar →
Crear Tests → Correr Tests → Validar) y produjo
`Test Cases/android/Login/SIM-TC-{4,5,6}-*.tc` + su Object Repository, validados contra
el runner headless antes de reportarse como PASSED.

### 2. Automatización de API real (Users/Posts, proyecto SIM)

```
@FuncionalQaPm/.prompts/super_agent_Qa_PM.md
@AutomationBackend/.prompts/super_agent_automation_backend.md

Automatiza las siguientes apis:
Ticket_HU: [SIM-TC-12]
TCs_list: [create_user (age > 100), login, get_post_by_id]
key_proyect: SIM
Precondiciones:
- BASE_URL de users → https://dummyjson.com, BASE_URL de posts → https://jsonplaceholder.typicode.com
- Convención de nombre obligatoria: test_<KEY_SIN_GUIONES>_<descripcion>.py
```

**Resultado:** matriz de escenarios validada → implementación en capas
(`config`→`models`/`builders`→`services`→`tests`) → corrida real con `pytest` como gate
antes de marcar la tarea `[Done]` en el backlog.

Los siguientes dos prompts (inventados para este entregable) se redactaron con la
metodología **Rol → Contexto → Tarea → Restricciones → Criterios de aceptación**, en vez
de una instrucción suelta — reduce la ambigüedad y evita que el agente dé por completado
un paso que no verificó.

### 3. Levantar el ambiente del proyecto desde cero 

```
ROL
Actúa como Ingeniero DevOps/QA responsable de dejar operativo, de punta a punta, el
entorno local de este repositorio en una máquina recién clonada (WSL2/Ubuntu).

CONTEXTO
El repo tiene dos capas de automatización independientes:
- AutomationFrontend: Katalon headless runner (Java/Gradle) + Appium contra un emulador Android.
- AutomationBackend: suite pytest + requests contra APIs públicas (DummyJSON, JSONPlaceholder).
No hay credenciales privadas ni servicios internos: todo corre contra herramientas
open-source y APIs públicas.

TAREA
1. Verifica/instala Java 11+, Gradle 7+, Node.js 18 LTS y Android SDK; expone
   ANDROID_HOME/ANDROID_SDK_ROOT y adb en el PATH.
2. Levanta el AVD "qa_android" (Pixel 6, API 34, google_apis x86_64) o confirma un
   dispositivo real conectado (adb devices).
3. Instala y arranca Appium Server 2.x con el driver UiAutomator2 en localhost:4723.
4. Crea el venv de AutomationBackend/tests, instala requirements.txt y copia
   .env.example a .env.

RESTRICCIONES
- No asumas que un paso quedó listo sin verificarlo con un comando real (adb devices,
  appium --version, pytest --version).
- No modifiques Profiles/, settings/ ni build.gradle salvo que sea estrictamente
  necesario para el build.

CRITERIOS DE ACEPTACIÓN
- "bash runner/run.sh list" lista los Test Cases sin error de conexión.
- "pytest tests/smoke/ --collect-only" recolecta los tests sin errores de import.
- Si algo no quedó listo, repórtalo explícitamente en vez de continuar como si funcionara.
```

### 4. Segmentar y crear el sistema agéntico 

```
ROL
Actúa como Arquitecto de Sistemas Agénticos, responsable de diseñar la estructura de
orquestación de IA de este repositorio — no de escribir código de producto.

CONTEXTO
Hoy el proyecto avanza a golpe de prompts sueltos por módulo, sin un protocolo repetible;
esto genera inconsistencia en cómo se ejecuta y se aprueba cada fase (planeación, diseño,
construcción, QA).

TAREA
1. Define un catálogo de skills independientes por rol (PM, QA, Dev, UI/UX), cada una
   como carpeta de prompts en .prompts/<skill>/ (system.md + tools.md).
2. Crea un orquestador por módulo (super_agent_<modulo>.md) que invoque esas skills en
   fases secuenciales: Planeación → Diseño → Construcción → Verificación.
3. Ninguna fase puede saltarse la aprobación de QA antes de marcar una tarea como Done
   en docs/BACKLOG.md.

RESTRICCIONES
- Si el catálogo de un orquestador referencia una skill cuya carpeta no existe todavía
  en .prompts/, el orquestador debe detenerse y avisarme explícitamente — nunca
  improvisar su comportamiento ni simular un system.md mental.
- No dupliques reglas ya documentadas dentro de cada skill; el orquestador solo coordina,
  no reimplementa.

CRITERIOS DE ACEPTACIÓN
- Cada super_agent_*.md documenta su catálogo, el protocolo de fases y la regla de
  "no improvisar skills inexistentes".
- Un mismo flujo (ej. automatizar un TC) se reproduce invocando el orquestador correcto,
  sin ambigüedad sobre qué skill activar en cada fase.
```

## Reflexión técnica

La IA aceleró de forma notoria el andamiaje repetitivo: la arquitectura de 3 capas en
Katalon, las capas `config`/`models`/`services` en `pytest` y el reporter HTML
autocontenido se construyeron en una fracción del tiempo que habría tomado escribirlos a
mano. Ese tiempo se reinvirtió en diseñar la estrategia de pruebas y priorizar por
riesgo, en lugar de en tareas repetitivas de bajo valor.

El punto crítico fue no aceptar el primer resultado como definitivo. Estos son los casos
concretos de errores o alucinaciones detectados y corregidos durante el desarrollo:

- **Confusión Web vs. Mobile en `AutomationFrontend`:** como el módulo ya traía un
  scaffold preparado para automatización web (`WebKeywords/`), al pedir automatizar la
  app Android (SauceLabs My Demo App) sobre el emulador ya inicializado, el agente
  intentó automatizarla en paralelo como Web *y* como Mobile, generando código basura en
  ambas capas — pese a que la instrucción siempre fue "app Android en el emulador ya
  inicializado". Se corrigió bloqueando el alcance: `WebKeywords/` no se toca salvo que
  se pida automatización web explícitamente en ese mismo turno (regla documentada en
  `AutomationFrontend/CLAUDE.md`).
- **Límite del agente para llegar al host Windows:** el setup real es una PC Windows 11
  con WSL2/Ubuntu como workspace. Verificar los tests mobile directamente en Katalon
  Studio (no solo vía runner headless) requiere Appium y Node.js instalados del lado
  Windows — el agente, operando solo dentro del workspace de WSL, no tenía forma de
  alcanzar ni instalar nada en el entorno Windows. Esa parte se resolvió con instalación
  manual en Windows; el agente quedó limitado a lo verificable desde WSL.
- **Cobertura asumida tras un reset de suite:** tras reconstruir la suite de
  `AutomationBackend`, el agente confirmaba haber creado tests nuevos cuando en realidad
  solo había sobrescrito unos que ya existían de ejemplo — asumía que todos los dominios
  (`users`, `auth`, `posts`) quedaron reconstruidos por igual. Se corrigió verificando
  dominio por dominio antes de reportar cobertura, en vez de confiar en la propia
  afirmación del agente.
- **Deriva en convención de nombres:** se forzó explícitamente la convención
  `test_<KEY_SIN_GUIONES>_<descripcion>` para mantener trazabilidad 1:1 con los TCs de
  AIO Tests, en vez de dejar que el agente propusiera nombres libres por archivo.
- **Auditorías de código incompletas:** al pedirle a la IA que audite una sección del
  sistema en busca de código muerto (carpetas duplicadas, scripts sin uso), detecta parte
  del problema pero no todo — quedan residuos que una sola pasada no marca. Se volvió
  práctica recurrente complementar con una revisión manual periódica en vez de tomar la
  auditoría de IA como fuente de verdad única.
- **Clasificación de Test Cases (component/smoke/regression):** el agente no distinguía
  con criterio fijo qué automatizar como `component` y qué promover a `smoke`/
  `regression`. Se corrigió documentando explícitamente la convención (`component` = todos
  los escenarios por endpoint; `smoke`/`regression` = reexportación por import del camino
  feliz/alternos, nunca reescritura) en `tests/docs/ARCHITECTURE.md`, para que dejara de
  decidirse caso por caso sin regla clara.

En ningún caso se aceptó código generado por la IA sin correrlo contra evidencia real
—dispositivo, emulador o llamada HTTP real—, el mismo criterio de calidad que exigen los
propios orquestadores del repo. Además, cada módulo, función, herramienta y clase que la
IA generó y que hoy forma parte del proyecto, tanto en backend como en frontend, se
revisó con detenimiento antes de darse por válida.

Para cerrar esta reflexión: la IA no solo agiliza la entrega, también es una excelente
vía de aprendizaje. Poder preguntar, pedir diagramas y que explique tecnologías,
herramientas y sistemas complejos facilita adquirir habilidades nuevas sobre la marcha.
Al mismo tiempo, un proyecto construido con agentes de IA exige ajustes constantes: a
medida que se trabaja con ellos hace falta definir reglas claras, crear hooks y
condiciones, e iterar sobre el pipeline hasta lograr resultados cada vez de mejor calidad
y en menos tiempo.