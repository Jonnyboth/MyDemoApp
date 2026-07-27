package com.kms.katalon.core.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Stub de @Keyword de Katalon — solo para que los Keywords del proyecto compilen.
 * En el runner, el routing se hace por reflexión directa sobre las clases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@interface Keyword {
    String name()        default ''
    String description() default ''
    String[] tags()      default []
}
