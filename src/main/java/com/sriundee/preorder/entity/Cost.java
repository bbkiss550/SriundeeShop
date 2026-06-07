package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_cost")
@Data
public class Cost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_cost")
    private Integer id;

    @Column(name = "c_create_date")
    private String create_date;

    @Column(name = "c_cost_code")
    private String cost_code;

    @Column(name = "ID_type_cost")
    private Integer type_cost;

    @Column(name = "c_price")
    private String price;

    @Column(name = "c_note")
    private String note;

    @Column(name = "c_delete")
    private String delete;

    @Column(name = "id_active_status")
    private String active_status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCreate_date() {
        return create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public String getCost_code() {
        return cost_code;
    }

    public void setCost_code(String cost_code) {
        this.cost_code = cost_code;
    }

    public Integer getType_cost() {
        return type_cost;
    }

    public void setType_cost(Integer type_cost) {
        this.type_cost = type_cost;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDelete() {
        return delete;
    }

    public void setDelete(String delete) {
        this.delete = delete;
    }

    public String getActive_status() {
        return active_status;
    }

    public void setActive_status(String active_status) {
        this.active_status = active_status;
    }

    @PrePersist
    public void prePersist() {
        if (active_status == null || active_status.isBlank()) {
            active_status = "A";
        }
    }
}
