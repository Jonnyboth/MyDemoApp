---
plataforma: Android
app_o_pagina: "MyDemoApp"
modulo: "Catalogo"
submodulo: "Catalogo"
ticket_relacionado: "sin_ticket"
explorado_con: mobile-mcp
ultima_actualizacion: "2026-07-28"
---

# Catalogo / Catalogo — Android

## 1. Objetivo del módulo/pantalla
Pantalla inicial de la app (tras splash). Muestra el listado completo de productos
disponibles (10 productos: 4 variantes de mochila, camisetas, luz para bicicleta, onesie,
chaquetas polares), con precio, rating y controles de orden. Desde aquí se accede al detalle
de cada producto y al carrito.

## 2. Funciones principales
- **Listado de productos** (`productRV`, RecyclerView de 2 columnas): imagen, título, precio
  (`$ XX.XX`), rating en estrellas.
- **Ordenar** (ícono superior derecho, `sortIV`): despliega menú con 4 opciones — Name
  Ascending (default), Name Descending, Price Ascending, Price Descending.
- **Ver carrito** (ícono superior derecho, `cartRL`): navega a "My Cart"; muestra badge con
  cantidad de ítems.
- **Menú lateral** (☰, `menuIV`): navegación a todos los módulos de la app.
- Tap en un producto → navega al detalle (ver [Carrito.md](../../Carrito/Carrito/Carrito.md)
  para el flujo posterior de agregar al carrito).

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id | Notas |
|---|---|---|---|
| "Products" (título) | TextView | `id=productTV` | |
| Ícono ordenar | ImageView | `id=sortIV` | label: "Shows current sorting order and displays available sorting options" |
| Ícono carrito | ImageView/RelativeLayout | `id=cartIV` / `id=cartRL` | Badge `id=cartTV` con cantidad |
| Tarjeta de producto | ViewGroup | `id=productIV` (imagen), `id=titleTV`, `id=priceTV`, `id=rattingV` | Repetido por cada producto |
| Lista de productos | RecyclerView | `id=productRV` | label: "Displays all products of catalog" |

## 4. Flujos documentados

### Flujo 1 — Listado por defecto (Name Ascending)
Orden observado: Sauce Labs Backpack, Backpack (green), Backpack (orange), Backpack (red),
Bike Light, Bolt T-Shirt, Fleece Jacket (brown/gray/green/pink/red...), Onesie. Todos
correctos alfabéticamente. Sin hallazgos.

### Flujo 2 — Ordenar por "Price - Descending" (⚠️ BUG confirmado)
1. Tocar ícono ordenar (`sortIV`).
2. Seleccionar "Price - Descending".

**Resultado esperado**: productos ordenados de mayor a menor precio, de forma estrictamente
descendente en toda la lista.

**Resultado real (verbatim de precios observados en orden de aparición, tras reingresar a
"Catalog" desde el menú para forzar scroll al tope)**:

```
$ 49.99  Sauce Labs Fleece Jacket (red)
$ 49.99  Sauce Labs Fleece Jacket (pink)
$ 49.99  Sauce Labs Fleece Jacket (green)
$ 49.99  Sauce Labs Fleece Jacket (brown)
...
$ 15.99  Sauce Labs Bolt T-Shirt
$ 9.99   Sauce Labs Bike Light      ← rompe el orden descendente
$ 29.99  Sauce Labs Backpack (yellow) ← $29.99 aparece DESPUÉS de $9.99
$ 29.99  Sauce Labs Backpack (violet)
```

El salto `$15.99 → $9.99 → $29.99` no es descendente ni ascendente: los Backpacks ($29.99,
más caros que Bike Light) aparecen después de un producto más barato. Ver Sección 7.

