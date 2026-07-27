---
plataforma: Android
app_o_pagina: "MyDemoApp"
modulo: "Carrito"
submodulo: "Carrito"
ticket_relacionado: "sin_ticket"
explorado_con: mobile-mcp
ultima_actualizacion: "2026-07-25"
---

# Carrito / Carrito — Android

## 1. Objetivo del módulo/pantalla
Permite revisar los productos agregados desde el detalle de producto, ajustar cantidades,
eliminar ítems y proceder al checkout. Accesible desde el ícono de carrito en el header de
cualquier pantalla principal.

## 2. Funciones principales
- **Agregar al carrito** (desde detalle de producto): selector de color, selector de
  cantidad (+/-), botón "Add to cart".
- **Ver carrito**: lista de productos agregados con imagen, título, precio, rating, color y
  cantidad.
- **Ajustar cantidad** por ítem: botones `+`/`-` (`plusIV`/`minusIV`), actualiza total en
  tiempo real.
- **Eliminar ítem**: botón "Remove Item" (`removeBt`), o decrementar cantidad hasta 0.
- **Proceder al checkout**: botón "Proceed To Checkout" (`cartBt`).
- **Estado vacío**: mensaje + botón "Go Shopping" cuando no hay ítems.

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id | Notas |
|---|---|---|---|
| "Add to cart" (detalle producto) | Button | `id=cartBt` (en detalle) | label: "Tap to add product to cart" |
| Selector cantidad (detalle) | ImageView x2 + TextView | `id=minusIV` / `id=noTV` / `id=plusIV` | |
| Selector color (detalle) | RecyclerView | `id=colorRV` con `id=colorIV` por swatch | Ver hallazgo en [Catalogo.md](../../Catalogo/Catalogo/Catalogo.md) |
| "My Cart" (título) | TextView | `id=productTV` | |
| Ítem de carrito | ViewGroup | `id=colorTitleTV` ("Color:") + `id=colorIV` | Aquí el color SÍ se muestra correcto |
| "Remove Item" | TextView/Button | `id=removeBt` | label: "Removes product from cart" |
| Total | TextView x2 | `id=itemsTV` ("N Items"), `id=totalPriceTV` | |
| "Proceed To Checkout" | Button | `id=cartBt` (en carrito) | label: "Confirms products for checkout" |
| Estado vacío | TextView + Button | `id=noItemTitleTV` ("No Items"), `id=shoppingBt` ("Go Shopping") | |

## 4. Flujos documentados

### Flujo 1 — Agregar producto con color y cantidad personalizados
1. Detalle de "Sauce Labs Backpack" → seleccionar color "Blue" → incrementar cantidad a 3
   (botón `+` x2) → "Add to cart".
2. Ir al carrito.

Resultado: badge del carrito muestra "3"; el ítem en el carrito muestra **"Color: 🔵"
(azul) correctamente** — confirma que el bug de imagen no actualizada (ver
[Catalogo.md](../../Catalogo/Catalogo/Catalogo.md) Sección 7) es solo visual en el detalle,
no un problema de datos. Total: `3 Items` / `$ 89.97` (3 × $29.99, correcto).

### Flujo 2 — Ajustar cantidad dentro del carrito
1. Con 3 unidades en el carrito, tocar `+` una vez.

Resultado: cantidad → 4, badge del header → "4", total → `$ 119.96` (correcto,
actualización en tiempo real).

2. Tocar `-` repetidamente hasta 1 unidad.

Resultado: cantidad → 1. Texto mostrado: **"1 Items"** (gramaticalmente debería ser singular
"1 Item"). Ver hallazgo Sección 7.

3. Tocar `-` una vez más (de 1 a 0).

Resultado esperado/observado: el ítem se **elimina automáticamente** del carrito y la
pantalla pasa al estado vacío ("No Items" / "Oh no! Your cart is empty. Fill it up with swag
to complete your purchase." / botón "Go Shopping"). Comportamiento correcto, sin hallazgos.

### Flujo 3 — Estado vacío → "Go Shopping"
1. Con el carrito vacío, tocar "Go Shopping".

Resultado: navega de vuelta al detalle del último producto visto / catálogo. Comportamiento
esperado.

## 5. Datos de prueba / valores de frontera observados
- Cantidad mínima permitida antes de auto-eliminar el ítem: 1 → 0 (elimina automáticamente,
  sin diálogo de confirmación).
- Totales verificados matemáticamente correctos en todos los casos probados (3×$29.99=$89.97,
  4×$29.99=$119.96, 1×$29.99=$29.99).

## 6. Evidencia
- Screenshot: `evidence/carrito_color_correcto.png` — ítem en carrito mostrando "Color: 🔵"
  correcto pese al bug visual del detalle.

## 7. Hallazgos abiertos / posibles bugs
- **[Menor/Contenido]** Texto "1 Items" no usa singular gramatical cuando la cantidad total
  es 1 (debería decir "1 Item"). Sugerido para
  [bug_report.md](../../../../../.prompts/skill_qa_engineer/bug_report.md) con severidad baja
  (cosmético/i18n).
- **[Informativo]** Eliminar el último ítem mediante el botón `-` no pide confirmación (a
  diferencia de "Remove Item", que tampoco pide confirmación). Comportamiento consistente,
  no se considera bug, pero podría evaluarse como mejora de UX (evitar eliminación accidental).

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| 2026-07-25 | Sesión QA exploratoria (emulador qa_android:5554) | Creación inicial |
