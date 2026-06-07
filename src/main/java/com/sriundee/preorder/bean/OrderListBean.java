package com.sriundee.preorder.bean;

public interface OrderListBean {
    Integer getID_order();
    String geto_order_date();
    String geto_order_code();
    String geto_customer_name();
    Integer getID_pay_method();
    String getpm_name();
    String geto_send_cost();
    String geto_discount();
    String geto_price_total();
    String geto_price_pledge();
    String geto_price_balance();
    String geto_net();
    String geto_remark();
    String getid_active_status();
    String getorder_status_names();
    String getorder_status_colors();
}
