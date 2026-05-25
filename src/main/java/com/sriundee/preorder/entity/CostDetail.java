package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_cost_detail")
@Data
public class CostDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_cost_detail")
    private Integer id;

    @Column(name = "ID_cost")
    private Integer cost;

    @Column(name = "ID_order_detail")
    private Integer order_detail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Integer getOrder_detail() {
        return order_detail;
    }

    public void setOrder_detail(Integer order_detail) {
        this.order_detail = order_detail;
    }
}
