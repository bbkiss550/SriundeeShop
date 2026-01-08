package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.sriundee.preorder.entity.Menu;
import com.sriundee.preorder.repository.MenuRepository;

@Component
public class MenuController {

	@Autowired
	MenuRepository menuRepository;
	
	public String getMenuList(Integer IDmenu,Integer mIDmenu) {
	    List<Menu> mainMenus = menuRepository.getDataAll();
	    StringBuilder strMenu = new StringBuilder();
	    
	    for (Menu m : mainMenus) {
	    	if ("N".equals(m.getParent())) {
	    		String mActive = "";
	    		if(IDmenu == m.getId()) {
	    			mActive = "active";
	    		}
	    		strMenu.append("<li class='sidebar-item " + mActive + "'>");
		    	strMenu.append("<a href='" + m.getUrl() + "' class='sidebar-link'>");
		    	strMenu.append("<i class='" + m.getIcon() + "'></i>");
		    	strMenu.append("<span>" + m.getName() + "</span>");
		    	strMenu.append("</a>");
		    	strMenu.append("</li>");
	    	} else if ("Y".equals(m.getParent())) {
	    		String mActive = "";
	    		String mStyle = "style='display: none;'";
	    		if(mIDmenu == m.getId()) {
	    			mActive = "active";
	    			mStyle = "style='display: block;'";
	    		}
	    		strMenu.append("<li class='sidebar-item has-sub " + mActive + "'>");
	    		strMenu.append("<a href='#' class='sidebar-link'><i class='" + m.getIcon() + "'></i><span>" + m.getName() + "</span></a>");
		    	strMenu.append("<ul class='submenu'"  + mStyle + ">");
		    	List<Menu> subMenus = menuRepository.getMenuParent(m.getId());
		    	for (Menu s : subMenus) {
		    		String sActive = "";
		    		if(IDmenu == s.getIdmenu()) {
		    			sActive = "active";
		    		}
		    		strMenu.append("<li class='submenu-item'><a href='" + s.getUrl() + "'>" + s.getName() + "</a></li>");
		    	}
		    	strMenu.append("</ul>");
		    	strMenu.append("</li>");
	    	}
	    }
	    
	    return strMenu.toString();
	}
}
