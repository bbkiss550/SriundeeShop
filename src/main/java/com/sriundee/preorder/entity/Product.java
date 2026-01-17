package com.sriundee.preorder.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_product")
@Data
public class Product {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_product")
    private Integer id;

    @Column(name = "p_name")
    private String name;

    @Column(name = "ID_type")
    private Integer type;
    
    @Column(name = "ID_art")
    private Integer artist;
    
    @Column(name = "p_end_date")
    private Date end_date;

    @Column(name = "p_send_date")
    private Date send_date;
    
    @Column(name = "ID_pay_type")
    private Integer payment_type;
    
    @Column(name = "p_last_pay_date")
    private Date last_pay_date;

    @Column(name = "ID_pro_status")
    private Integer product_status;

    @Column(name = "p_delete")
    private String delete;

    @Column(name = "p_pic")
    private String pic;

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

	public Date getEnd_date() {
		return end_date;
	}

	public void setEnd_date(Date end_date) {
		this.end_date = end_date;
	}

	public Date getSend_date() {
		return send_date;
	}

	public void setSend_date(Date send_date) {
		this.send_date = send_date;
	}

	public Integer getPayment_type() {
		return payment_type;
	}

	public void setPayment_type(Integer payment_type) {
		this.payment_type = payment_type;
	}

	public Date getLast_pay_date() {
		return last_pay_date;
	}

	public void setLast_pay_date(Date last_pay_date) {
		this.last_pay_date = last_pay_date;
	}

	public Integer getProduct_status() {
		return product_status;
	}

	public void setProduct_status(Integer product_status) {
		this.product_status = product_status;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

	public String getPic() {
		return pic;
	}

	public void setPic(String pic) {
		this.pic = pic;
	}
}
