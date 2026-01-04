package com.sriundee.preorder.bean;

import java.beans.JavaBean;
import java.sql.Date;

@JavaBean
public interface ArtistBean {
	
	Integer getID_art();
    String getA_name();
    
    Integer getID_group();
    String getG_name();
    
    String getA_delete();
}
