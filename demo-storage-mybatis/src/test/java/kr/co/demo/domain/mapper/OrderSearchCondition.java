package kr.co.demo.domain.mapper;

import kr.co.demo.domain.OrderStatus;

import java.math.BigDecimal;

/**
 * 주문 검색 조건
 */
public class OrderSearchCondition {

    private OrderStatus status;
    private String customerName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}
