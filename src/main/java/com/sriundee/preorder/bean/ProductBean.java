package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface ProductBean {
	
    Integer getID_product();
    String getp_name();
    
    Integer getID_type();
    String gett_name();
    
    Integer getID_art();
    String geta_name();
    String geta_logo();
    
    String getp_end_date();
    String getp_send_date();
    String getp_last_pay_date();
    
    Integer getID_pay_type();
    String getpt_name();
    
    Integer getID_pro_status();
    String getps_name();
    
    String getp_delete();
    String getp_pic();
}