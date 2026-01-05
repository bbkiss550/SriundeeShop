package com.sriundee.preorder.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_version")
@Data
public class Version {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ver")
    private Integer id;

    @Column(name = "ID_pro")
    private Integer product;
    
    @Column(name = "v_name")
    private String name;

    @Column(name = "v_delete")
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}
}