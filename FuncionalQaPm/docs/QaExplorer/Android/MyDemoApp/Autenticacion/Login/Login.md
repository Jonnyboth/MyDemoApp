---
plataforma: Android
app_o_pagina: "MyDemoApp"
modulo: "Autenticacion"
submodulo: "Login"
ticket_relacionado: "sin_ticket"
explorado_con: mobile-mcp
ultima_actualizacion: "2026-07-25"
---

# Autenticacion / Login — Android

## 1. Objetivo del módulo/pantalla
Permite a un usuario iniciar sesión con un usuario/contraseña de la lista de cuentas de prueba
provistas por la app (Sauce Labs My Demo App), o seleccionarlas por atajo desde la lista
"Usernames/Password" al pie de la pantalla. El login habilita el flujo de Checkout.

## 2. Funciones principales
- Login manual: escribir usuario y contraseña en los campos y presionar "Login".
- Autocompletado por atajo: tocar un usuario de la lista "Usernames" rellena usuario **y**
  contraseña automáticamente (excepto para `visual@example.com`, que no tiene contraseña
  visible en la lista).
- Logout: desde el menú lateral, ítem "Log Out", con diálogo de confirmación
  ("¿Are you sure you want to logout?" / CANCEL / LOGOUT).
- Validación de campos obligatorios antes de intentar autenticar.

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id | Notas |
|---|---|---|---|
| Campo usuario | EditText | `id=nameET` | Sin hint fijo, vacío por defecto |
| Campo contraseña | EditText | `id=passwordET` | Enmascarado (`••••••••`) tras autocompletar |
| Botón "Login" | Button | `id=loginBtn` | label: "Tap to login with given credentials" |
| Lista "Usernames" | TextView x3 | `id=username1TV/username2TV/username3TV` | `bod@example.com`, `alice@example.com (locked out)`, `visual@example.com` |
| Ítem menú "Log In"/"Log Out" | TextView | `id=itemTV` | Cambia de texto según estado de sesión |
| Diálogo de confirmación de logout | AlertDialog | `android:id/button1` (LOGOUT) / `button2` (CANCEL) | Aparece siempre al tocar "Log Out" |

## 4. Flujos documentados

### Flujo 1 — Login exitoso
1. Abrir menú lateral (☰) → "Log In".
2. Tocar `bod@example.com` en la lista de usuarios → autocompleta usuario y contraseña
   (`10203040`).
3. Tocar "Login".

Resultado esperado: regresa al catálogo de productos; el ítem del menú cambia a "Log Out".
Confirmado correcto.

### Flujo 2 — Usuario bloqueado (locked out)
1. Tocar `alice@example.com (locked out)` en la lista → autocompleta usuario (sin password
   visible en la lista, pero el campo password conserva el valor previamente cargado).
2. Tocar "Login".

Resultado esperado: **no** inicia sesión.
Mensaje de error (verbatim): **"Sorry this user has been locked out."**

### Flujo 3 — Validación de campo vacío
1. Sin llenar ningún campo, tocar "Login".

Resultado: se muestra borde rojo + ícono de alerta en el campo usuario.
Mensaje de error (verbatim): **"Username is required"**

### Flujo 4 — Logout
1. Con sesión iniciada, abrir menú lateral → "Log Out".
2. Se muestra diálogo: título **"Log Out"**, mensaje **"Are you sure you want to logout"**,
   botones **CANCEL** / **LOGOUT**.
3. Tocar LOGOUT.

Resultado esperado: cierra sesión, el ítem del menú vuelve a "Log In". Confirmado correcto.

## 5. Datos de prueba / valores de frontera observados
- `bod@example.com` / `10203040` → login válido.
- `alice@example.com` / `10203040` → cuenta bloqueada (mensaje de error, no navega).
- `visual@example.com` → sin contraseña visible en la lista de atajos; no se probó login
  manual con esta cuenta en esta sesión.
- La sesión **no persiste** si la app se relanza tras quedar en background/():
  se observó que tras `mobile_launch_app` sobre un proceso ya finalizado, el menú vuelve a
  mostrar "Log In" aunque un login previo hubiera sido exitoso en el mismo proceso. Dentro del
  mismo proceso en ejecución (navegando entre pantallas sin relanzar), la sesión sí se mantiene.

## 6. Evidencia
- Screenshot: `evidence/login_locked_out.png` — mensaje "Sorry this user has been locked out."

## 7. Hallazgos abiertos / posibles bugs
- **[Menor/UX]** La sesión no persiste entre relanzamientos del proceso de la app (aunque el
  usuario haya iniciado sesión correctamente antes). No se confirmó si es el comportamiento
  esperado (app de demo, sin backend real) o un defecto — sugerido documentarlo como pregunta
  de negocio antes de convertirlo en bug vía [bug_report.md](../../../../../.prompts/skill_qa_engineer/bug_report.md).
- **[Menor]** La cuenta `visual@example.com` no muestra contraseña en la lista de atajos de
  login (a diferencia de `bod`), lo que puede confundir a un usuario que solo dispone de esa
  vía para conocer credenciales de prueba.

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| 2026-07-25 | Sesión QA exploratoria (emulador qa_android:5554) | Creación inicial |
