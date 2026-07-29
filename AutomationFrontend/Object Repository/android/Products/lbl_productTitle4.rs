<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Titulo del producto en la posicion 4 (segunda fila, segunda columna) del grid "Products" -- solo lectura, usado para validar el orden alfabetico por defecto (SIM-TC-16). resource-id compartido entre todos los productos del RecyclerView, se usa instance(3) para tomar siempre la cuarta tarjeta. Sin content-desc (confirmado en dump real, emulator-5554).</description>
   <name>lbl_productTitle4</name>
   <tag></tag>
   <elementGuidId>1b32feba-d397-4599-b304-972b3289c058</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>d19d53ad-c982-448b-ab42-a39e66c9851f</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/titleTV</value>
      <webElementGuid>94fabd37-1e8b-4fce-91ce-981cea44fd55</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(3)</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/titleTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[4]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[4]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(3)</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
