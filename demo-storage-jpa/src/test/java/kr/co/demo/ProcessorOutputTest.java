package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import kr.co.demo.core.mapper.DomainMapper;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.OrderItemEntity;
import kr.co.demo.domain.mapper.OrderStorageMapper;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

class ProcessorOutputTest {

	@Nested
	@DisplayName("EntityGenerator 출력 검증")
	class EntityOutput {

		@Test
		@DisplayName("@Entity 어노테이션 존재")
		void hasEntityAnnotation() {
			assertThat(OrderEntity.class.isAnnotationPresent(Entity.class)).isTrue();
		}

		@Test
		@DisplayName("@Table(name='orders') 어노테이션")
		void tableAnnotation() {
			Table table = OrderEntity.class.getAnnotation(Table.class);
			assertThat(table).isNotNull();
			assertThat(table.name()).isEqualTo("orders");
		}

		@Test
		@DisplayName("id 필드에 @Id + @GeneratedValue")
		void idAnnotations() throws Exception {
			Field id = OrderEntity.class.getDeclaredField("id");
			assertThat(id.isAnnotationPresent(Id.class)).isTrue();
			assertThat(id.isAnnotationPresent(GeneratedValue.class)).isTrue();
			assertThat(id.getAnnotation(GeneratedValue.class).strategy())
					.isEqualTo(GenerationType.IDENTITY);
		}

		@Test
		@DisplayName("orderNumber 필드에 @Column(name='order_no', nullable=false)")
		void columnAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("orderNumber");
			Column col = f.getAnnotation(Column.class);
			assertThat(col).isNotNull();
			assertThat(col.name()).isEqualTo("order_no");
			assertThat(col.nullable()).isFalse();
		}

