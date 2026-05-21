package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface LotBean {

    Integer getID_lot();
    String getl_lot_number();
    String getl_create_date();
    String getl_start_date();
    String getl_end_date();
    String getl_arrive_date();
    String getl_delete();
    Integer getdetail_count();
}
