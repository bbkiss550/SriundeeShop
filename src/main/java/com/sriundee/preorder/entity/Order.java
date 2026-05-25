package com.sriundee.preorder.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity
@Table(name = "t_order")
@Data
public class Order {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_order")
    private Integer id;

    @Column(name = "o_order_date")
    @Temporal(TemporalType.DATE)
    private Date order_date;
    
    @Column(name = "o_order_code")
    private String order_code;
    
    @Column(name = "o_customer_name")
    private String customer_name;

    @Column(name = "ID_pay_method")
    private Integer pay_method;

    @Column(name = "ID_pay_type")
    private Integer pay_type;

    @Column(name = "o_last_pay_date")
    @Temporal(TemporalType.DATE)
    private Date last_pay_date;

    @Column(name = "o_send_cost")
    private double send_cost;

    @Column(name = "o_discount")
    private double discount;

    @Column(name = "o_price_total")
    private double price_total;

    @Column(name = "o_price_pledge")
    private double price_pledge;

    @Column(name = "o_price_balance")
    private double price_balance;

    @Column(name = "o_net")
    private double net;

    @Column(name = "o_remark")
    private String remark;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getOrder_date() {
		return order_date;
	}

	public void setOrder_date(Date order_date) {
		this.order_date = order_date;
	}

	public String getOrder_code() {
		return order_code;
	}

	public void setOrder_code(String order_code) {
		this.order_code = order_code;
	}

	public String getCustomer_name() {
		return customer_name;
	}

	public void setCustomer_name(String customer_name) {
		this.customer_name = customer_name;
	}

	public Integer getPay_method() {
		return pay_method;
	}

	public void setPay_method(Integer pay_method) {
		this.pay_method = pay_method;
	}

	public Integer getPay_type() {
		return pay_type;
	}

	public void setPay_type(Integer pay_type) {
		this.pay_type = pay_type;
	}

	public Date getLast_pay_date() {
		return last_pay_date;
	}

	public void setLast_pay_date(Date last_pay_date) {
		this.last_pay_date = last_pay_date;
	}

	public double getSend_cost() {
		return send_cost;
	}

	public void setSend_cost(double send_cost) {
		this.send_cost = send_cost;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
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

	public double getNet() {
		return net;
	}

	public void setNet(double net) {
		this.net = net;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@PrePersist
	public void prePersist() {
		if (order_date == null) {
			order_date = new Date();
		}
	}
}
