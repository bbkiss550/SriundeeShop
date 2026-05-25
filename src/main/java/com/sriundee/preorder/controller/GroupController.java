package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.repository.GroupRepository;

@Controller
public class GroupController {

	@Autowired
	private GroupRepository groupRepository;
	
    public String getDataList() {
	    List<Group> mainGroup = groupRepository.getDropdownData();
	    StringBuilder strGroup = new StringBuilder();
	    for (Group g : mainGroup) {
	    	strGroup.append("<option value='" + g.getId() + "'>" + g.getName() + "</option>");
	    }
	    
	    return strGroup.toString();
    }
}
