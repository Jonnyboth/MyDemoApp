<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Titulo del producto en la posicion 1 (primera fila, primera columna) del grid "Products" -- solo lectura, usado para validar el orden alfabetico por defecto (SIM-TC-16). resource-id compartido entre todos los productos del RecyclerView (igual que productIV en btn_firstProductImage.rs), se usa instance(0) para tomar siempre la primera tarjeta. Sin content-desc (confirmado en dump real, emulator-5554).</description>
   <name>lbl_productTitle1</name>
   <tag></tag>
   <elementGuidId>4ca7ccb0-9dfd-4bdb-b2b5-84f05dfad2bf</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>31bff8ca-ad17-4f1f-94e3-280f8427150c</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/titleTV</value>
      <webElementGuid>6d568c68-a1fd-4c0f-87a3-47c062233c7b</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(0)</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/titleTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[1]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>(//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/titleTV"])[1]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/titleTV").instance(0)</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
