# .rs File Template Reference — Katalon Mobile (MobileElementEntity)

## Critical Rules

1. Root tag MUST be `<MobileElementEntity>` (NOT `<WebElementEntity>`)
2. Properties list MUST use `<webElementProperties>` (NOT `<properties>`)
3. `<locatorStrategy>ATTRIBUTES</locatorStrategy>` is MANDATORY
4. `<platform>ANDROID</platform>` or `<platform>IOS</platform>` is MANDATORY
5. `<selectorMethod>BASIC</selectorMethod>` is required
6. Generate a unique UUID for `<elementGuidId>` — use any UUID generator

---

## Template A — Android: Locate by resource-id

Use when the element has a stable `resource-id` in the app.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <elementGuidId>REPLACE-WITH-UUID-HERE</elementGuidId>
   <imagePath></imagePath>
   <name>ELEMENT_NAME</name>
   <selectorCollection>
      <entry>
         <key>BASIC</key>
         <value>resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
      </entry>
   </selectorCollection>
   <selectorMethod>BASIC</selectorMethod>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>resource-id</name>
         <type>Main</type>
         <value>com.tuempresa.app:id/YOUR_RESOURCE_ID</value>
      </WebElementProperty>
   </webElementProperties>
   <locator>
      <locatorStrategy>ATTRIBUTES</locatorStrategy>
      <value>resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
   </locator>
   <locatorCollection>
      <entry>
         <key>BASIC</key>
         <value>resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
      </entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
```

**Quick-fill checklist:**
- [ ] Replace `REPLACE-WITH-UUID-HERE` with a real UUID (e.g. `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)
- [ ] Replace `ELEMENT_NAME` with the file name (e.g. `buttonCloseWarningToast`)
- [ ] Replace `YOUR_RESOURCE_ID` with actual id from `mobile_list_elements_on_screen`

---

## Template B — Android: Locate by class + text

Use when the element has visible text but no unique resource-id.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <elementGuidId>REPLACE-WITH-UUID-HERE</elementGuidId>
   <imagePath></imagePath>
   <name>ELEMENT_NAME</name>
   <selectorCollection>
      <entry>
         <key>BASIC</key>
         <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot;</value>
      </entry>
   </selectorCollection>
   <selectorMethod>BASIC</selectorMethod>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>class</name>
         <type>Main</type>
         <value>android.widget.TextView</value>
      </WebElementProperty>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>text</name>
         <type>Main</type>
         <value>ELEMENT_TEXT</value>
      </WebElementProperty>
   </webElementProperties>
   <locator>
      <locatorStrategy>ATTRIBUTES</locatorStrategy>
      <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot;</value>
   </locator>
   <locatorCollection>
      <entry>
         <key>BASIC</key>
         <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot;</value>
      </entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
```

**Quick-fill checklist:**
- [ ] Replace `REPLACE-WITH-UUID-HERE` with a real UUID
- [ ] Replace `ELEMENT_NAME` with the file name
- [ ] Replace `ELEMENT_TEXT` with exact text shown on screen (case-sensitive, Unicode-safe)
- [ ] Adjust `class` if element is not a TextView (e.g. `android.widget.Button`, `android.view.View`)

---

## Template C — Android: Locate by class + text + resource-id (most precise)

Use when text alone is ambiguous (multiple elements with same text on screen).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <elementGuidId>REPLACE-WITH-UUID-HERE</elementGuidId>
   <imagePath></imagePath>
   <name>ELEMENT_NAME</name>
   <selectorCollection>
      <entry>
         <key>BASIC</key>
         <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot; AND resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
      </entry>
   </selectorCollection>
   <selectorMethod>BASIC</selectorMethod>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>class</name>
         <type>Main</type>
         <value>android.widget.TextView</value>
      </WebElementProperty>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>text</name>
         <type>Main</type>
         <value>ELEMENT_TEXT</value>
      </WebElementProperty>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>resource-id</name>
         <type>Main</type>
         <value>com.tuempresa.app:id/YOUR_RESOURCE_ID</value>
      </WebElementProperty>
   </webElementProperties>
   <locator>
      <locatorStrategy>ATTRIBUTES</locatorStrategy>
      <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot; AND resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
   </locator>
   <locatorCollection>
      <entry>
         <key>BASIC</key>
         <value>class=&quot;android.widget.TextView&quot; AND text=&quot;ELEMENT_TEXT&quot; AND resource-id=&quot;com.tuempresa.app:id/YOUR_RESOURCE_ID&quot;</value>
      </entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
```

---

## Template D — iOS: Locate by accessibility id

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <elementGuidId>REPLACE-WITH-UUID-HERE</elementGuidId>
   <imagePath></imagePath>
   <name>ELEMENT_NAME</name>
   <selectorCollection>
      <entry>
         <key>BASIC</key>
         <value>accessibility id=&quot;ACCESSIBILITY_LABEL&quot;</value>
      </entry>
   </selectorCollection>
   <selectorMethod>BASIC</selectorMethod>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <WebElementProperty>
         <condition>EQUALS</condition>
         <isSelected>true</isSelected>
         <matchCondition>EQUALS</matchCondition>
         <name>accessibility id</name>
         <type>Main</type>
         <value>ACCESSIBILITY_LABEL</value>
      </WebElementProperty>
   </webElementProperties>
   <locator>
      <locatorStrategy>ATTRIBUTES</locatorStrategy>
      <value>accessibility id=&quot;ACCESSIBILITY_LABEL&quot;</value>
   </locator>
   <locatorCollection>
      <entry>
         <key>BASIC</key>
         <value>accessibility id=&quot;ACCESSIBILITY_LABEL&quot;</value>
      </entry>
   </locatorCollection>
   <locatorStrategy>ATTRIBUTES</locatorStrategy>
   <platform>IOS</platform>
</MobileElementEntity>
```

---

## Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `Name is null at MobileLocatorStrategy.valueOf` | Missing `<locatorStrategy>` tag | Add `<locatorStrategy>ATTRIBUTES</locatorStrategy>` inside `<locator>` AND as standalone tag |
| Tap hits wrong element | Used `<WebElementEntity>` instead of `<MobileElementEntity>` | Rename root tag and fix `<properties>` → `<webElementProperties>` |
| Element not found | Text has trailing space, special char, or wrong case | Copy text exactly from `mobile_list_elements_on_screen` output |
| Element not found (dynamic content) | Text changes per session (e.g. prices, distances) | Use resource-id only, or use `Mobile.scrollToText()` + tap by text at runtime |
| `&quot;` showing as literal text | XML was not escaped properly | Always escape `"` as `&quot;` inside XML attribute value strings |

---

## UUID Generation (Quick Methods)

**Terminal:**
```bash
uuidgen
# Output: A1B2C3D4-E5F6-7890-ABCD-EF1234567890
```

**Online:** https://www.uuidgenerator.net/

**Pattern (manual):** `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx` — use any hex digits; uniqueness within project is sufficient.
