package com.sriundee.preorder.response;

public class OrderDetailResponse {
	
    private String listDetail;
    private Integer total_price;
    private Integer pledge_price;
    private Integer balance_price;

    public OrderDetailResponse(String listDetail, Integer total_price, Integer pledge_price, Integer balance_price) {
        this.listDetail = listDetail;
        this.total_price = total_price;
        this.pledge_price = pledge_price;
        this.balance_price = balance_price;
    }

	public String getListDetail() {
		return listDetail;
	}

	public void setListDetail(String listDetail) {
		this.listDetail = listDetail;
	}

	public Integer getTotal_price() {
		return total_price;
	}

	public void setTotal_price(Integer total_price) {
		this.total_price = total_price;
	}

	public Integer getPledge_price() {
		return pledge_price;
	}

	public void setPledge_price(Integer pledge_price) {
		this.pledge_price = pledge_price;
	}

	public Integer getBalance_price() {
		return balance_price;
	}

	public void setBalance_price(Integer balance_price) {
		this.balance_price = balance_price;
	}
}
