package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.dto.ArtistDto;
import com.sriundee.preorder.entity.Artist;
import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;

import org.springframework.ui.Model;

@Controller
public class GroupController {

	@Autowired
	private GroupRepository groupRepository;
	
    public String getDataList() {
	    List<Group> mainGroup = groupRepository.getDataAll();
	    StringBuilder strGroup = new StringBuilder();
	    for (Group g : mainGroup) {
	    	strGroup.append("<option value='" + g.getId() + "'>" + g.getName() + "</option>");
	    }
	    
	    return strGroup.toString();
    }
}