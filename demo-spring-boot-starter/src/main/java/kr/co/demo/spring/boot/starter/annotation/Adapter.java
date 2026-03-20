package kr.co.demo.spring.boot.starter.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 포트의 구현체를 나타내는 어노테이션
 */
@Component
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {
	@AliasFor(annotation = Component.class)
	String value() default "";
}
