package kr.co.demo.core.storage;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.demo.core.storage.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnumCompleteTest {

	@ParameterizedTest
	@EnumSource(DialectType.class)
	@DisplayName("DialectType valueOf 왕복")
	void dialectValueOf(DialectType dt) {
		assertThat(DialectType.valueOf(dt.name())).isEqualTo(dt);
	}

	@ParameterizedTest
	@EnumSource(RelationType.class)
	@DisplayName("RelationType valueOf 왕복")
	void relationValueOf(RelationType rt) {
		assertThat(RelationType.valueOf(rt.name())).isEqualTo(rt);
	}

	@ParameterizedTest
	@EnumSource(EnumType.class)
	@DisplayName("EnumType valueOf 왕복")
	void enumTypeValueOf(EnumType et) {
		assertThat(EnumType.valueOf(et.name())).isEqualTo(et);
	}

	@ParameterizedTest
	@EnumSource(CascadeType.class)
	@DisplayName("CascadeType valueOf 왕복")
	void cascadeValueOf(CascadeType ct) {
		assertThat(CascadeType.valueOf(ct.name())).isEqualTo(ct);
	}

	@ParameterizedTest
	@EnumSource(FetchType.class)
	@DisplayName("FetchType valueOf 왕복")
	void fetchValueOf(FetchType ft) {
		assertThat(FetchType.valueOf(ft.name())).isEqualTo(ft);
	}

	@ParameterizedTest
	@EnumSource(JoinType.class)
	@DisplayName("JoinType valueOf 왕복")
	void joinValueOf(JoinType jt) {
		assertThat(JoinType.valueOf(jt.name())).isEqualTo(jt);
	}

	@Test
	@DisplayName("DialectType ordinal 순서")
	void dialectOrdinals() {
		assertThat(DialectType.MYSQL.ordinal()).isEqualTo(0);
		assertThat(DialectType.POSTGRES.ordinal()).isEqualTo(1);
		assertThat(DialectType.ORACLE.ordinal()).isEqualTo(2);
		assertThat(DialectType.H2.ordinal()).isEqualTo(3);
	}

	@Test
	@DisplayName("RelationType 4개")
	void relationCount() {
		assertThat(RelationType.values()).hasSize(4);
	}

	@Test
	@DisplayName("EnumType 2개")
	void enumTypeCount() {
		assertThat(EnumType.values()).hasSize(2);
	}

	@Test
	@DisplayName("CascadeType 6개")
	void cascadeCount() {
		assertThat(CascadeType.values()).hasSize(6);
	}

	@Test
	@DisplayName("FetchType 3개")
	void fetchCount() {
		assertThat(FetchType.values()).hasSize(3);
	}

	@Test
	@DisplayName("JoinType 5개")
	void joinCount() {
		assertThat(JoinType.values()).hasSize(5);
	}
}
