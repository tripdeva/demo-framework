package kr.co.demo.client.jpa.config;

/**
 * ThreadLocal 기반 DataSource 컨텍스트 홀더.
 *
 * <p>현재 스레드에서 사용할 DataSource 키를 설정/조회/해제합니다.
 *
 * @author demo-framework
 * @since 1.1.0
 */
public final class DataSourceContextHolder {

	private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

	private DataSourceContextHolder() {
	}

	/**
	 * 현재 스레드의 DataSource 키를 설정합니다.
	 *
	 * @param key DataSource 키
	 */
	public static void set(String key) {
		CONTEXT.set(key);
	}

	/**
	 * 현재 스레드의 DataSource 키를 반환합니다.
	 *
	 * @return DataSource 키 (미설정 시 null → 기본 DataSource)
	 */
	public static String get() {
		return CONTEXT.get();
	}

	/**
	 * 현재 스레드의 DataSource 키를 해제합니다.
	 */
	public static void clear() {
		CONTEXT.remove();
	}
}
