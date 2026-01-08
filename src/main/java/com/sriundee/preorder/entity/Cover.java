package com.sriundee.preorder.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_cover")
@Data
public class Cover {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_cover")
    private Integer id;

    @Column(name = "ID_pro")
    private Integer product;
    
    @Column(name = "ID_web")
    private Integer website;

    @Column(name = "ID_ver")
    private Integer version;
    
    @Column(name = "c_name")
    private String name;

    @Column(name = "c_price_total")
    private double price_total;

    @Column(name = "c_price_pledge")
    private double price_pledge;

    @Column(name = "c_price_balance")
    private double price_balance;

    @Column(name = "c_price_1st")
    private double price_1st;

    @Column(name = "c_price_2nd")
    private double price_2nd;

    @Column(name = "c_price_last")
    private double price_last;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getProduct() {
		return product;
	}

	public void setProduct(Integer product) {
		this.product = product;
	}

	public Integer getWebsite() {
		return website;
	}

	public void setWebsite(Integer website) {
		this.website = website;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
}