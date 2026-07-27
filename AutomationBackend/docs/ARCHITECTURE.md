# Arquitectura del Módulo `AutomationBackend`

## 1. Qué es este módulo

`AutomationBackend` automatiza pruebas de **APIs REST** (sin UI: nada de Selenium/Appium/Playwright para este propósito, aunque el `.mcp.json` del repo trae otros servidores MCP compartidos con módulos hermanos). Tiene dos partes que trabajan juntas:

```
AutomationBackend/
├── tests/      → el framework de automatización en sí (pytest + requests)
└── .prompts/   → las Skills que diseñan, construyen, ejecutan y documentan esas pruebas
```

## 2. El framework de pruebas (`tests/`)

Framework en capas (`config` → `core` → `models`/`builders` → `services` → `tests`), con 4 niveles de prueba (`smoke`/`regression`/`component`/`e2e`) — `component/` es la fuente única de verdad (1 archivo por endpoint), `smoke/`/`regression/` reexportan por import sin duplicar lógica, `e2e/` tiene código propio para flujos encadenados (ver `tests/docs/ARCHITECTURE.md` §2.1) —, reporte HTML propio y trazabilidad completa (cURL + tiempos) de cada request. Detalle completo:

- [`tests/docs/ARCHITECTURE.md`](../tests/docs/ARCHITECTURE.md) — capas, patrones de diseño y flujo de una petición.
- [`tests/docs/MODULES.md`](../tests/docs/MODULES.md) — resumen de cada archivo/módulo.
- [`tests/README.md`](../tests/README.md) — instalación, ejecución y reporte.

## 3. Las Skills de automatización (`.prompts/`)

Un Súper Agente Orquestador (`.prompts/super_agent_automation_backend.md`, cargado automáticamente vía `.claudecode.md`) coordina el ciclo de vida de la automatización delegando en 3 Skills:

| Skill | Fase | Responsabilidad |
|---|---|---|
| [`skill_api_test_designer`](../.prompts/skill_api_test_designer/system.md) | Diseño | Matriz de escenarios de prueba clasificada por nivel, antes de escribir código. Toma como insumo un cURL directo, una historia de usuario, o un Test Case ya existente en AIO Tests. |
| [`skill_api_automation_developer`](../.prompts/skill_api_automation_developer/system.md) | Construcción | Implementa la matriz aprobada respetando la arquitectura en capas del framework. |
| [`skill_api_test_executor`](../.prompts/skill_api_test_executor/system.md) | Ejecución | Corre la suite real y aprueba/rechaza el criterio de aceptación (gate de calidad). |

Ver el protocolo completo de orquestación (orden de fases, reglas del workspace) en [`.prompts/super_agent_automation_backend.md`](../.prompts/super_agent_automation_backend.md).

## 4. Extender el framework con una Skill nueva

Para automatizar un dominio/endpoint que no existe hoy, no se edita código a mano fuera de este flujo: se activa `skill_api_test_designer` → se valida la matriz → se activa `skill_api_automation_developer` → se activa `skill_api_test_executor` como gate. El detalle capa por capa (qué archivo tocar y en qué orden) está en [`skill_api_automation_developer/references/layer-templates.md`](../.prompts/skill_api_automation_developer/references/layer-templates.md).
