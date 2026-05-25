package com.sriundee.preorder.dto;

public class CoverDto {

    private Integer id;
    private Integer product;
    private Integer website;
    private Integer version;
    private String name;
    private double price_total;
    private double price_pledge;
    private double price_balance;

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
	@Override
	public String toString() {
		return "CoverDto [id=" + id + ", product=" + product + ", website=" + website + ", version=" + version
				+ ", name=" + name + ", price_total=" + price_total + ", price_pledge=" + price_pledge
				+ ", price_balance=" + price_balance + "]";
	}
}