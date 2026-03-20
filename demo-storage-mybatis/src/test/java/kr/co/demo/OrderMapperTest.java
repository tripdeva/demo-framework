package kr.co.demo;

import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.mapper.OrderMapper;
import kr.co.demo.domain.mapper.OrderSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderMapper 통합 테스트
 *
 * <p>테스트 시나리오:
 * <ul>
 *     <li>BaseMapper 상속 메서드 (기본 CRUD)</li>
 *     <li>어노테이션 방식 커스텀 메서드</li>
 *     <li>XML 방식 커스텀 메서드</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class OrderMapperTest {

    @Autowired
    OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        orderMapper.insert(createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000")));
        orderMapper.insert(createOrder("ORD-002", "홍길동", OrderStatus.CONFIRMED, new BigDecimal("20000")));
        orderMapper.insert(createOrder("ORD-003", "김철수", OrderStatus.PENDING, new BigDecimal("30000")));
        orderMapper.insert(createOrder("ORD-004", "김철수", OrderStatus.SHIPPED, new BigDecimal("40000")));
        orderMapper.insert(createOrder("ORD-005", "이영희", OrderStatus.DELIVERED, new BigDecimal("50000")));
    }

    // ==================== BaseMapper 상속 메서드 테스트 ====================

    @Test
    @DisplayName("BaseMapper 상속 - findById")
    void testInheritedFindById() {
        List<Order> all = orderMapper.findAll();
        Order first = all.get(0);

        Order found = orderMapper.findById(first.getId());

        assertThat(found).isNotNull();
        assertThat(found.getOrderNumber()).isEqualTo(first.getOrderNumber());
        System.out.println("✅ BaseMapper.findById 상속 성공!");
    }

    @Test
    @DisplayName("BaseMapper 상속 - findAll")
    void testInheritedFindAll() {
        List<Order> orders = orderMapper.findAll();

        assertThat(orders).hasSize(5);
        System.out.println("✅ BaseMapper.findAll 상속 성공! 총 " + orders.size() + "건");
    }

    @Test
    @DisplayName("BaseMapper 상속 - count")
    void testInheritedCount() {
        long count = orderMapper.count();

        assertThat(count).isEqualTo(5);
        System.out.println("✅ BaseMapper.count 상속 성공! 총 " + count + "건");
    }

    // ==================== 어노테이션 방식 커스텀 메서드 테스트 ====================

    @Test
    @DisplayName("어노테이션 방식 - findByStatus")
    void testAnnotationFindByStatus() {
        List<Order> pendingOrders = orderMapper.findByStatus(OrderStatus.PENDING);

        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).allMatch(o -> o.getStatus() == OrderStatus.PENDING);
        System.out.println("✅ 어노테이션 방식 findByStatus 성공! PENDING 주문 " + pendingOrders.size() + "건");
    }

    @Test
    @DisplayName("어노테이션 방식 - findByCustomerName")
    void testAnnotationFindByCustomerName() {
        List<Order> orders = orderMapper.findByCustomerName("홍길동");

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomerName().equals("홍길동"));
        System.out.println("✅ 어노테이션 방식 findByCustomerName 성공! 홍길동 주문 " + orders.size() + "건");
    }

    // ==================== XML 방식 커스텀 메서드 테스트 ====================

    @Test
    @DisplayName("XML 방식 - findByCondition (상태 조건)")
    void testXmlFindByConditionWithStatus() {
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setStatus(OrderStatus.PENDING);

        List<Order> orders = orderMapper.findByCondition(condition);

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getStatus() == OrderStatus.PENDING);
        System.out.println("✅ XML 방식 findByCondition (상태) 성공!");
    }

    @Test
    @DisplayName("XML 방식 - findByCondition (고객명 LIKE 조건)")
    void testXmlFindByConditionWithCustomerName() {
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setCustomerName("김");

        List<Order> orders = orderMapper.findByCondition(condition);

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomerName().contains("김"));
        System.out.println("✅ XML 방식 findByCondition (고객명 LIKE) 성공!");
    }

    @Test
    @DisplayName("XML 방식 - findByCondition (금액 범위 조건)")
    void testXmlFindByConditionWithAmountRange() {
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setMinAmount(new BigDecimal("20000"));
        condition.setMaxAmount(new BigDecimal("40000"));

        List<Order> orders = orderMapper.findByCondition(condition);

        assertThat(orders).hasSize(3);
        assertThat(orders).allMatch(o -> 
            o.getTotalAmount().compareTo(new BigDecimal("20000")) >= 0 &&
            o.getTotalAmount().compareTo(new BigDecimal("40000")) <= 0
        );
        System.out.println("✅ XML 방식 findByCondition (금액 범위) 성공! " + orders.size() + "건");
    }

    @Test
    @DisplayName("XML 방식 - findByCondition (복합 조건)")
    void testXmlFindByConditionWithMultipleConditions() {
        OrderSearchCondition condition = new OrderSearchCondition();
        condition.setStatus(OrderStatus.PENDING);
        condition.setMinAmount(new BigDecimal("15000"));

        List<Order> orders = orderMapper.findByCondition(condition);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getCustomerName()).isEqualTo("김철수");
        System.out.println("✅ XML 방식 findByCondition (복합 조건) 성공!");
    }

    @Test
    @DisplayName("XML 방식 - findByCondition (조건 없음 - 전체 조회)")
    void testXmlFindByConditionWithNoCondition() {
        OrderSearchCondition condition = new OrderSearchCondition();

        List<Order> orders = orderMapper.findByCondition(condition);

        assertThat(orders).hasSize(5);
        System.out.println("✅ XML 방식 findByCondition (조건 없음) 성공! 전체 " + orders.size() + "건");
    }

    private Order createOrder(String orderNumber, String customerName,
                              OrderStatus status, BigDecimal totalAmount) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomerName(customerName);
        order.setStatus(status);
        order.setTotalAmount(totalAmount);
        order.setOrderedAt(LocalDateTime.now());
        return order;
    }
}
