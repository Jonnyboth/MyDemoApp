# Backlog

Estado del backlog reflejado desde Jira (proyecto de ejemplo, clave `TP`). Este archivo se
actualiza a medida que `skill_project_manager` inserta o refina Historias de Usuario, y
`skill_qa_engineer` aprueba su verificación. El contenido de abajo es un **ejemplo ilustrativo**
del formato esperado — sustitúyelo por el backlog real de tu propio proyecto Jira.

## Épica de ejemplo: Autenticación de usuarios

| Issue | Título | Tipo | Estado |
|---|---|---|---|
| TP-14 | Permitir inicio de sesión con email y contraseña | Historia | Tareas por hacer (Backlog) |
| TP-15 | Permitir registro de nuevos usuarios con email | Historia | Tareas por hacer (Backlog) |
| TP-16 | Permitir recuperación de contraseña vía correo | Historia | Tareas por hacer (Backlog) |

Cada HU incluye: narrativa Como/Quiero/Para, Contexto/Notas, Sugerencias de UX/UI orientativas
(redactadas por `skill_project_manager`, sin prototipos formales — ver nota de bloqueo abajo) y
Criterios de Aceptación en formato Gherkin/BDD.

### ⚠️ Nota de orquestación
La Fase de Diseño formal (prototipos estructurales en Markdown/HTML/CSS, breakpoints
responsivos) corresponde a `skill_ui_ux_designer`, referenciada en `super_agent_Qa_PM.md` pero cuya
carpeta aún no existe en `.prompts/`. Las HUs anteriores incluyen únicamente sugerencias de
UX/UI de alto nivel como parte de la especificación funcional (dentro del alcance de
`skill_project_manager`), no un prototipo validado. Antes de iniciar la Fase de Construcción de
estas HUs, se requiere definir `skill_ui_ux_designer` o indicar cómo proceder.
