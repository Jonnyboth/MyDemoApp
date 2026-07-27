package com.kms.katalon.core.testobject

/**
 * Bridge de SelectorMethod de Katalon.
 * Usado por LocatorHelper para construir TestObjects dinámicos.
 */
enum SelectorMethod {
    BASIC,
    ATTRIBUTES,
    XPATH,
    CSS,
    ID,
    NAME,
    CLASS_NAME,
    ACCESSIBILITY,
    ANDROID_UI_AUTOMATOR,
    ANDROID_VIEWTAG,
    IOS_PREDICATE_STRING,
    IOS_CLASS_CHAIN,
    IMAGE,
    CUSTOM
}
