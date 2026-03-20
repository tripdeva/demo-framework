package kr.co.demo.domain.mapper;

import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 사용자 정의 OrderMapper
 *
 * <p>자동 생성된 OrderBaseMapper를 상속하여 커스텀 메서드를 추가합니다.
 */
@Mapper
public interface OrderMapper extends OrderBaseMapper {

    // ==================== 어노테이션 방식 ====================

    /**
     * 상태로 주문 조회
     */
    @Select("SELECT id, order_no AS orderNumber, customer_name AS customerName, status, total_amount AS totalAmount, ordered_at AS orderedAt FROM orders WHERE status = #{status}")
    List<Order> findByStatus(OrderStatus status);

    /**
     * 고객명으로 주문 조회
     */
    @Select("SELECT id, order_no AS orderNumber, customer_name AS customerName, status, total_amount AS totalAmount, ordered_at AS orderedAt FROM orders WHERE customer_name = #{customerName}")
    List<Order> findByCustomerName(String customerName);

    // ==================== XML 방식 (메서드 시그니처만) ====================

    /**
     * 복잡한 조건 검색 (XML로 구현)
     */
    List<Order> findByCondition(OrderSearchCondition condition);
}
