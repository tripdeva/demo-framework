package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link StorageJoin}의 컨테이너 어노테이션.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageJoins {

	/**
	 * JOIN 배열.
	 *
	 * @return StorageJoin 배열
	 */
	StorageJoin[] value();
}
