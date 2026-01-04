package com.sriundee.preorder.dto;

import java.sql.Date;

public class ProductDto {

	private Integer id;

    private String name;

    private Integer type;
    
    private Integer artist;
    
    private String end_date;

    private String send_date;
    
    private String second_pay_date;
    
    private Integer payment_type;
    
    private String last_pay_date;

    private Integer product_status;

    private String pic;
    
    public ProductDto() {
    }

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getArtist() {
		return artist;
	}

	public void setArtist(Integer artist) {
		this.artist = artist;
	}

	public String getEnd_date() {
		return end_date;
	}

	public void setEnd_date(String end_date) {
		this.end_date = end_date;
	}

	public String getSend_date() {
		return send_date;
	}

	public void setSend_date(String send_date) {
		this.send_date = send_date;
	}

	public String getSecond_pay_date() {
		return second_pay_date;
	}

	public void setSecond_pay_date(String second_pay_date) {
		this.second_pay_date = second_pay_date;
	}

	public Integer getPayment_type() {
		return payment_type;
	}

	public void setPayment_type(Integer payment_type) {
		this.payment_type = payment_type;
	}

	public String getLast_pay_date() {
		return last_pay_date;
	}

	public void setLast_pay_date(String last_pay_date) {
		this.last_pay_date = last_pay_date;
	}

	public Integer getProduct_status() {
		return product_status;
	}

	public void setProduct_status(Integer product_status) {
		this.product_status = product_status;
	}

	public String getPic() {
		return pic;
	}

	public void setPic(String pic) {
		this.pic = pic;
	}

	@Override
	public String toString() {
		return "ProductDto [name=" + name + ", type=" + type + ", artist=" + artist + ", end_date="
				+ end_date + ", send_date=" + send_date + ", second_pay_date=" + second_pay_date + ", payment_type="
				+ payment_type + ", last_pay_date=" + last_pay_date + ", product_status=" + product_status + ", pic=" + pic + "]";
	}

    
}
