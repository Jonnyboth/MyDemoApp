# POM Structure Reference — Katalon Mobile (Groovy)

## What is POM in Katalon Context?

Page Object Model (POM) in Katalon maps to **Custom Keywords** (groovy classes under `Keywords/`).
Each "page" or "screen" becomes a Groovy class with `@Keyword`-annotated methods.
Test scripts only call keywords — they do not contain raw `Mobile.*` calls.

---

## Directory Layout

```
Keywords/
  tuempresa/
    HomePage.groovy
    SuperPage.groovy
    MarketPage.groovy
    CheckoutPage.groovy

Object Repository/
  tuempresa/
    home/
      buttonSuperVertical.rs
      buttonMercadoVertical.rs
    super/
      searchBarSuper.rs
      storeName.rs
    market/
      headerGeant.rs
      buttonCloseWarningToast.rs

Scripts/
  testOpenMarketGeant/
    Script1773807085649.groovy   ← thin orchestration only

Test Cases/
  testOpenMarketGeant.tc
```

---

## Custom Keyword Class Template

```groovy
// Keywords/tuempresa/SuperPage.groovy
package tuempresa

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

public class SuperPage {

    @Keyword
    void tapSuperVertical() {
        Mobile.tap(findTestObject('Object Repository/tuempresa/home/buttonSuperVertical'), 10)
        Mobile.waitForElementPresent(
            findTestObject('Object Repository/tuempresa/super/searchBarSuper'), 10)
    }

    @Keyword
    void scrollToSupermercados() {
        Mobile.scrollToText('Supermercados', FailureHandling.OPTIONAL)
        Mobile.delay(1)
    }

    @Keyword
    void scrollRightInSupermercados() {
        Mobile.swipe(800, 1900, 400, 1900)
        Mobile.delay(1)
    }

    @Keyword
    void tapStore(String storeName) {
        // Dynamic object: locate by text at runtime
        TestObject obj = Mobile.findElements('android.widget.TextView',
            [text: storeName, 'resource-id': 'com.tuempresa.app:id/storeName'])
        Mobile.tap(obj, 5)
    }

    @Keyword
    boolean isStoreHeaderVisible(String storeName) {
        return Mobile.verifyElementVisible(
            findTestObject('Object Repository/tuempresa/market/headerGeant'), 5)
    }
}
```

---

## MarketPage Keyword Example (Géant Flow)

```groovy
// Keywords/tuempresa/MarketPage.groovy
package tuempresa

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling

import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

public class MarketPage {

    @Keyword
    void closeWarningToastIfPresent() {
        try {
            Mobile.tap(
                findTestObject('Object Repository/tuempresa/market/buttonCloseWarningToast'), 3)
        } catch (Exception e) {
            // Toast may not appear — this is acceptable
        }
    }

    @Keyword
    void verifyGeantHeader() {
        Mobile.verifyElementVisible(
            findTestObject('Object Repository/tuempresa/market/headerGeant'), 5)
    }
}
```

---

## Thin Test Script (Calls Keywords Only)

```groovy
// Scripts/testOpenMarketGeant/Script1773807085649.groovy
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling

// 1. Launch app via reusable test case
WebUI.callTestCase(findTestCase('Test Cases/testOpenApp'), [:], FailureHandling.STOP_ON_FAILURE)
Mobile.delay(3)

// 2. Page interactions via keywords
CustomKeywords.'tuempresa.MarketPage.closeWarningToastIfPresent'()
CustomKeywords.'tuempresa.SuperPage.tapSuperVertical'()
CustomKeywords.'tuempresa.SuperPage.scrollToSupermercados'()
CustomKeywords.'tuempresa.SuperPage.scrollRightInSupermercados'()
CustomKeywords.'tuempresa.SuperPage.tapStore'('Geant')
Mobile.delay(3)
CustomKeywords.'tuempresa.MarketPage.verifyGeantHeader'()
```

---

## Calling Custom Keywords from Scripts

Syntax:
```groovy
CustomKeywords.'packageName.ClassName.methodName'(args)
```

Examples:
```groovy
CustomKeywords.'tuempresa.HomePage.tapSuperVertical'()
CustomKeywords.'tuempresa.SuperPage.tapStore'('Géant')
CustomKeywords.'tuempresa.MarketPage.closeWarningToastIfPresent'()
```

No import needed — `CustomKeywords` is globally available in Katalon scripts.

---

## GlobalVariable Usage

Defined in `Profiles/default.glbl`:
```xml
<GlobalVariableEntities>
  <GlobalVariableEntity>
    <name>APP_PATH</name>
    <value>'/path/to/tuempresa.apk'</value>
  </GlobalVariableEntity>
  <GlobalVariableEntity>
    <name>DEVICE_ID</name>
    <value>'R5CY111XY3E'</value>
  </GlobalVariableEntity>
</GlobalVariableEntities>
```

Usage in scripts and keywords:
```groovy
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable

Mobile.startApplication(GlobalVariable.APP_PATH, false)
```

---

## callTestCase vs Custom Keywords

| Use Case | Approach |
|---|---|
| Reuse entire test flows (launch, login) | `WebUI.callTestCase(findTestCase('Test Cases/...'), [:], FailureHandling.STOP_ON_FAILURE)` |
| Reuse UI interactions within a test | `CustomKeywords.'package.Class.method'(args)` |
| One-off step not worth abstracting | Direct `Mobile.*` call in script |

---

## iOS Considerations

- Same Keyword classes work for iOS if locators are defined in `.rs` files per platform
- iOS `.rs` files use `<platform>IOS</platform>` and `xpath` or `accessibility id` strategy
- `Mobile.startApplication(appPath, false)` works the same on iOS
- Swipe coordinates differ — must recapture from iOS device
- `Mobile.scrollToText('Supermercados')` works on both platforms

---

## Naming Conventions

| Entity | Convention | Example |
|---|---|---|
| Keyword package | lowercase, domain | `tuempresa` |
| Keyword class | PascalCase + 'Page' | `SuperPage` |
| Keyword method | camelCase, imperative | `tapSuperVertical()` |
| Object Repository folder | camelCase screen name | `tuempresa/super/` |
| `.rs` file name | camelCase, descriptive | `buttonSuperVertical.rs` |
| Script folder | camelCase test name | `testOpenMarketGeant/` |
| Test Case name | camelCase, full | `testOpenMarketGeant` |

---

## Anti-Patterns to Avoid

- ❌ Raw `Mobile.tap(...)` directly in script (use keywords)
- ❌ Hardcoded coordinates in scripts (use swipe only inside keywords)
- ❌ `<WebElementEntity>` tag in `.rs` files for mobile (must be `<MobileElementEntity>`)
- ❌ Calling `Mobile.callTestCase(...)` — does not exist; use `WebUI.callTestCase(...)`
- ❌ Missing `Mobile.waitForElementPresent(...)` after navigation — causes timing failures
- ❌ `Mobile.tap(obj, 0)` when element might not be immediately visible — use `10` instead
