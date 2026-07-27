---
plataforma: Android
app_o_pagina: "MyDemoApp"
modulo: "Checkout"
submodulo: "Checkout"
ticket_relacionado: "sin_ticket"
explorado_con: mobile-mcp
ultima_actualizacion: "2026-07-25"
---

# Checkout / Checkout — Android

## 1. Objetivo del módulo/pantalla
Formulario de dirección de envío, paso previo a "To Payment". Requiere sesión iniciada
(si no hay sesión, "Proceed To Checkout" redirige a Login antes de mostrar el formulario).

## 2. Funciones principales
- Formulario con campos: Full Name*, Address Line 1*, Address Line 2 (opcional), City*,
  State/Region (opcional), Zip Code*, Country* (`*` = obligatorio, validado al enviar).
- Botón "To Payment" (`paymentBtn`): valida campos obligatorios antes de continuar.
- Navegación entre campos vía teclado ("Next"/Tab del IME).

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id | Notas |
|---|---|---|---|
| Full Name | EditText | `id=fullNameET` | |
| Address Line 1 | EditText | `id=address1ET` | |
| Address Line 2 | EditText | `id=address2ET` | Único campo opcional de dirección |
| City | EditText | `id=cityET` | Ver hallazgo crítico Sección 7 |
| State/Region | EditText | `id=stateET` | Opcional |
| Zip Code | EditText | `id=zipET` | Ver hallazgo crítico Sección 7 |
| Country | EditText | `id=countryET` | |
| "To Payment" | Button | `id=paymentBtn` | label: "Saves user info for checkout" |

## 4. Flujos documentados

### Flujo 0 — Acceso sin sesión / con sesión
"Proceed To Checkout" desde el carrito, sin sesión iniciada, redirige a la pantalla de Login
antes de mostrar el formulario de Checkout. Tras loguear, continúa automáticamente al
formulario de Checkout (no hay que repetir la acción "Proceed To Checkout").

### Flujo 1 — Los valores visibles en los campos son solo placeholders (⚠️ posible confusión de UX)
Al entrar a Checkout, todos los campos muestran texto en gris que **parece** información
precargada: `Rebecca Winter`, `Mandorley 112`, `Entrance 1`, `Truro`, `Cornwall`, `89750`,
`United Kingdom`. Un usuario podría asumir (como ocurrió durante esta exploración) que son
valores reales ya cargados y tocar directamente "To Payment".

**Resultado real**: son *hints* (placeholder), no valores del campo. Al tocar "To Payment" sin
escribir nada, **todos los campos obligatorios muestran error de validación** (borde rojo +
ícono + mensaje), confirmando que el formulario estaba vacío:
- Full Name: **"Please provide your full name."**
- Address Line 1: **"Please provide your address."**
- City: **"Please provide your city."**
- Zip Code: **"Please provide your zip"**
- Country: **"Please provide your"** (mensaje truncado visualmente, verbatim tal como se
  muestra en pantalla — ver evidencia).

Ver Sección 7 para valoración de severidad.

### Flujo 2 — Bug crítico: el campo "City" intercepta los toques destinados a "Zip Code" / "Country"
Al intentar completar el formulario con datos reales:
1. Full Name y Address Line 1 se pudieron editar con éxito tocando directamente sobre el
   campo (usando coordenadas reales confirmadas vía `list_elements_on_screen`).
2. Al intentar tocar el campo **City** para editarlo, el texto terminaba **concatenado en
   Address Line 1** (el campo anterior).
