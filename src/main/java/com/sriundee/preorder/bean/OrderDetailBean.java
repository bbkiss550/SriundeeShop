package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface OrderDetailBean {
	
    Integer getID_order_detail();
    Integer getID_order();
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
    String getc_price_1st();
    String getc_price_2nd();
    String getc_price_last();

    Integer getod_qty();
    String getod_price_total();
    String getod_price_pledge();
    String getod_price_balance();
    String getod_price_1st();
    String getod_price_2nd();
    String getod_price_last();

    Integer getID_pay_type();
    String getp_second_pay_date();
    String getp_last_pay_date();
    Integer getID_order_status();
    String getos_name();
}