**Re-verificación 2026-07-28 (⚠️ NO reproducido — ver Sección 7):** en dos corridas limpias
(app relanzada, `sortIV` → "Price - Descending" con coordenadas exactas de
`list_elements_on_screen`), el listado completo quedó estrictamente descendente de extremo a
extremo (`$49.99` Fleece Jacket ×6 → `$29.99` Backpack ×6 → `$15.99` T-Shirt ×10 → `$9.99`
Bike Light → `$7.99` Onesie), sin ninguna ruptura. No se reprodujo el salto documentado
arriba. Nota: en el primer intento de esta sesión, un tap con coordenadas mal escaladas
(tomadas de una captura de pantalla en vez de `list_elements_on_screen`) seleccionó por error
"Name - Descending" en lugar de "Price - Descending" — el menú "Sort by:" tiene filas
angostas (~115px de alto) y visualmente próximas entre sí, lo que hace plausible un mistap
similar por parte de un usuario real, aunque en esta sesión el origen confirmado del error fue
de la herramienta de automatización, no de la app. Se recomienda re-triage de SIM-11 con una
nueva corrida antes de asumir que el bug sigue vigente.

**Re-verificación 2026-07-28, sesión 3 (⚠️ NO reproducido — 3ª corrida limpia consecutiva):**
recorrido completo del listado (`mobile_swipe_on_screen`) tras aplicar "Price - Descending"
desde cero (app recién lanzada). Secuencia verificada extremo a extremo: `$49.99` Fleece
Jacket (red/pink/green/brown ×4 visibles, resto por color) → `$29.99` Backpack (negro, green,
orange, red, violet, yellow) → `$15.99` T-Shirt (Bolt + Test.allTheThings(), todas las
variantes) → `$9.99` Bike Light → `$7.99` Onesie. Estrictamente descendente, sin rupturas.
Evidencia: `evidence/sort_price_descending_2026-07-28_top.png` y
`evidence/sort_price_descending_2026-07-28_bottom.png`. Con 3 corridas limpias consecutivas
sin reproducir el salto, el hallazgo original de SIM-11 pierde soporte — soporta la hipótesis
de mistap de automatización/usuario en vez de defecto real de ordenamiento.

Adicionalmente (hallazgo UX menor): al aplicar cualquier criterio de orden, el `RecyclerView`
**no resetea el scroll al tope** — el usuario queda viendo una posición intermedia de la
lista reordenada, lo que visualmente parece "desordenado" aunque en la porción visible el
sub-orden sea correcto. Solo se aprecia el orden real navegando manualmente al inicio de la
lista (p. ej. reingresando por el menú "Catalog").

### Flujo 3 — Detalle de producto → color no refleja en imagen (⚠️ bug)
1. Tocar la imagen de "Sauce Labs Backpack" (negro) desde el listado.
2. En el detalle, tocar el swatch de color "Blue".

**Resultado esperado**: la imagen del producto cambia para reflejar el color seleccionado.
**Resultado real**: el indicador de selección (`aroundIV`) se mueve correctamente al swatch
azul, pero la imagen grande del producto (`productIV`) permanece mostrando el backpack negro
— no se actualiza visualmente. El dato sí se registra correctamente a nivel de negocio (el
carrito, tras "Add to cart", muestra el swatch "Color: 🔵" azul correcto — ver
[Carrito.md](../../Carrito/Carrito/Carrito.md)). Es decir: **el bug es puramente visual en la
imagen de previsualización del detalle**, no de datos.

## 5. Datos de prueba / valores de frontera observados
- Todos los productos: `$ 29.99` (Backpacks x4), `$ 15.99` (T-Shirts, varias), `$ 9.99` (Bike
  Light), `$ 49.99` (Fleece Jackets, varias), `$ 7.99` (Onesie).
- Colores disponibles en el selector del detalle de "Sauce Labs Backpack": Black, Blue, Gray,
  Green (4 swatches) — nótese que el catálogo lista variantes de Backpack independientes
  (green/orange/red) que no coinciden 1:1 con estos 4 swatches del selector de color del
  detalle (p. ej. no hay swatch "Orange" ni "Red" pese a existir "Backpack (orange)" y
  "Backpack (red)" como productos de catálogo separados).