3. Al intentar tocar **Zip Code** o **Country** (situados debajo de City), el foco/teclado
   **siempre vuelve a activarse sobre City**, sin importar el método de interacción probado:
   - Tap simple → foco permanece/regresa a City.
   - Doble tap → foco permanece en City.
   - Long-press (mantiene 500ms) → abre el menú contextual "Paste / Select all" **sobre el
     campo City**, no sobre Zip Code (evidencia: `bug_zip_code_touch_target_intercepta_city.png`).
   - Navegación por teclado (`KEYCODE_TAB` / botón "Next" del IME) → el orden de tabulación
     es `Address Line 2 → City → State/Region → Country`, **saltándose Zip Code por
     completo** — el campo nunca recibe foco por esta vía tampoco.
   - `KEYCODE_DPAD_DOWN` → tampoco mueve el foco a Zip Code.
   - Intento de aislar el problema con una sesión Appium independiente (`appium_set_value`)
     para editar el elemento directamente: no se pudo establecer una segunda sesión de
     automatización en paralelo sobre el mismo dispositivo (conflicto de puerto ADB con la
     sesión de `mobile-mcp` ya activa) — no se llegó a una conclusión vía esa ruta.

**Resultado**: **el campo obligatorio "Zip Code" es efectivamente inalcanzable mediante
interacción táctil normal ni navegación de teclado**, lo que bloquea la validación
obligatoria de ese campo y por lo tanto **impide completar el checkout** en las condiciones
probadas (emulador Android 14, densidad de pantalla 1080×2400).

## 5. Datos de prueba / valores de frontera observados
- Full Name: `QA Tester` — aceptado.
- Address Line 1: `Calle Falsa 123` — aceptado.
- City: `Bogota` — aceptado (alcanzable solo vía `KEYCODE_TAB`, no por touch).
- State/Region: dejado vacío (opcional, sin asterisco) — no bloquea el envío.
- Country: `Colombia` — aceptado (alcanzable vía `KEYCODE_TAB` desde State/Region).
- Zip Code: no se logró editar en esta sesión por el bug de Sección 7; quedó con el
  placeholder original `89750` (nunca aceptado como valor real, la validación lo marca en
  rojo persistentemente).

## 6. Evidencia
- Screenshot: `evidence/checkout_placeholders_parecen_prellenados.png` — formulario recién
  abierto, todos los valores en gris (placeholders).
- Screenshot: `evidence/checkout_validacion_campos_vacios.png` — errores de validación tras
  tocar "To Payment" sin llenar campos.
- Screenshot: `evidence/bug_zip_code_touch_target_intercepta_city.png` — long-press sobre la
  posición visual de "Zip Code" abre el menú contextual "Paste/Select all" sobre el campo
  "City" en su lugar.

## 7. Hallazgos abiertos / posibles bugs
- **[CRÍTICO — bloquea el flujo de compra]** El campo "Zip Code" (obligatorio) no puede
  recibir foco mediante ningún método de interacción táctil o de teclado probado (tap, doble
  tap, long-press, Tab/Next del IME, D-pad down). El campo "City" parece tener un área táctil
  (touch target) sobredimensionada que intercepta toques destinados a los campos
  Zip Code/Country ubicados debajo. Adicionalmente, el orden de navegación por teclado
  (`imeOptions`/`nextFocusDown`) omite Zip Code por completo. **Impacto**: un usuario real no
  puede completar el checkout, ya que Zip Code es obligatorio y su validación nunca se
  satisface. Sugerido como bug de máxima prioridad para
  [bug_report.md](../../../../../.prompts/skill_qa_engineer/bug_report.md) — pendiente de
  confirmación del usuario antes de reportarlo formalmente en Jira.
- **[Medio/UX]** Los valores de ejemplo en los campos del formulario de envío se muestran con
  un estilo (gris, texto ya legible) que puede confundirse fácilmente con datos reales
  prellenados, en vez de placeholders. Sugerido evaluar un estilo de hint más distintivo o un
  texto de ejemplo con formato claramente distinto al de un valor real ingresado.
- **[Informativo]** No se pudo determinar si el bug de Zip Code es específico de esta
  resolución/densidad de emulador (1080×2400, Android 14) o se reproduce en otros
  dispositivos/tamaños — recomendable confirmar en un dispositivo físico o emulador de otra
  densidad antes de reportar como bug universal.

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| 2026-07-25 | Sesión QA exploratoria (emulador qa_android:5554) | Creación inicial |
