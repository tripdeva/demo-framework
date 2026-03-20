package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.OrderItemEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import kr.co.demo.domain.repository.OrderItemEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import kr.co.demo.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class CascadeIntegrationTest {

	@Autowired private OrderEntityRepository orderRepo;
	@Autowired private OrderItemEntityRepository itemRepo;
	@Autowired private EntityManager em;

	private OrderEntity order(String num) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName("Test");
		e.setStatus(OrderStatus.PENDING);
		e.setTotalAmount(BigDecimal.ONE);
		e.setOrderedAt(LocalDateTime.now());
		e.setItems(new ArrayList<>());
		return e;
	}

	private OrderItemEntity item(OrderEntity o, String prod) {
		OrderItemEntity i = new OrderItemEntity();
		i.setProductName(prod);
		i.setQuantity(1);
		i.setUnitPrice(BigDecimal.ONE);
		i.setOrder(o);
		return i;
	}

	@Nested
	@DisplayName("Cascade PERSIST 테스트")
	class CascadePersist {

		@Test
		@DisplayName("Order 저장 시 Items도 함께 저장 (cascade=ALL)")
		void cascadePersist() {
			OrderEntity o = order("CP-001");
			o.getItems().add(item(o, "Item A"));
			o.getItems().add(item(o, "Item B"));

			orderRepo.save(o);
			em.flush();
			em.clear();

			OrderEntity found = orderRepo.findById(o.getId()).orElse(null);
			assertThat(found).isNotNull();
			assertThat(found.getItems()).hasSize(2);
		}

		@Test
		@DisplayName("3개 아이템 cascade 저장")
		void cascadePersistThree() {
			OrderEntity o = order("CP-002");
			o.getItems().add(item(o, "A"));
			o.getItems().add(item(o, "B"));
			o.getItems().add(item(o, "C"));

			orderRepo.save(o);
			em.flush();
			em.clear();

			assertThat(orderRepo.findById(o.getId()).get().getItems()).hasSize(3);
		}
	}

	@Nested
	@DisplayName("Cascade REMOVE 테스트")
	class CascadeRemove {

		@Test
		@DisplayName("Order 삭제 시 Items도 함께 삭제")
		void cascadeRemove() {
			OrderEntity o = order("CR-001");
			o.getItems().add(item(o, "Del A"));
			o.getItems().add(item(o, "Del B"));

			orderRepo.save(o);
			em.flush();
			em.clear();

			long itemCountBefore = itemRepo.count();
			orderRepo.deleteById(o.getId());
			em.flush();

			assertThat(itemRepo.count()).isEqualTo(itemCountBefore - 2);
		}
	}

	@Nested
	@DisplayName("Cascade MERGE 테스트")
	class CascadeMerge {

		@Test
		@DisplayName("Order 수정 시 Item 추가도 반영")
		void cascadeMergeAddItem() {
			OrderEntity o = order("CM-001");
			o.getItems().add(item(o, "Original"));
			orderRepo.save(o);
			em.flush();
			em.clear();

			OrderEntity managed = orderRepo.findById(o.getId()).get();
			managed.getItems().add(item(managed, "Added"));
			em.flush();
			em.clear();

			assertThat(orderRepo.findById(o.getId()).get().getItems()).hasSize(2);
		}
	}
}
