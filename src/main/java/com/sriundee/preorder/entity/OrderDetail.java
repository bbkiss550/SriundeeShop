package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_order_detail")
@Data
public class OrderDetail {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_order_detail")
    private Integer id;

    @Column(name = "ID_order")
    private Integer order;

    @Column(name = "ID_cover")
    private Integer cover;

    @Column(name = "od_qty")
    private Integer qty;

    @Column(name = "od_price_total")
    private double price_total;

    @Column(name = "od_price_pledge")
    private double price_pledge;

    @Column(name = "od_price_balance")
    private double price_balance;

    @Column(name = "ID_order_status")
    private Integer order_status;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public Integer getCover() {
		return cover;
	}

	public void setCover(Integer cover) {
		this.cover = cover;
	}

	public Integer getQty() {
		return qty;
	}

	public void setQty(Integer qty) {
		this.qty = qty;
	}

	public double getPrice_total() {
		return price_total;
	}

	public void setPrice_total(double price_total) {
		this.price_total = price_total;
	}

	public double getPrice_pledge() {
		return price_pledge;
	}

	public void setPrice_pledge(double price_pledge) {
		this.price_pledge = price_pledge;
	}

	public double getPrice_balance() {
		return price_balance;
	}

	public void setPrice_balance(double price_balance) {
		this.price_balance = price_balance;
	}

	public Integer getOrder_status() {
		return order_status;
	}

	public void setOrder_status(Integer order_status) {
		this.order_status = order_status;
	}
}