package kr.co.demo.core.storage.annotation;

import java.lang.annotation.*;

/**
 * Patch 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {
}

