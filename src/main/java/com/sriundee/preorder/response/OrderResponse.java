package com.sriundee.preorder.response;

import com.sriundee.preorder.bean.ProductBean;

public class OrderResponse {
	
	private ProductBean product;
    private String listQty;
    private String listWebsite;
    private String listVersion;
    private String listCover;

    public OrderResponse(ProductBean product, String listQty, String listWebsite, String listVersion, String listCover) {
        this.product = product;
        this.listQty = listQty;
        this.listWebsite = listWebsite;
        this.listVersion = listVersion;
        this.listCover = listCover;
    }

	public ProductBean getProduct() {
		return product;
	}

	public void setProduct(ProductBean product) {
		this.product = product;
	}

	public String getListQty() {
		return listQty;
	}

	public void setListQty(String listQty) {
		this.listQty = listQty;
	}

	public String getListWebsite() {
		return listWebsite;
	}

	public void setListWebsite(String listWebsite) {
		this.listWebsite = listWebsite;
	}

	public String getListVersion() {
		return listVersion;
	}

	public void setListVersion(String listVersion) {
		this.listVersion = listVersion;
	}

	public String getListCover() {
		return listCover;
	}

	public void setListCover(String listCover) {
		this.listCover = listCover;
	}

}
