<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>RecyclerView del catalogo "Products". A diferencia de btn_firstProductImage (resource-id productIV, compartido con las imagenes de producto dentro de "My Cart"), este content-desc es exclusivo del catalogo -- "My Cart" usa "Displays list of selected products" para su propio RecyclerView con el mismo resource-id productRV. Sirve para confirmar sin ambiguedad que el catalogo esta visible, no solo "algun productIV visible".</description>
   <name>lbl_productsCatalog</name>
   <tag></tag>
   <elementGuidId>9c3e5a1f-2d7b-4f6e-8a0c-1b3d5e7f9a2c</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>androidx.recyclerview.widget.RecyclerView</value>
      <webElementGuid>4d6f8a0c-3e5b-4d7f-9a1c-2e4f6a8c0d3e</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>content-desc</name>
      <type>Main</type>
      <value>Displays all products of catalog</value>
      <webElementGuid>6e8a0c2d-4f6b-4e8a-9c1d-3f5a7c9e1b4f</webElementGuid>
   </webElementProperties>
   <locator>Displays all products of catalog</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/productRV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>//androidx.recyclerview.widget.RecyclerView[@content-desc="Displays all products of catalog"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value>Displays all products of catalog</value></entry>
      <entry><key>ATTRIBUTES</key><value>//androidx.recyclerview.widget.RecyclerView[@content-desc="Displays all products of catalog"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value></value></entry>
      <entry><key>CLASS_NAME</key><value>androidx.recyclerview.widget.RecyclerView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ACCESSIBILITY</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
