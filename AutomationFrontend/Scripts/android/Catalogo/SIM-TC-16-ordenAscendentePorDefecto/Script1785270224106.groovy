import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

// Ticket: SIM-17 -- Ordenar el catalogo de productos por nombre o precio
// Objetivo: valida que el catalogo "Products" se muestre ordenado alfabeticamente
//           de forma ascendente (Name - Ascending) por defecto, sin que el cliente
//           aplique ninguna accion de ordenamiento
// Datos: N/A (caso de solo lectura/observacion)

// ─── PRECONDICION ─────────────────────────────────────────────
Mobile.comment('SIM-TC-16: Verificar orden alfabetico ascendente por defecto al abrir el catalogo')
Mobile.comment('PRECONDICION: reiniciar la app (pm clear + reabrir) para partir de un estado base limpio, sin ningun criterio de orden aplicado')
CustomKeywords.'com.MyDemoApp.steps.android.AppLifecycleSteps.restartApp'()

// ─── PASOS DEL TEST ───────────────────────────────────────────
Mobile.comment('STEP 1: Confirmar que el catalogo Products esta visible tras el splash, sin requerir login')
CustomKeywords.'com.MyDemoApp.steps.android.ProductsSteps.ensureOnProductsScreen'()

Mobile.comment('ASSERT 1: El listado de productos esta ordenado alfabeticamente de la A a la Z (Name - Ascending, criterio por defecto)')
CustomKeywords.'com.MyDemoApp.steps.android.ProductsSteps.assertDefaultSortIsNameAscending'()