## 6. Evidencia
- Screenshot: `evidence/catalogo_default.png` — listado por defecto (Name Ascending).
- Screenshot: `evidence/sort_price_descending_bug.png` — estado tras aplicar "Price - Descending" (nota: en la segunda repetición de la sesión el sort no se aplicó de forma reproducible por un problema de touch-targeting en el menú de opciones, ver Sección 7; la secuencia de precios documentada en el Flujo 2 fue verificada y registrada manualmente elemento por elemento vía `list_elements_on_screen` en la primera ejecución).
- Screenshot: `evidence/detalle_color_no_actualiza_imagen.png` — swatch "Blue" seleccionado, imagen sigue en negro.
- Screenshot: `evidence/sort_price_descending_2026-07-28_top.png` — tope del listado tras "Price - Descending" (Fleece Jacket $49.99), sesión 3 de re-verificación.
- Screenshot: `evidence/sort_price_descending_2026-07-28_bottom.png` — final del listado tras "Price - Descending" (Bike Light $9.99 → Onesie $7.99), sesión 3 de re-verificación.

## 7. Hallazgos abiertos / posibles bugs
- **[CRÍTICO — Crash confirmado]** Al tocar un producto del listado tras haber cambiado el
  criterio de orden (`sortIV`), la app puede **crashear** con
  `java.lang.ArrayIndexOutOfBoundsException: length=6; index=18` en
  `ProductCatalogFragment.lambda$setAdapter$0$ProductCatalogFragment` (`ProductCatalogFragment.java:156`),
  disparado desde `ProductsAdapter$1.onClick` (`ProductsAdapter.java:57`). Confirmado en
  `adb logcat -b crash` (proceso `com.saucelabs.mydemoapp.android`, ver stacktrace completo
  abajo). Hipótesis: el `OnClickListener` del adapter usa la **posición de la vista dentro del
  RecyclerView** (que puede incluir posiciones más allá de los datos reales tras un
  reordenamiento/scroll) en vez de la posición real dentro del array de productos (longitud 6
  en el momento del crash), causando el índice fuera de rango. Sugerido para
  [bug_report.md](../../../../../.prompts/skill_qa_engineer/bug_report.md) — prioridad alta,
  saca al usuario de la app (vuelve a Home) sin mensaje de error visible.

  ```
  java.lang.ArrayIndexOutOfBoundsException: length=6; index=18
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment.lambda$setAdapter$0$ProductCatalogFragment(ProductCatalogFragment.java:156)
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment$$ExternalSyntheticLambda0.OnClick(Unknown Source:4)
      at com.saucelabs.mydemoapp.android.view.adapters.ProductsAdapter$1.onClick(ProductsAdapter.java:57)
      at android.view.View.performClick(View.java:7659)
  ```

  **Re-verificación 2026-07-28 (✅ CONFIRMADO, sigue vigente):** reproducido de nuevo tocando
  "Sauce Labs Onesie" tras ordenar por "Price - Descending" y hacer scroll al fondo del
  listado. Evidencia verbatim vía `adb logcat -b crash` (proceso
  `com.saucelabs.mydemoapp.android`, PID `19256`, 2026-07-28 19:51:15):
  ```
  java.lang.ArrayIndexOutOfBoundsException: length=6; index=23
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment.lambda$setAdapter$0$ProductCatalogFragment(ProductCatalogFragment.java:156)
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment$$ExternalSyntheticLambda0.OnClick(Unknown Source:4)
      at com.saucelabs.mydemoapp.android.view.adapters.ProductsAdapter$1.onClick(ProductsAdapter.java:57)
      at android.view.View.performClick(View.java:7659)
  ```
  Mismo punto de falla exacto (`ProductCatalogFragment.java:156` / `ProductsAdapter.java:57`),
  solo cambia el índice fuera de rango (18 → 23), consistente con la hipótesis original de que
  el `OnClickListener` usa la posición de la vista en el `RecyclerView` en vez de la posición
  real en el array de productos.

  **Hallazgo adicional 2026-07-28 (nuevo síntoma, mismo bug):** tras el crash, la app **no
  vuelve limpiamente al catálogo**. La captura tomada inmediatamente después del crash mostró
  la pantalla "My Cart" vacía ("No Items") en lugar de Home/Catalog — y un relanzamiento
  posterior de la app (`mobile_launch_app`, proceso nuevo) también abrió directamente en "My
  Cart" en vez de en el catálogo (pantalla de entrada esperada tras el splash). Esto sugiere
  que el estado de navegación queda corrupto/persistido apuntando al fragmento equivocado, no
  solo que la app "vuelve a Home" como se documentó originalmente — el impacto en UX es mayor:
  el usuario pierde el punto de entrada normal al catálogo hasta navegar manualmente por el
  menú lateral.

  **Re-verificación 2026-07-28, sesión 3 (✅ CRASH CONFIRMADO por 3ª vez; ⚠️ síntoma de
  navegación NO reproducido esta vez):** tras ordenar por "Price - Descending" y hacer scroll
  hasta el final del listado, tap en "Sauce Labs Onesie" → la app crasheó de inmediato,
  saliendo al Home de Android (fuera de la app por completo, no solo a otra pantalla interna).
  Evidencia verbatim vía `adb logcat -b crash` (proceso `com.saucelabs.mydemoapp.android`, PID
  `22393`, 2026-07-28 20:51:53):
  ```
  java.lang.ArrayIndexOutOfBoundsException: length=6; index=23
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment.lambda$setAdapter$0$ProductCatalogFragment(ProductCatalogFragment.java:156)
      at com.saucelabs.mydemoapp.android.view.fragments.ProductCatalogFragment$$ExternalSyntheticLambda0.OnClick(Unknown Source:4)
      at com.saucelabs.mydemoapp.android.view.adapters.ProductsAdapter$1.onClick(ProductsAdapter.java:57)
      at android.view.View.performClick(View.java:7659)
  ```
  Mismo punto de falla exacto y mismo índice fuera de rango (`index=23`) que la re-verificación
  anterior — el crash es determinístico para este mismo estado de scroll/orden, no un evento
  aleatorio. A diferencia de la sesión anterior, esta vez `mobile_launch_app` sí devolvió a la
  app directamente al catálogo (Name Ascending) tras el crash — el síntoma de "aterrizar en My
  Cart" no se reprodujo en este intento, por lo que se documenta como **inconsistente** (puede
  depender del estado previo de navegación antes del crash) y no como parte fija del defecto.
  El crash en sí (`ArrayIndexOutOfBoundsException`) se mantiene **100% reproducible** en las 3
  sesiones de hoy.

