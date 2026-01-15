package com.sriundee.preorder.entity;

import jakarta.persistence.*;
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

    @Column(name = "ID_cov")
    private Integer cover;

    @Column(name = "od_qty")
    private Integer qty;

    @Column(name = "od_price_total")
    private double price_total;

    @Column(name = "od_price_pledge")
    private double price_pledge;

    @Column(name = "od_price_balance")
    private double price_balance;

    @Column(name = "od_price_1st")
    private double price_1st;

    @Column(name = "od_price_2nd")
    private double price_2nd;

    @Column(name = "od_price_last")
    private double price_last;

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

	public double getPrice_1st() {
		return price_1st;
	}

	public void setPrice_1st(double price_1st) {
		this.price_1st = price_1st;
	}

	public double getPrice_2nd() {
		return price_2nd;
	}

	public void setPrice_2nd(double price_2nd) {
		this.price_2nd = price_2nd;
	}

	public double getPrice_last() {
		return price_last;
	}

	public void setPrice_last(double price_last) {
		this.price_last = price_last;
	}

	public Integer getOrder_status() {
		return order_status;
	}

	public void setOrder_status(Integer order_status) {
		this.order_status = order_status;
	}
}