<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Boton "Proceed To Checkout" en "My Cart". Mismo resource-id que btn_addToCart (cartBt) pero pantalla y content-desc distintos.</description>
   <name>btn_proceedToCheckout</name>
   <tag></tag>
   <elementGuidId>ece235bf-3bc5-4b2d-bc7b-296026c6e254</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.Button</value>
      <webElementGuid>8e89b32d-81b2-420c-adbb-d08875f6e373</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>content-desc</name>
      <type>Main</type>
      <value>Confirms products for checkout</value>
      <webElementGuid>58eb2e5e-bd1a-4b2a-ad68-042bbaaee1b3</webElementGuid>
   </webElementProperties>
   <locator>Confirms products for checkout</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/cartBt</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>//android.widget.Button[@content-desc="Confirms products for checkout"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value>Confirms products for checkout</value></entry>
      <entry><key>ATTRIBUTES</key><value>//android.widget.Button[@content-desc="Confirms products for checkout"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/cartBt")</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.Button</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ACCESSIBILITY</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
