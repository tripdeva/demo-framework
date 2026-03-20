package kr.co.demo.client.mybatis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

/**
 * MyBatis Storage 자동 설정
 *
 * <p>MyBatis가 클래스패스에 있을 때 자동으로 활성화됩니다.
 *
 * @author demo-framework
 * @since 1.0.0
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.ibatis.session.SqlSessionFactory")
public class MybatisStorageAutoConfiguration {

    // MyBatis 관련 추가 설정이 필요할 경우 여기에 Bean 정의
}
