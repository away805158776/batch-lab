package com.example.batchlab.domain.entity;

import java.math.BigDecimal;

public class TransactionEntity {

    private Long id;
    private String name;
    private BigDecimal amount;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TransactionEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}
