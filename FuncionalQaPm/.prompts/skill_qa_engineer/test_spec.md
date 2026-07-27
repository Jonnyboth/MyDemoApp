# Estructura JSON de un Caso de Prueba (Test Case) — AIO Tests

Este documento define el "contrato" de entrada que usan tanto `aio_tests_client.py`
(CLI) como `aio_tests_api.py` (FastAPI) para crear/actualizar Casos de Prueba.

## Esquema — creación (`create`)

```json
{
  "title": "string (obligatorio) — título corto y accionable del caso",
  "description": "string — objetivo/qué valida este caso",
  "precondition": "string — estado previo requerido del sistema/datos",
  "priority": "High | Medium | Low",
  "labels": ["string", "..."],
  "steps": [
    {
      "step": "string (obligatorio) — acción a ejecutar",
      "test_data": "string — datos usados en el paso (puede ir vacío)",
      "expected_result": "string (obligatorio) — resultado esperado, verificable"
    }
  ]
}
```

Campos obligatorios: `title` y al menos un elemento en `steps` (cada paso requiere `step` y `expected_result`).

> **Nota sobre `labels`**: los ejemplos de este documento muestran etiquetas temáticas
> (`"login"`, `"smoke"`, etc.) para ilustrar el campo, pero la convención **obligatoria**
> vigente exige incluir siempre `"created_by_ai"` más el ticket relacionado (o `"sin_ticket"`
> si no aplica) — ver Regla 8 de [references/formatting-rules.md](references/formatting-rules.md).
> Ejemplo correcto: `"labels": ["created_by_ai", "TP-118", "login", "smoke"]`.

## Esquema — actualización (`update`)

Acepta cualquier subconjunto de los campos anteriores. Solo se envían los campos que cambian:

```json
{
  "description": "Nueva descripción del caso",
  "priority": "Low"
}
```

Para reemplazar los pasos completos, se envía la lista `steps` completa (la API de AIO Tests reemplaza la colección, no hace merge por índice):

```json
{
  "steps": [
    {"step": "Nuevo paso 1", "test_data": "", "expected_result": "Nuevo resultado esperado 1"},
    {"step": "Nuevo paso 2", "test_data": "", "expected_result": "Nuevo resultado esperado 2"}
  ]
}
```

## Ejemplo completo — Login exitoso

```json
{
  "title": "Verificar login exitoso con credenciales válidas",
  "description": "Valida que un usuario registrado y activo pueda iniciar sesión correctamente.",
  "precondition": "El usuario 'qa_user@ejemplo.com' existe previamente en base de datos y está activo.",
  "priority": "High",
  "labels": ["login", "smoke"],
  "steps": [
    {
      "step": "Navegar a la pantalla de login",
      "test_data": "URL: https://tuapp.ejemplo.com/login",
      "expected_result": "Se muestra el formulario de login con campos usuario y contraseña"
    },
    {
      "step": "Ingresar usuario y contraseña válidos y presionar 'Ingresar'",
      "test_data": "usuario: qa_user@ejemplo.com / password: Test1234!",
      "expected_result": "El sistema redirige al dashboard principal del usuario autenticado"
    }
  ]
}
```

## Ejemplo completo — Login fallido (credenciales inválidas)

```json
{
  "title": "Verificar bloqueo de login con contraseña incorrecta",
  "description": "Valida que el sistema rechace el acceso cuando la contraseña no coincide con el usuario.",
  "precondition": "El usuario 'qa_user@ejemplo.com' existe previamente en base de datos y está activo.",
  "priority": "High",
  "labels": ["login", "negativo"],
  "steps": [
    {
      "step": "Navegar a la pantalla de login",
      "test_data": "URL: https://tuapp.ejemplo.com/login",
      "expected_result": "Se muestra el formulario de login con campos usuario y contraseña"
    },
    {
      "step": "Ingresar usuario válido con contraseña incorrecta y presionar 'Ingresar'",
      "test_data": "usuario: qa_user@ejemplo.com / password: incorrecta",
      "expected_result": "Se muestra el mensaje de error 'Usuario o contraseña incorrectos' y el usuario permanece en la pantalla de login"
    }
  ]
}
```

## Ejemplo — actualizar solo el estado de un caso (mantenimiento)

```json
{
  "priority": "Medium",
  "labels": ["login", "smoke", "regresion"]
}
```

Guardar este JSON en un archivo (ej. `caso.json`) y usarlo con:
```bash
python3 .prompts/skill_qa_engineer/aio_tests_client.py create --json-file caso.json
```
o enviarlo directamente al endpoint `POST /test-cases` del servicio FastAPI (ver [tools.md](tools.md)).

## Nota sobre el mapeo interno al esquema real de AIO Tests

El JSON de arriba es el contrato "amigable" que recibe este script. Internamente,
`aio_tests_client.py` lo traduce al esquema real `CaseFullDetails` de la API de
AIO Tests antes de enviarlo:

| Campo amigable (este documento) | Campo real en AIO Tests |
|---|---|
| `priority: "High"` | `priority: {"name": "High"}` (debe coincidir con una prioridad ya configurada en el proyecto) |
| `labels: ["login"]` | `tags: [{"tag": {"ID": ..., "name": "login"}}]` (ver nota de tags abajo) |
| `steps[].test_data` | `steps[].data` |
| `steps[].expected_result` | `steps[].expectedResult` |
| (implícito, si hay `steps`) | `scriptType: {"name": "Classic"}` — obligatorio cuando se envían pasos "TEXT"; sin esto la API responde 400 "Invalid or missing value for Test Script Type" |

No necesitas escribir el JSON en el formato real: basta con seguir la estructura
amigable documentada arriba. Si el valor de `priority` no coincide exactamente
(mismo texto) con una prioridad configurada en el proyecto Jira, AIO Tests
puede rechazar la solicitud (HTTP 400) o ignorarla; verifica los nombres
válidos con `GET /api/v1/project/{jiraProjectId}/config/testcase/priority`.

### Tags/`labels`: resolución automática vía `/tag`

AIO Tests no permite crear un tag "al vuelo" dentro del payload del Caso de
Prueba: primero hay que resolver cada nombre de `labels` a un tag real
(existente o recién creado) llamando a `POST /api/v1/project/{jiraProjectId}/tag`
(operación "create-or-get"), y recién con el `ID` devuelto se arma
`tags: [{"tag": {"ID": ..., "name": ...}}]`. `aio_tests_client.py` hace esto
automáticamente (función `_resolve_tags`) cada vez que `create_test_case` o
`update_test_case` reciben `labels` — no requiere ningún paso manual adicional.

### Actualizar (`update`): no es un PATCH parcial real

La API de AIO Tests no soporta actualización parcial verdadera: un `PUT`
exitoso requiere reenviar el Caso completo, incluyendo `ID` y `version` para
control de concurrencia optimista (así lo indica su propia documentación:
"Fetch the existing case details ... Update the required parameters and call
this API"). Por eso `update_test_case()` primero hace `GET` del caso actual,
aplica los campos de `updates` encima, y recién entonces hace el `PUT` con el
objeto resultante completo. Desde el punto de vista de quien usa el script,
`updates` sigue pudiendo ser parcial (ej. solo `{"priority": "Low"}`); la
fusión con el resto del caso ocurre de forma transparente.
