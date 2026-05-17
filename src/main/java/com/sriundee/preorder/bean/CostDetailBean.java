package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface CostDetailBean {

    Integer getID_cost_detail();
    Integer getID_cost();
    String getc_create_date();
    Integer getID_type_cost();
    String gettc_name();
    String getc_price();
    String getc_note();
    String getc_delete();

    Integer getID_order_detail();
    Integer getID_order();
    String geto_customer_name();
    Integer getID_pro();
    String getp_name();
    Integer getID_type();
    String gett_name();
    Integer getID_art();
    String geta_name();
    Integer getID_web();
    String getw_name();
    Integer getID_ver();
    String getv_name();
    Integer getID_cover();
    String getc_name();
    String getc_price_total();
    String getc_price_pledge();
    String getc_price_balance();
    Integer getod_qty();
    String getod_price_total();
    String getod_price_pledge();
    String getod_price_balance();
    Integer getID_pay_method();
    Integer getID_order_status();
    String getos_name();
}
