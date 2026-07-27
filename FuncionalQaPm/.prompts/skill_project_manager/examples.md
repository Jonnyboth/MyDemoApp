# Plantilla de invocación de `skill_project_manager`

Copia, pega y rellena los placeholders `[ ]` antes de enviar el prompt al agente.

```
Activa la skill skill_project_manager.

Quiero que redactes una Historia de Usuario de nivel Senior, con criterios de
aceptación en formato Gherkin (BDD), y la insertes directamente en el Backlog
de Jira usando el MCP de Atlassian configurado.

- Project Key en Jira: [PROJECT_KEY]
- Título / funcionalidad a describir: [TITULO_O_DESCRIPCION_DE_LA_FUNCIONALIDAD]
- Contexto de negocio (opcional pero recomendado): [CONTEXTO_ADICIONAL_REGLAS_DE_NEGOCIO_RESTRICCIONES]
- Rol/perfil de usuario para el "Como...": [ROL_DEL_USUARIO]
- Criterios de aceptación obligatorios que deben incluirse sí o sí: [CRITERIOS_DE_ACEPTACION_ESPECIFICOS_O_"NINGUNO_ADICIONAL"]

Antes de insertarla en Jira, muéstrame la HU redactada para mi validación.
```

## Ejemplo relleno

```
Activa la skill skill_project_manager.

Quiero que redactes una Historia de Usuario de nivel Senior, con criterios de
aceptación en formato Gherkin (BDD), y la insertes directamente en el Backlog
de Jira usando el MCP de Atlassian configurado.

- Project Key en Jira: TP
- Título / funcionalidad a describir: Recordatorio automático de vencimiento de un plan
- Contexto de negocio (opcional pero recomendado): El sistema ya envía notificaciones push;
  esta funcionalidad debe reutilizar ese servicio y no debe enviar recordatorios duplicados
  si el usuario ya renovó el plan.
- Rol/perfil de usuario para el "Como...": usuario registrado en la app
- Criterios de aceptación obligatorios que deben incluirse sí o sí:
  1. Si faltan 7 días para la fecha de vencimiento del plan, se envía una notificación push.
  2. Si el usuario ya renovó el plan, no se envía ningún recordatorio.

Antes de insertarla en Jira, muéstrame la HU redactada para mi validación.
```

## Variante: inserción directa sin revisión previa

Si no necesitas validar la redacción antes de insertarla en Jira, agrega al final:

```
No necesito revisar el borrador antes: redáctala e insértala directamente en el Backlog.
```
