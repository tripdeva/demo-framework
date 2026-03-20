package kr.co.demo.spring.boot.starter.support;

import java.io.IOException;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

/**
 * YAML 파일을 @PropertySource에서 사용할 수 있도록 지원하는 Factory.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @PropertySource(value = "classpath:config.yml",
 *                 factory = YamlPropertySourceFactory.class)
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

	@Override
	public PropertySource<?> createPropertySource(String name,
	                                               EncodedResource resource) throws IOException {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(resource.getResource());
		Properties properties = factory.getObject();

		return new PropertiesPropertySource(
				resource.getResource().getFilename(),
				properties);
	}
}
