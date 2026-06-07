package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface ProductOrderSummaryBean {

    Integer getID_product();
    String getw_name();
    String getv_name();
    String getc_name();
    Integer getproduct_qty();
}
