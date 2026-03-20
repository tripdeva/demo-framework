package kr.co.demo.client.jpa.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Multi DataSource 라우팅을 지원하는 DataSource.
 *
 * <p>{@link DataSourceContextHolder}에 설정된 키에 따라
 * 적절한 DataSource로 요청을 라우팅합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * DataSourceContextHolder.set("secondary");
 * // secondary DataSource로 쿼리 실행
 * DataSourceContextHolder.clear();
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 * @see DataSourceContextHolder
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

	/**
	 * 현재 스레드의 DataSource 키를 반환합니다.
	 *
	 * @return DataSource 조회 키 (null이면 기본 DataSource)
	 */
	@Override
	protected Object determineCurrentLookupKey() {
		return DataSourceContextHolder.get();
	}
}
