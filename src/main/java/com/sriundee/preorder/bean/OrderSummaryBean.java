package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface OrderSummaryBean {
	
    double getsum_price_total();
    double getsum_price_pledge();
    double getsum_price_balance();
}