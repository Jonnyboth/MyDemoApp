---
plataforma: Android
app_o_pagina: "MyDemoApp"
modulo: "Menu"
submodulo: "Menu"
ticket_relacionado: "sin_ticket"
explorado_con: mobile-mcp
ultima_actualizacion: "2026-07-25"
---

# Menu / Menu — Android

## 1. Objetivo del módulo/pantalla
Menú lateral (drawer) accesible desde el ícono ☰ en el header de cualquier pantalla
principal. Es el punto central de navegación de la app y agrupa módulos de demostración de
funcionalidades nativas (cámara, geolocalización, WebView, dibujo, huella digital, etc.)
además del catálogo y la sesión de usuario.

## 2. Funciones principales
Ítems del menú, en orden: **Catalog, WebView, QR Code Scanner, Geo Location, Drawing** (separador)
**About, Reset App State, FingerPrint, Virtual USB, Crash app (debug)** (separador)
**Log In / Log Out** (según estado de sesión).

## 3. Componentes identificados
| Nombre visible | Tipo | Selector / accessibility id | Notas |
|---|---|---|---|
| Ítem de menú | TextView | `id=itemTV` | Repetido por cada entrada; texto = nombre del módulo |
| Lista del menú | RecyclerView | `id=menuRV` | label: "Recycler view for menu" |
| Drawer completo | ViewGroup | `id=drawerMenu` | |

## 4. Flujos documentados

### Flujo 1 — About
Pantalla estática: logo, versión **"V.2.2.0-build 25"**, logo repetido, link **"Go to the
Sauce Labs website."**. Sin campos ni interacción más allá del link externo (no se navegó el
link en esta sesión). Sin hallazgos.

### Flujo 2 — Geo Location
1. Abrir "Geo Location" → solicita permiso de ubicación del sistema (diálogo nativo Android
   con opciones Precise/Approximate + While using the app/Only this time/Don't allow).
2. Conceder "While using the app".

Resultado: pantalla muestra **Latitude: 37.4219983** / **Longitude: -122.084** en vivo, con
botones "Start Observing"/"Stop Observing" (Stop Observing activo por defecto tras iniciar).
Texto explicativo indica que deja de observar la ubicación al salir de la pantalla. Sin
hallazgos — funcionalidad correcta.

### Flujo 3 — WebView
Formulario simple: campo "URL" (placeholder `https://www.website.com`) + texto ayuda "Enter
an HTTPS url" + botón "Go To Site".
- Al tocar "Go To Site" con el campo vacío: no navega y no se observa ningún mensaje de error
  visual nuevo (el texto de ayuda ya estaba presente antes del intento). Ver hallazgo Sección 7.
- No se completó la prueba con una URL válida en esta sesión (interrumpida por navegación
  hacia atrás accidental).

### Flujo 4 — QR Code Scanner
Solicita permiso de cámara del sistema ("Allow My Demo App to take pictures and record
video?"). Al conceder "While using the app", el diálogo de permiso volvió a aparecer en un
segundo intento sin abrir la vista de cámara — posible limitación del entorno del emulador
(sin cámara física/virtual configurada) más que un defecto de la app; no se pudo confirmar la
vista de escaneo en sí en esta sesión.

### Flujo 5 — Drawing
Pantalla con canvas cuadriculado (`signature_pad`) + botones "Clear"/"Save". Solicita permiso
de acceso a fotos/videos/música al entrar. Un gesto de swipe sobre el canvas no dejó trazo
visible — no concluyente si es limitación del gesto simulado (swipe recto sin fricción) o un
problema real de la superficie de dibujo; sugerido reprobar con un gesto de trazo más lento/
manual antes de reportarlo como bug.

### Flujo 6 — Sesión (Log In / Log Out)
Ver detalle completo en [Login.md](../../Autenticacion/Login/Login.md). El logout muestra un
diálogo de confirmación nativo (título "Log Out", mensaje "Are you sure you want to logout",
botones CANCEL/LOGOUT) antes de cerrar sesión efectivamente.

### Módulos no explorados en profundidad en esta sesión
**Reset App State**, **FingerPrint**, **Virtual USB**, **Crash app (debug)** — se confirmó su
presencia en el menú pero no se ejecutaron (quedan pendientes para una siguiente sesión
exploratoria; "Crash app (debug)" en particular debe probarse con cautela ya que su nombre
sugiere que fuerza un crash intencional de la app, útil para pruebas de reporte de crashes
pero no se activó en esta sesión para no interferir con el crash real ya detectado en
[Catalogo.md](../../Catalogo/Catalogo/Catalogo.md)).

## 5. Datos de prueba / valores de frontera observados
- Coordenadas de geolocalización observadas: `37.4219983, -122.084` (ubicación por defecto
  del emulador — Mountain View, CA, sede de Google/zona por defecto de AVD).
- Versión de la app: `V.2.2.0-build 25`.

## 6. Evidencia
No se guardaron capturas dedicadas de este módulo en esta sesión (ver capturas embebidas en
la narrativa de Login.md y Catalogo.md para los flujos de sesión relacionados).

## 7. Hallazgos abiertos / posibles bugs
- **[Menor/UX]** En WebView, enviar el formulario con el campo URL vacío no produce ningún
  feedback visual de error (el texto "Enter an HTTPS url" es una ayuda estática, no un
  mensaje de validación dinámico) — el usuario no recibe confirmación de que la acción fue
  bloqueada. Sugerido para bug_report con severidad baja (UX), pendiente de confirmación del
  usuario.
- **[Informativo]** QR Code Scanner no llegó a mostrar la vista de cámara tras conceder el
  permiso en esta sesión — recomendable reprobar en un emulador con cámara virtual habilitada
  antes de concluir si es un bug real o una limitación de entorno.
- **[Informativo]** El gesto de dibujo en "Drawing" no dejó trazo visible con un swipe simple
  — recomendable reprobar con gestos multi-punto (`appium_gesture` con puntos intermedios) en
  vez de un swipe recto antes de concluir.

## 8. Historial de cambios
| Fecha | Sesión/Autor | Cambio |
|---|---|---|
| 2026-07-25 | Sesión QA exploratoria (emulador qa_android:5554) | Creación inicial |
