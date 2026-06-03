package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface CostPressBean {

    Integer getID_cost();
    String getc_cost_code();
    String getc_create_date();
    Integer getID_type_cost();
    String gettc_name();
    String getc_price();
    String getc_note();
    String getc_delete();
}
