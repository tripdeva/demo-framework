package kr.co.demo.domain;

import kr.co.demo.core.storage.annotation.*;
import kr.co.demo.core.storage.enums.EnumType;
import kr.co.demo.core.storage.enums.RelationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 도메인 객체
 *
 * <p>Annotation Processor 테스트용 도메인 클래스입니다.
 * 빌드 시 다음 클래스들이 자동 생성됩니다:
 * <ul>
 *     <li>OrderEntity.java</li>
 *     <li>OrderStorageMapper.java</li>
 *     <li>OrderEntityRepository.java</li>
 * </ul>
 */
@StorageTable("orders")
public class Order {

    @StorageId
    private Long id;

    @StorageColumn(value = "order_no", nullable = false, unique = true)
    private String orderNumber;

    @StorageColumn(nullable = false)
    private String customerName;

    @StorageEnum(EnumType.STRING)
    private OrderStatus status;

    @StorageColumn(nullable = false)
    private BigDecimal totalAmount;

    private LocalDateTime orderedAt;

    @StorageRelation(type = RelationType.ONE_TO_MANY, mappedBy = "order")
    private List<OrderItem> items;

    @StorageTransient
    private String tempCalculation;

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getTempCalculation() {
        return tempCalculation;
    }

    public void setTempCalculation(String tempCalculation) {
        this.tempCalculation = tempCalculation;
    }
}
