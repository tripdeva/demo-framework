package kr.co.demo.spring.boot.starter.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 서비스의 구현체를 나타내는 어노테이션
 */
@Service
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseCase {
	@AliasFor(annotation = Service.class)
	String value() default "";
}
