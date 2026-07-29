<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Titulo del producto en la posicion 3 (segunda fila, primera columna) del grid "Products" -- solo lectura, usado para validar el orden alfabetico por defecto (SIM-TC-16). resource-id compartido entre todos los productos del RecyclerView, se usa instance(2) para tomar siempre la tercera tarjeta. Sin content-desc (confirmado en dump real, emulator-5554).</description>
   <name>lbl_productTitle3</name>
   <tag></tag>
   <elementGuidId>fc0fcee9-e6d9-4e0c-a92a-c16e720ec2c8</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>9334794f-9f97-4fc9-af31-c4eaad49fa91</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/titleTV</value>
      <webElementGuid>d1b475f4-29e5-410a-8436-dfa26d00f5c0</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(2)</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/titleTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[3]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[3]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(2)</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
