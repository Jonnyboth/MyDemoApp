# Catálogo de test cases y utilidades reutilizables

> Extraído sin cambios de lógica desde `BMO-FlowPlanner.agent.md` — Fase 1.
> Actualizar esta tabla cuando se agreguen nuevos setUp test cases o utilidades comunes.

| Test Case / Utilidad | Ruta | Punto de llegada / Responsabilidad | Cuándo reutilizarlo |
|----------------------|------|------------------------------------|----------------------|
| `openApp` | `Scripts/android/openApp/` | Home de TuEmpresa cargado y verificado, anti-crash activo | El flujo comienza desde Home de la app |
| `OpenStoreGeant` | `Scripts/android/OpenStoreGeant/` | Home de la tienda Geant cargado y validado | El flujo comienza dentro de Geant (búsqueda, producto, carrito) |
| `UtilsPage` | `Keywords/com/tuempresa/page/common/UtilsPage.groovy` | Scroll adaptativo + validación masiva de elementos | Siempre que un Page class necesite scroll o validar múltiples elementos. No reimplementar. |
| `DeviceResolutionPage` | `Keywords/com/tuempresa/page/common/DeviceResolutionPage.groovy` | Caché de resolución + escalado de coordenadas | Siempre que un Page class use `tapAtPosition` o swipes con coordenadas base |

> ⚠️ Nota post-Fase 1 del refactor: `Test Cases/`, `Keywords/`, `Scripts/` y
> `Object Repository/` fueron vaciados como baseline limpio. Esta tabla describe la
> convención a seguir cuando se recreen `openApp`/`OpenStoreGeant` — no asumir que
> existen físicamente hasta que `qa-test-creator` los genere de nuevo.

## Regla de decisión — punto de entrada óptimo

```
1. ¿El flujo empieza desde cero (app cerrada)?
   → Usar openApp como setUp base.

2. ¿El flujo empieza en Home de TuEmpresa?
   → Reutilizar openApp como setUp. No reescribir la navegación de apertura.

3. ¿El flujo empieza dentro de Geant (búsqueda, detalle producto, carrito)?
   → Reutilizar OpenStoreGeant como setUp.

4. ¿El flujo empieza en otra sección no cubierta?
   → Indicar que se necesita un nuevo setUp. Proponerlo en el plan para que
     qa-test-creator lo cree.
```

Si se reutiliza un test case existente como setUp, incluirlo explícitamente en el plan:
```
setUp: CustomKeywords.'...' o llamada al test case X
Motivo: evitar reescribir pasos ya automatizados y estabilizados
```

## Equivalente Web (nuevo, Fase 2 del refactor)

El runner ahora soporta Selenium (`Test Cases/web/...`). Cuando el flujo a planificar sea
Web, aplicar la misma lógica de reuso: antes de proponer un nuevo setUp de login/navegación
inicial, verificar si ya existe un Script en `Scripts/web/<setUp>/` que cubra ese punto de
entrada. Si no existe ninguno todavía, proponerlo en el plan igual que se haría para Android.
