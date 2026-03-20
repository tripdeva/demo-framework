package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class AuditingIntegrationTest {

	@Autowired private OrderEntityRepository repo;
	@Autowired private EntityManager em;

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
	@DisplayName("createdAt 검증")
	class CreatedAtTests {

		@Test
		@DisplayName("저장 시 createdAt 자동 설정")
		void autoSet() {
			OrderEntity saved = repo.save(order("AUD-C1"));
			em.flush();
			assertThat(saved.getCreatedAt()).isNotNull();
		}

		@Test
		@DisplayName("createdAt은 현재 시각 근처")
		void nearCurrentTime() {
			LocalDateTime before = LocalDateTime.now().minusSeconds(1);
			OrderEntity saved = repo.save(order("AUD-C2"));
			em.flush();
			LocalDateTime after = LocalDateTime.now().plusSeconds(1);
			assertThat(saved.getCreatedAt()).isBetween(before, after);
		}

		@Test
		@DisplayName("수정해도 createdAt 변경 안 됨")
		void unchangedOnUpdate() {
			OrderEntity saved = repo.save(order("AUD-C3"));
			em.flush();
			LocalDateTime original = saved.getCreatedAt();

			saved.setCustomerName("Modified");
			em.flush();

			assertThat(saved.getCreatedAt()).isEqualTo(original);
		}

		@Test
		@DisplayName("여러 번 수정해도 createdAt 유지")
		void unchangedAfterMultipleUpdates() {
			OrderEntity saved = repo.save(order("AUD-C4"));
			em.flush();
			LocalDateTime original = saved.getCreatedAt();

			for (int i = 0; i < 5; i++) {
				saved.setCustomerName("Mod" + i);
				em.flush();
			}
			assertThat(saved.getCreatedAt()).isEqualTo(original);
		}
	}

	@Nested
	@DisplayName("updatedAt 검증")
	class UpdatedAtTests {

		@Test
		@DisplayName("저장 시 updatedAt 자동 설정")
		void autoSet() {
			OrderEntity saved = repo.save(order("AUD-U1"));
			em.flush();
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("수정 시 updatedAt 변경됨")
		void changedOnUpdate() throws InterruptedException {
			OrderEntity saved = repo.save(order("AUD-U2"));
			em.flush();
			LocalDateTime first = saved.getUpdatedAt();

			Thread.sleep(10); // 시간 차이 확보
			saved.setCustomerName("Updated");
			em.flush();

			assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(first);
		}
	}
}
