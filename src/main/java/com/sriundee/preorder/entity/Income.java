package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_income")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_income")
    private Integer id;

    @Column(name = "c_create_date")
    private String createDate;

    @Column(name = "c_customer_name")
    private String customerName;

    @Column(name = "ID_type_income")
    private Integer typeIncome;

    @Column(name = "c_price")
    private String price;

    @Column(name = "c_note")
    private String note;

    @Column(name = "c_delete")
    private String delete;

    @Column(name = "ID_order")
    private Integer order;

    @Column(name = "ID_sale")
    private Integer sale;

    @Column(name = "id_active_status")
    private String activeStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getTypeIncome() {
        return typeIncome;
    }

    public void setTypeIncome(Integer typeIncome) {
        this.typeIncome = typeIncome;
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getSale() {
        return sale;
    }

    public void setSale(Integer sale) {
        this.sale = sale;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    @PrePersist
    public void prePersist() {
        if (activeStatus == null || activeStatus.isBlank()) {
            activeStatus = "A";
        }
    }
}