		@Test
		@DisplayName("orderNumber 필드에 unique=true")
		void uniqueConstraint() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("orderNumber");
			Column col = f.getAnnotation(Column.class);
			assertThat(col.unique()).isTrue();
		}

		@Test
		@DisplayName("customerName 필드에 nullable=false")
		void customerNameNotNull() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("customerName");
			Column col = f.getAnnotation(Column.class);
			assertThat(col).isNotNull();
			assertThat(col.nullable()).isFalse();
		}

		@Test
		@DisplayName("status 필드에 @Enumerated(EnumType.STRING)")
		void enumAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("status");
			Enumerated en = f.getAnnotation(Enumerated.class);
			assertThat(en).isNotNull();
			assertThat(en.value()).isEqualTo(EnumType.STRING);
		}

		@Test
		@DisplayName("@StorageTransient 필드(tempCalculation)가 Entity에 없음")
		void transientFieldAbsent() {
			List<String> fieldNames = Arrays.stream(OrderEntity.class.getDeclaredFields())
					.map(Field::getName)
					.toList();
			assertThat(fieldNames).doesNotContain("tempCalculation");
		}

		@Test
		@DisplayName("items 필드에 @OneToMany")
		void oneToManyAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("items");
			assertThat(f.isAnnotationPresent(OneToMany.class)).isTrue();
			OneToMany otm = f.getAnnotation(OneToMany.class);
			assertThat(otm.mappedBy()).isEqualTo("order");
		}

		@Test
		@DisplayName("OrderItemEntity.order 필드에 @ManyToOne")
		void manyToOneAnnotation() throws Exception {
			Field f = OrderItemEntity.class.getDeclaredField("order");
			assertThat(f.isAnnotationPresent(ManyToOne.class)).isTrue();
		}

		@Test
		@DisplayName("@StorageIndex → @Table(indexes) 생성됨")
		void indexAnnotation() {
			Table table = OrderEntity.class.getAnnotation(Table.class);
			assertThat(table.indexes()).hasSize(2);

			jakarta.persistence.Index idx0 = table.indexes()[0];
			assertThat(idx0.name()).isEqualTo("idx_status");
			assertThat(idx0.columnList()).isEqualTo("status");
			assertThat(idx0.unique()).isFalse();

			jakarta.persistence.Index idx1 = table.indexes()[1];
			assertThat(idx1.name()).isEqualTo("idx_customer_status");
			assertThat(idx1.columnList()).isEqualTo("customer_name, status");
			assertThat(idx1.unique()).isTrue();
		}

		@Test
		@DisplayName("Entity에 기본 생성자 존재")
		void noArgConstructor() throws Exception {
			OrderEntity instance = OrderEntity.class.getDeclaredConstructor().newInstance();
			assertThat(instance).isNotNull();
		}

		@Test
		@DisplayName("@StorageVersion → @Version 생성됨")
		void versionAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("version");
			assertThat(f.isAnnotationPresent(
					jakarta.persistence.Version.class)).isTrue();
		}

		@Test
		@DisplayName("@StorageCreatedAt → @CreatedDate + @Column(updatable=false)")
		void createdAtAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("createdAt");
			assertThat(f.isAnnotationPresent(
					org.springframework.data.annotation.CreatedDate.class)).isTrue();
			Column col = f.getAnnotation(Column.class);
			assertThat(col).isNotNull();
			assertThat(col.updatable()).isFalse();
		}

		@Test
		@DisplayName("@StorageUpdatedAt → @LastModifiedDate")
		void updatedAtAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("updatedAt");
			assertThat(f.isAnnotationPresent(
					org.springframework.data.annotation.LastModifiedDate.class)).isTrue();
		}

		@Test
		@DisplayName("@EntityListeners(AuditingEntityListener) 존재")
		void entityListenersAnnotation() {
			jakarta.persistence.EntityListeners listeners =
					OrderEntity.class.getAnnotation(jakarta.persistence.EntityListeners.class);
			assertThat(listeners).isNotNull();
			assertThat(listeners.value()).hasSize(1);
			assertThat(listeners.value()[0].getSimpleName())
					.isEqualTo("AuditingEntityListener");
		}

		@Test
		@DisplayName("cascade=ALL, fetch=LAZY 관계 어노테이션")
		void cascadeFetchAnnotation() throws Exception {
			Field f = OrderEntity.class.getDeclaredField("items");
			OneToMany otm = f.getAnnotation(OneToMany.class);
			assertThat(otm.cascade()).contains(jakarta.persistence.CascadeType.ALL);
			assertThat(otm.fetch()).isEqualTo(jakarta.persistence.FetchType.LAZY);
		}

		@Test
		@DisplayName("모든 필드에 getter/setter 존재")
		void gettersAndSetters() {
			List<String> fields = List.of("id", "orderNumber", "customerName",
					"status", "totalAmount", "orderedAt");
			for (String field : fields) {
				String cap = field.substring(0, 1).toUpperCase() + field.substring(1);
				assertThat(hasMethod(OrderEntity.class, "get" + cap))
						.as("getter for " + field).isTrue();
				assertThat(hasMethod(OrderEntity.class, "set" + cap))
						.as("setter for " + field).isTrue();
			}
		}

		private boolean hasMethod(Class<?> clazz, String name) {
			return Arrays.stream(clazz.getMethods())
					.anyMatch(m -> m.getName().equals(name));
		}
	}

	@Nested
	@DisplayName("RepositoryGenerator 출력 검증")
	class RepositoryOutput {

		@Test
		@DisplayName("JpaRepository를 상속")
		void extendsJpaRepository() {
			assertThat(JpaRepository.class.isAssignableFrom(OrderEntityRepository.class))
					.isTrue();
		}

		@Test
		@DisplayName("제네릭 타입이 <OrderEntity, Long>")
		void genericTypes() {
			var interfaces = OrderEntityRepository.class.getGenericInterfaces();
			assertThat(interfaces).hasSizeGreaterThan(0);
			ParameterizedType pt = (ParameterizedType) interfaces[0];
			assertThat(pt.getActualTypeArguments()[0].getTypeName())
					.contains("OrderEntity");
		}

		@Test
		@DisplayName("인터페이스임")
		void isInterface() {
			assertThat(OrderEntityRepository.class.isInterface()).isTrue();
		}
	}

	@Nested
	@DisplayName("MapperGenerator 출력 검증")
	class MapperOutput {

		@Test
		@DisplayName("@Component 어노테이션")
		void hasComponent() {
			assertThat(OrderStorageMapper.class.isAnnotationPresent(Component.class))
					.isTrue();
		}

		@Test
		@DisplayName("DomainMapper 인터페이스 구현")
		void implementsDomainMapper() {
			assertThat(DomainMapper.class.isAssignableFrom(OrderStorageMapper.class))
					.isTrue();
		}

		@Test
		@DisplayName("toStorage 메서드 존재")
		void hasToStorage() {
			assertThat(hasMethod("toStorage")).isTrue();
		}

		@Test
		@DisplayName("toDomain 메서드 존재")
		void hasToDomain() {
			assertThat(hasMethod("toDomain")).isTrue();
		}

		private boolean hasMethod(String name) {
			return Arrays.stream(OrderStorageMapper.class.getMethods())
					.anyMatch(m -> m.getName().equals(name));
		}
	}
}
