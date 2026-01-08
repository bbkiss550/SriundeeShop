package com.sriundee.preorder.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_type")
@Data
public class Type {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_type")
    private Integer id;

    @Column(name = "t_name")
    private String name;

    @Column(name = "t_delete")
    private String delete;

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

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}
}