package com.sriundee.preorder.dto;

public class VersionDto {
	
	private String name;
	private Integer product;
    
    public VersionDto() {
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getProduct() {
		return product;
	}

	public void setProduct(Integer product) {
		this.product = product;
	}

	@Override
	public String toString() {
		return "VersionDto [name=" + name + ", product=" + product + "]";
	}
}