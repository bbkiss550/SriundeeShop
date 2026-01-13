package com.sriundee.preorder.response;

import com.sriundee.preorder.bean.ProductBean;

public class CoverResponse {
	
	private ProductBean product;
    private String listVersion;
    private String listWebsite;

    public CoverResponse(ProductBean product, String listVersion, String listWebsite) {
        this.product = product;
        this.listVersion = listVersion;
        this.listWebsite = listWebsite;
    }

	public ProductBean getProduct() {
		return product;
	}

	public void setProduct(ProductBean product) {
		this.product = product;
	}

	public String getListVersion() {
		return listVersion;
	}

	public void setListVersion(String listVersion) {
		this.listVersion = listVersion;
	}

	public String getListWebsite() {
		return listWebsite;
	}

	public void setListWebsite(String listWebsite) {
		this.listWebsite = listWebsite;
	}
}
