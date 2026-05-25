package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_menu")
@Data
public class Menu {
    @Id
    @Column(name = "ID_menu")
    private Integer id;

    @Column(name = "m_name")
    private String name;

    @Column(name = "m_parent")
    private String parent;

    @Column(name = "m_ID_menu")
    private Integer idmenu;
    
    @Column(name = "m_url")
    private String url;

    @Column(name = "m_icon")
    private String icon;

    @Column(name = "m_order")
    private Integer order;

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

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public Integer getIdmenu() {
		return idmenu;
	}

	public void setIdmenu(Integer idmenu) {
		this.idmenu = idmenu;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}
}
