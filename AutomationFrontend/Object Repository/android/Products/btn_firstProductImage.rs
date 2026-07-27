<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Imagen del primer producto del grid "Products" (Sauce Labs Backpack) - abre el detalle. resource-id compartido entre todos los productos del RecyclerView, se usa instance(0) para tomar siempre el primero.</description>
   <name>btn_firstProductImage</name>
   <tag></tag>
   <elementGuidId>a4dc6163-de09-46fe-8e7e-6f968b161796</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.ImageView</value>
      <webElementGuid>0514102f-73a4-4fb7-9535-0aa51e6f05b3</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/productIV</value>
      <webElementGuid>c332a9a8-62f7-4f5b-9d21-250cac6acaf2</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/productIV").instance(0)</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/productIV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>(//android.widget.ImageView[@resource-id="com.saucelabs.mydemoapp.android:id/productIV"])[1]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>(//android.widget.ImageView[@resource-id="com.saucelabs.mydemoapp.android:id/productIV"])[1]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/productIV").instance(0)</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.ImageView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