- **[Alto — requiere re-triage, ver re-verificación en Flujo 2]** Orden "Price - Descending":
  el hallazgo original (salto no descendente) **no se reprodujo** en dos corridas limpias el
  2026-07-28. Antes de mantener SIM-11 abierto tal cual, re-ejecutar la validación (posible
  falso positivo por mistap en el menú de orden durante la sesión original).
- **[Menor/UX]** El scroll no se resetea al tope al cambiar el criterio de orden.
- **[Menor/UI]** El selector de color en el detalle de producto no actualiza la imagen de
  previsualización (aunque sí registra el dato correctamente en el carrito).

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| 2026-07-25 | Sesión QA exploratoria (emulador qa_android:5554) | Creación inicial |
| 2026-07-28 | Sesión QA exploratoria — orquestador Scrum Master AI, previa a redacción de HU (emulador `emulator-5554`, mobile-mcp) | Re-verificación del componente de ordenamiento (Flujo 2): crash SIM-9 confirmado vigente con evidencia logcat fresca (`index=23`) + nuevo síntoma de aterrizaje en "My Cart" tras el crash; bug SIM-11 (Price-Descending no estrictamente descendente) **no reproducido** en 2 corridas limpias — marcado para re-triage. |
| 2026-07-28 | Sesión QA exploratoria 3 — previa a creación de HU "Ordenamiento del catálogo" en backlog SIM (emulador `emulator-5554`, mobile-mcp) | 3ª corrida limpia: SIM-11 (orden Price-Descending no estrictamente descendente) **sigue sin reproducirse** de extremo a extremo. Crash SIM-9 (`ArrayIndexOutOfBoundsException` al tocar producto tras ordenar) **reproducido por 3ª vez consecutiva**, mismo punto de falla exacto — confirmado 100% reproducible; síntoma secundario de aterrizar en "My Cart" tras relanzar **no** se repitió esta vez (marcado inconsistente). Insumo para la HU de ordenamiento del catálogo creada en Jira SIM. |
