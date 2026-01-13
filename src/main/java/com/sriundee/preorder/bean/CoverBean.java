package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface CoverBean {

	Integer getID_cover();
	
	Integer getID_pro();
	String getp_name();
	
	Integer getID_type();
	String gett_name();
	
	Integer getID_web();
	String getw_name();
	
	Integer getID_ver();
	String getv_name();
	
	String getc_name();
	Double getc_price_total();
	Double getc_price_pledge();
	Double getc_price_balance();
	Double getc_price_1st();
	Double getc_price_2nd();
	Double getc_price_last();
}