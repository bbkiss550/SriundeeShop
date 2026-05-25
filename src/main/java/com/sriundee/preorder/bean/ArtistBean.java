package com.sriundee.preorder.bean;

import java.beans.JavaBean;

@JavaBean
public interface ArtistBean {
	
	Integer getID_art();
    String geta_name();
    
    Integer getID_group();
    String getg_name();
    
    String geta_logo();
    String geta_delete();
}
