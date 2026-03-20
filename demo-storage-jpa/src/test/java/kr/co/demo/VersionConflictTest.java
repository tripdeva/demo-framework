package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class VersionConflictTest {

	@Autowired private OrderEntityRepository repo;
	@Autowired private EntityManager em;
	@Autowired private JdbcTemplate jdbc;

	private OrderEntity order(String num) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName("Test");
		e.setStatus(OrderStatus.PENDING);
		e.setTotalAmount(BigDecimal.ONE);
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	@Nested
	@DisplayName("@Version 동작 검증")
	class VersionTests {

		@Test
		@DisplayName("초기 저장 시 version = 0")
		void initialVersion() {
			OrderEntity saved = repo.save(order("V-001"));
			em.flush();
			assertThat(saved.getVersion()).isEqualTo(0L);
		}

		@Test
		@DisplayName("1회 수정 후 version = 1")
		void afterOneUpdate() {
			OrderEntity saved = repo.save(order("V-002"));
			em.flush();
			saved.setCustomerName("Mod1");
			em.flush();
			assertThat(saved.getVersion()).isEqualTo(1L);
		}

		@Test
		@DisplayName("2회 수정 후 version = 2")
		void afterTwoUpdates() {
			OrderEntity saved = repo.save(order("V-003"));
			em.flush();
			saved.setCustomerName("Mod1");
			em.flush();
			saved.setCustomerName("Mod2");
			em.flush();
			assertThat(saved.getVersion()).isEqualTo(2L);
		}

		@Test
		@DisplayName("version이 DB에 저장됨")
		void versionPersistedInDb() {
			OrderEntity saved = repo.save(order("V-004"));
			em.flush();
			saved.setCustomerName("Up");
			em.flush();

			Long dbVersion = jdbc.queryForObject(
					"SELECT VERSION FROM ORDERS WHERE ID = ?", Long.class, saved.getId());
			assertThat(dbVersion).isEqualTo(1L);
		}

		@Test
		@DisplayName("detach된 엔티티의 stale version 수정 → 예외")
		void staleVersionUpdate() {
			OrderEntity saved = repo.save(order("V-005"));
			em.flush();

			// detach로 영속성 컨텍스트에서 분리
			Long id = saved.getId();
			Long staleVersion = saved.getVersion();
			em.clear();

			// 새로 로드 후 수정 → version 증가
			OrderEntity current = repo.findById(id).get();
			current.setCustomerName("First Update");
			repo.saveAndFlush(current);
			em.clear();

			// stale version으로 새 인스턴스 만들어 저장 시도
			OrderEntity stale = order("V-005");
			stale.setId(id);
			stale.setVersion(staleVersion);
			stale.setCustomerName("Stale Update");

			assertThatThrownBy(() -> repo.saveAndFlush(stale))
					.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		}
	}
}
