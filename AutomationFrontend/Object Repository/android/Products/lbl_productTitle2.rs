<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Titulo del producto en la posicion 2 (primera fila, segunda columna) del grid "Products" -- solo lectura, usado para validar el orden alfabetico por defecto (SIM-TC-16). resource-id compartido entre todos los productos del RecyclerView, se usa instance(1) para tomar siempre la segunda tarjeta. Sin content-desc (confirmado en dump real, emulator-5554).</description>
   <name>lbl_productTitle2</name>
   <tag></tag>
   <elementGuidId>925c8e9c-eb51-4089-bfba-b6bbce1c7f71</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>c8234d6c-a179-4dfc-af61-c00eaee93881</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/titleTV</value>
      <webElementGuid>30777a63-def1-44da-8378-0ca5f5cefc63</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(1)</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/titleTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[2]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[2]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(1)</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
