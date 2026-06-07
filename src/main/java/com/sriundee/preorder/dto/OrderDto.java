package com.sriundee.preorder.dto;

import java.util.List;

public class OrderDto {

    private Integer id;
    private String customer_name;
    private Integer pay_method;
    private Integer pay_type;
    private String order_date;
    private String last_pay_date;
    private double send_cost;
    private double discount;
    private double price_total;
    private double price_pledge;
    private double price_balance;
    private double net;
    private String remark;
    private String active_status;
    private String id_active_status;
    private List<OrderDetailDto> items;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
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
	public String getOrder_date() {
		return order_date;
	}
	public void setOrder_date(String order_date) {
		this.order_date = order_date;
	}
	public String getLast_pay_date() {
		return last_pay_date;
	}
	public void setLast_pay_date(String last_pay_date) {
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
	public String getActive_status() {
		return active_status;
	}
	public void setActive_status(String active_status) {
		this.active_status = active_status;
	}
	public String getId_active_status() {
		return id_active_status;
	}
	public void setId_active_status(String id_active_status) {
		this.id_active_status = id_active_status;
	}
	public List<OrderDetailDto> getItems() {
		return items;
	}
	public void setItems(List<OrderDetailDto> items) {
		this.items = items;
	}
}
