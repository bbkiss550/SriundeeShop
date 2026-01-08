package com.sriundee.preorder.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_artist")
@Data
public class Artist {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_art")
    private Integer id;

    @Column(name = "a_name")
    private String name;

    @Column(name = "ID_group")
    private Integer group;

    @Column(name = "a_delete")
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

	public Integer getGroup() {
		return group;
	}

	public void setGroup(Integer group) {
		this.group = group;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}
}