# Reglas duras de locators — R-K1, R-K2, R-K3 (no negociables)

> Copia canónica. `qa-test-creator` y `qa-debugger` referencian este archivo en vez de
> duplicar el texto — evita drift entre skills.

## R-K1 — `<selectorMethod>` solo acepta valores de *método*, no *estrategias*

Valores permitidos: `BASIC`, `XPATH`, `IMAGE`, `IMAGE_BASED_GENERIC`, `CUSTOM_LOCATOR`.

**Prohibido** usar `ATTRIBUTES`, `ANDROID_UI_AUTOMATOR`, `ACCESSIBILITY`, `ID`, `CSS` como
valor de `<selectorMethod>` — son **estrategias** y van en `<locatorStrategy>`. Si Katalon
no encuentra el objeto en runtime ("Object not found") aunque el `.rs` parezca correcto,
casi siempre es esta regla.

## R-K2 — Consistencia obligatoria `<locator>` ↔ `<locatorStrategy>`

| `<locatorStrategy>` | Formato exigido en `<locator>` |
|---|---|
| `ANDROID_UI_AUTOMATOR` | Empieza con `new UiSelector()` |
| `ATTRIBUTES` / `XPATH` | Empieza con `//` o `/` |
| `ACCESSIBILITY` | content-desc literal (sin `//`, sin `new UiSelector`) |
| `ID` | resource-id literal |
| `CSS` (Web) | selector CSS literal (sin `//`) |

Confirmar también que la misma cadena esté correctamente en la entrada
`<locatorCollection>` correspondiente. El compilador no valida esto — la desalineación
revienta solo en runtime.

## R-K3 — XPath compatible con Appium UiAutomator2 / Selenium

Prohibidos los axes XPath: `following::`, `preceding::`, `ancestor::`,
`following-sibling::`, `preceding-sibling::`, `descendant-or-self::`. Appium UiAutomator2
los rechaza con `InvalidSelectorException`.

Permitido: `//tag[@attr='val']`, `//*[@attr]`, `contains()`, `starts-with()`, predicados
`[N]`, padre con `/..`.

Para vincular elementos hermanos (ej. "el botón asociado al título X"), usar **UiSelector
chains** (`fromParent`, `childSelector`) en Android, o un selector CSS con combinador
(`+`, `~`, `>`) en Web — nunca XPath axes.

## Checklist de cierre antes de guardar un `.rs`

- [ ] `<MobileElementEntity>` (o `<WebElementEntity>` — nunca mezclar)
- [ ] `<locator>` es texto plano, no bloque XML con hijos
- [ ] `<locatorStrategy>` al nivel raíz
- [ ] `<locatorCollection>` con las 12 entradas estándar (ID, NAME, XPATH, IMAGE,
      ACCESSIBILITY, ATTRIBUTES, ANDROID_VIEWTAG, IOS_PREDICATE_STRING,
      ANDROID_UI_AUTOMATOR, CLASS_NAME, CUSTOM, IOS_CLASS_CHAIN — agregar `CSS` para Web)
- [ ] ≥ 2 estrategias pobladas (Locator Coverage Rule)
- [ ] Sin duplicados por resource-id/CSS o nombre

## Locator Coverage Rule

Al documentar cualquier elemento, poblar siempre las estrategias disponibles en orden de
prioridad: `ACCESSIBILITY` (1) → `ANDROID_UI_AUTOMATOR` (2) → `ATTRIBUTES` (3). Nunca crear
un `.rs` con menos de 2 estrategias pobladas; si genuinamente no hay una segunda, documentar
`<!-- NOT AVAILABLE: razón -->` y escalar al usuario si la cobertura queda en 1/3.
