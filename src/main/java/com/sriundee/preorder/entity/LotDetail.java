package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_lot_detail")
@Data
public class LotDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_lot_detail")
    private Integer id;

    @Column(name = "ID_lot")
    private Integer lot;

    @Column(name = "ID_order_detail")
    private Integer order_detail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLot() {
        return lot;
    }

    public void setLot(Integer lot) {
        this.lot = lot;
    }

    public Integer getOrder_detail() {
        return order_detail;
    }

    public void setOrder_detail(Integer order_detail) {
        this.order_detail = order_detail;
    }
}
