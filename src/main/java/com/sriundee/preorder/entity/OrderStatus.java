package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_order_status")
@Data
public class OrderStatus {
	
    @Id
    @Column(name = "ID_order_status")
    private Integer id;

    @Column(name = "os_name")
    private String name;

    @Column(name = "os_color")
    private String color;
    
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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

}
