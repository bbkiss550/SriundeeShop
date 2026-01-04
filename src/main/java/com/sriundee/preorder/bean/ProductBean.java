package com.sriundee.preorder.bean;

import java.beans.JavaBean;
import java.sql.Date;

@JavaBean
public interface ProductBean {
	
    Integer getID_product();
    String getP_name();
    
    Integer getID_type();
    String getT_name();
    
    Integer getID_art();
    String getA_name();
    
    String getP_end_date();
    String getP_send_date();
    String getP_second_pay_date();
    String getP_last_pay_date();
    
    Integer getID_pay_type();
    String getPt_name();
    
    Integer getID_pro_status();
    String getPs_name();
    
    String getP_delete();
    String getP_pic();
}