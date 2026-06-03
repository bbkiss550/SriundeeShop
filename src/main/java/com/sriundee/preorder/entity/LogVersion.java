package com.sriundee.preorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_log_version")
@Data
public class LogVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_log_version")
    private Integer id;

    @Column(name = "lv_version")
    private String version;

    @Column(name = "lv_date")
    private String date;

    @Column(name = "lv_desc")
    private String description;
}
