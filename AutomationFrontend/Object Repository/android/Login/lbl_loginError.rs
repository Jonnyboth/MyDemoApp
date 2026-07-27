<?xml version="1.0" encoding="UTF-8"?>
<MobileElementEntity>
   <description>Label de error debajo del campo Password - muestra "Sorry this user has been locked out." o "Username is required" segun el escenario</description>
   <name>lbl_loginError</name>
   <tag></tag>
   <elementGuidId>50f60d70-9da7-48b2-870a-dfa9b27aa9d6</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>android.widget.TextView</value>
      <webElementGuid>9f92bc0d-ca38-41c4-b769-7eaee0c8255e</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>resource-id</name>
      <type>Main</type>
      <value>com.saucelabs.mydemoapp.android:id/passwordErrorTV</value>
      <webElementGuid>bf182c3d-499b-41b1-83ac-5fb4a5f892fa</webElementGuid>
   </webElementProperties>
   <locator>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/passwordErrorTV")</locator>
   <locatorCollection>
      <entry><key>ID</key><value>com.saucelabs.mydemoapp.android:id/passwordErrorTV</value></entry>
      <entry><key>NAME</key><value></value></entry>
      <entry><key>XPATH</key><value>//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/passwordErrorTV"]</value></entry>
      <entry><key>IMAGE</key><value></value></entry>
      <entry><key>ACCESSIBILITY</key><value><!-- NOT AVAILABLE: sin content-desc --></value></entry>
      <entry><key>ATTRIBUTES</key><value>//android.widget.TextView[@resource-id="com.saucelabs.mydemoapp.android:id/passwordErrorTV"]</value></entry>
      <entry><key>ANDROID_VIEWTAG</key><value></value></entry>
      <entry><key>IOS_PREDICATE_STRING</key><value></value></entry>
      <entry><key>ANDROID_UI_AUTOMATOR</key><value>new UiSelector().resourceId("com.saucelabs.mydemoapp.android:id/passwordErrorTV")</value></entry>
      <entry><key>CLASS_NAME</key><value>android.widget.TextView</value></entry>
      <entry><key>CUSTOM</key><value></value></entry>
      <entry><key>IOS_CLASS_CHAIN</key><value></value></entry>
   </locatorCollection>
   <locatorStrategy>ANDROID_UI_AUTOMATOR</locatorStrategy>
   <platform>ANDROID</platform>
</MobileElementEntity>
