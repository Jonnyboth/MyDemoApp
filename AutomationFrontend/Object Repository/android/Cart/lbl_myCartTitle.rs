<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Titulo "My Cart" de la pantalla de carrito. Mismo resource-id que el titulo "Products" del catalogo (TextView de titulo generico reutilizado por pantalla) - valido solo en el contexto de la pantalla Cart.</description>
   <name>lbl_myCartTitle</name>
   <tag></tag>
   <elementGuidId>ffaacbe3-b932-4bd2-95a4-95dc65a48eb6</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>f3c2d735-c53d-442a-aa83-5b0cb0a5480d</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/productTV</value>
      <webElementGuid>6725e665-5cc2-484b-a471-d71b5cab0b4f</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/productTV")</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/productTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/productTV"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/productTV"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/productTV")</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
