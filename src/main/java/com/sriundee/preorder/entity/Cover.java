package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "c_delete")
    private String delete;
    
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

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}
}