package kr.co.demo.domain;

import kr.co.demo.core.storage.annotation.StorageColumn;
import kr.co.demo.core.storage.annotation.StorageId;
import kr.co.demo.core.storage.annotation.StorageRelation;
import kr.co.demo.core.storage.annotation.StorageTable;
import kr.co.demo.core.storage.enums.RelationType;

import java.math.BigDecimal;

/**
 * 주문 항목 도메인 객체
 *
 * <p>Annotation Processor 테스트용 도메인 클래스입니다.
 * Order와 ManyToOne 관계를 가집니다.
 */
@StorageTable("order_items")
public class OrderItem {

    @StorageId
    private Long id;

    @StorageColumn(nullable = false)
    private String productName;

    @StorageColumn(nullable = false)
    private Integer quantity;

    @StorageColumn(nullable = false)
    private BigDecimal unitPrice;

    @StorageRelation(type = RelationType.MANY_TO_ONE)
    private Order order;

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
