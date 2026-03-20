package kr.co.demo;

import kr.co.demo.client.mybatis.util.Patch;
import kr.co.demo.client.mybatis.util.PatchValue;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.mapper.OrderBaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderBaseMapper 테스트
 */
@SpringBootTest
@Transactional
class OrderBaseMapperTest {

    @Autowired
    OrderBaseMapper orderBaseMapper;

    @Test
    void testInsertAndFindById() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));

        // when
        int result = orderBaseMapper.insert(order);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(order.getId()).isNotNull();  // 자동 생성된 ID

        // findById 테스트
        Order found = orderBaseMapper.findById(order.getId());
        assertThat(found).isNotNull();
        assertThat(found.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(found.getCustomerName()).isEqualTo("홍길동");
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);

        System.out.println("✅ insert & findById 성공!");
    }

    @Test
    void testFindAll() {
        // given
        orderBaseMapper.insert(createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000")));
        orderBaseMapper.insert(createOrder("ORD-002", "김철수", OrderStatus.CONFIRMED, new BigDecimal("20000")));
        orderBaseMapper.insert(createOrder("ORD-003", "이영희", OrderStatus.SHIPPED, new BigDecimal("30000")));

        // when
        List<Order> orders = orderBaseMapper.findAll();

        // then
        assertThat(orders).hasSize(3);
        System.out.println("✅ findAll 성공! 조회된 주문: " + orders.size() + "건");
    }

    @Test
    void testUpdate() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));
        orderBaseMapper.insert(order);

        // when
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("15000"));

        int result = orderBaseMapper.update(order);

        // then
        assertThat(result).isEqualTo(1);

        Order updated = orderBaseMapper.findById(order.getId());
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15000"));

        System.out.println("✅ update 성공!");
    }

    @Test
    void testPatch() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));
        orderBaseMapper.insert(order);

        Patch<Order> patch = Patch.create(
                Order.class,
                order.getId(),
                PatchValue.of("orderNumber", "TEST"),
                PatchValue.of("customerName", "TEST"),
                PatchValue.of("orderedAt", null)
        );

        int result = orderBaseMapper.patch(patch);

        // then
        assertThat(result).isEqualTo(1);

        Order updated = orderBaseMapper.findById(order.getId());
        assertThat(updated.getOrderNumber()).isEqualTo("TEST");
        assertThat(updated.getCustomerName()).isEqualTo("TEST");

        System.out.println("✅ patch 성공!");
    }

    @Test
    void testUpsert() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));
        orderBaseMapper.insert(order);

        Order order2 = orderBaseMapper.findById(order.getId());
        order2.setCustomerName("TEST");

        int result = orderBaseMapper.save(order2);

        // then
        assertThat(result).isEqualTo(1);

        Order updated = orderBaseMapper.findById(order.getId());
        assertThat(updated.getCustomerName()).isEqualTo("TEST");

        System.out.println("✅ upsert 성공!");
    }

    @Test
    void testDeleteById() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));
        orderBaseMapper.insert(order);
        Long orderId = order.getId();

        // when
        int result = orderBaseMapper.deleteById(orderId);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(orderBaseMapper.findById(orderId)).isNull();

        System.out.println("✅ deleteById 성공!");
    }

    @Test
    void testCount() {
        // given
        orderBaseMapper.insert(createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000")));
        orderBaseMapper.insert(createOrder("ORD-002", "김철수", OrderStatus.CONFIRMED, new BigDecimal("20000")));

        // when
        long count = orderBaseMapper.count();

        // then
        assertThat(count).isEqualTo(2);
        System.out.println("✅ count 성공! 총 " + count + "건");
    }

    @Test
    void testExistsById() {
        // given
        Order order = createOrder("ORD-001", "홍길동", OrderStatus.PENDING, new BigDecimal("10000"));
        orderBaseMapper.insert(order);

        // when & then
        assertThat(orderBaseMapper.existsById(order.getId())).isTrue();
        assertThat(orderBaseMapper.existsById(9999L)).isFalse();

        System.out.println("✅ existsById 성공!");
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
