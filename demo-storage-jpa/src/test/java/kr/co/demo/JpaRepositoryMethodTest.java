package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class JpaRepositoryMethodTest {

	@Autowired private OrderEntityRepository repo;
	@Autowired private EntityManager em;

	private OrderEntity order(String num, String name, OrderStatus st, BigDecimal amt) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(name);
		e.setStatus(st);
		e.setTotalAmount(amt);
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	@BeforeEach
	void setUp() {
		IntStream.range(0, 25).forEach(i ->
				repo.save(order("JR-" + String.format("%03d", i), "User" + (i % 5),
						OrderStatus.values()[i % 5],
						new BigDecimal((i + 1) * 1000))));
		em.flush();
		em.clear();
	}

	@Nested
	@DisplayName("JpaRepository 기본 메서드")
	class BasicMethods {

		@Test
		@DisplayName("count")
		void count() {
			assertThat(repo.count()).isEqualTo(25);
		}

		@Test
		@DisplayName("existsById - true")
		void existsTrue() {
			Long id = repo.findAll().get(0).getId();
			assertThat(repo.existsById(id)).isTrue();
		}

		@Test
		@DisplayName("existsById - false")
		void existsFalse() {
			assertThat(repo.existsById(999999L)).isFalse();
		}

		@Test
		@DisplayName("findById - present")
		void findByIdPresent() {
			Long id = repo.findAll().get(0).getId();
			assertThat(repo.findById(id)).isPresent();
		}

		@Test
		@DisplayName("findById - empty")
		void findByIdEmpty() {
			assertThat(repo.findById(999999L)).isEmpty();
		}

		@Test
		@DisplayName("findAllById")
		void findAllById() {
			List<Long> ids = repo.findAll().stream()
					.limit(5).map(OrderEntity::getId).toList();
			assertThat(repo.findAllById(ids)).hasSize(5);
		}

		@Test
		@DisplayName("deleteAllById")
		void deleteAllById() {
			List<Long> ids = repo.findAll().stream()
					.limit(3).map(OrderEntity::getId).toList();
			repo.deleteAllById(ids);
			em.flush();
			assertThat(repo.count()).isEqualTo(22);
		}

		@Test
		@DisplayName("deleteAll(entities)")
		void deleteAllEntities() {
			List<OrderEntity> toDelete = repo.findAll().stream().limit(5).toList();
			repo.deleteAll(toDelete);
			em.flush();
			assertThat(repo.count()).isEqualTo(20);
		}

		@Test
		@DisplayName("saveAll + flush")
		void saveAllFlush() {
			List<OrderEntity> news = List.of(
					order("NEW-1", "A", OrderStatus.PENDING, BigDecimal.ONE),
					order("NEW-2", "B", OrderStatus.PENDING, BigDecimal.ONE));
			repo.saveAllAndFlush(news);
			assertThat(repo.count()).isEqualTo(27);
		}
	}

	@Nested
	@DisplayName("정렬 메서드")
	class SortMethods {

		@Test
		@DisplayName("findAll(Sort) - 금액 오름차순")
		void sortAsc() {
			List<OrderEntity> sorted = repo.findAll(
					Sort.by(Sort.Direction.ASC, "totalAmount"));
			for (int i = 0; i < sorted.size() - 1; i++) {
				assertThat(sorted.get(i).getTotalAmount())
						.isLessThanOrEqualTo(sorted.get(i + 1).getTotalAmount());
			}
		}

		@Test
		@DisplayName("findAll(Sort) - 이름 내림차순")
		void sortDesc() {
			List<OrderEntity> sorted = repo.findAll(
					Sort.by(Sort.Direction.DESC, "customerName"));
			assertThat(sorted).isNotEmpty();
		}

		@Test
		@DisplayName("findAll(Sort) - 복합 정렬")
		void multiSort() {
			List<OrderEntity> sorted = repo.findAll(
					Sort.by("status").ascending().and(Sort.by("totalAmount").descending()));
			assertThat(sorted).hasSize(25);
		}
	}

	@Nested
	@DisplayName("페이징 메서드")
	class PagingMethods {

		@Test
		@DisplayName("Page 0, size 5")
		void firstPage() {
			Page<OrderEntity> page = repo.findAll(PageRequest.of(0, 5));
			assertThat(page.getContent()).hasSize(5);
			assertThat(page.getTotalElements()).isEqualTo(25);
			assertThat(page.getTotalPages()).isEqualTo(5);
			assertThat(page.isFirst()).isTrue();
			assertThat(page.hasNext()).isTrue();
		}

		@Test
		@DisplayName("마지막 페이지")
		void lastPage() {
			Page<OrderEntity> page = repo.findAll(PageRequest.of(4, 5));
			assertThat(page.getContent()).hasSize(5);
			assertThat(page.isLast()).isTrue();
			assertThat(page.hasNext()).isFalse();
		}

		@Test
		@DisplayName("빈 페이지")
		void emptyPage() {
			Page<OrderEntity> page = repo.findAll(PageRequest.of(10, 5));
			assertThat(page.getContent()).isEmpty();
			assertThat(page.getTotalElements()).isEqualTo(25);
		}

		@Test
		@DisplayName("페이지 + 정렬")
		void pageWithSort() {
			Page<OrderEntity> page = repo.findAll(
					PageRequest.of(0, 10, Sort.by("totalAmount").descending()));
			assertThat(page.getContent()).hasSize(10);
			assertThat(page.getContent().get(0).getTotalAmount())
					.isGreaterThanOrEqualTo(page.getContent().get(9).getTotalAmount());
		}

		@ParameterizedTest
		@ValueSource(ints = {1, 2, 3, 5, 7, 10, 13, 25, 50})
		@DisplayName("다양한 pageSize에서 totalElements 일관")
		void variousPageSizes(int size) {
			Page<OrderEntity> page = repo.findAll(PageRequest.of(0, size));
			assertThat(page.getTotalElements()).isEqualTo(25);
		}

		@Test
		@DisplayName("전체 페이지 순회 시 모든 엔티티 커버")
		void iterateAllPages() {
			int pageSize = 7;
			int totalCollected = 0;
			for (int p = 0; ; p++) {
				Page<OrderEntity> page = repo.findAll(PageRequest.of(p, pageSize));
				totalCollected += page.getContent().size();
				if (!page.hasNext()) break;
			}
			assertThat(totalCollected).isEqualTo(25);
		}
	}

	@Nested
	@DisplayName("수정 후 재조회")
	class UpdateAndReread {

		@Test
		@DisplayName("필드 수정 → flush → clear → 재조회 일치")
		void modifyFlushReread() {
			OrderEntity e = repo.findAll().get(0);
			e.setCustomerName("MODIFIED");
			repo.flush();
			em.clear();

			OrderEntity reloaded = repo.findById(e.getId()).get();
			assertThat(reloaded.getCustomerName()).isEqualTo("MODIFIED");
		}

		@Test
		@DisplayName("status 변경 후 재조회")
		void statusChange() {
			OrderEntity e = repo.findAll().get(0);
			e.setStatus(OrderStatus.CANCELLED);
			repo.flush();
			em.clear();

			assertThat(repo.findById(e.getId()).get().getStatus())
					.isEqualTo(OrderStatus.CANCELLED);
		}
	}
}
