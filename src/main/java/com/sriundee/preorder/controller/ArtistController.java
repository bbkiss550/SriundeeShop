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

import com.sriundee.preorder.bean.ArtistBean;
import com.sriundee.preorder.dto.ArtistDto;
import com.sriundee.preorder.model.Artist;
import com.sriundee.preorder.model.Group;
import com.sriundee.preorder.model.Type;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;

import org.springframework.ui.Model;

@Controller
public class ArtistController {

	@Autowired
    private MenuController menuService;
	
	@Autowired
	private ArtistRepository artistRepository;
	
	@Autowired
	private GroupController groupController;
	
    @GetMapping("/artist")
    public String index(Model model) {
		String menuList = menuService.getMenuList(3,2);
	    model.addAttribute("mainMenus", menuList);
	    
		List<ArtistBean> artistList = artistRepository.getDataAll();
		StringBuilder strArtist = new StringBuilder();
		Integer row_id = 0;
		for (ArtistBean a : artistList) {
			row_id +=1;
			strArtist.append("<tr>");
			strArtist.append("<td>" + row_id + "</td>");
			strArtist.append("<td>" + a.getA_name() + "</td>");
			strArtist.append("<td>" + a.getG_name() + "</td>");
			strArtist.append("<td><div class='buttons'><a class='btn icon btn-warning' onclick='edit_data(" + a.getID_art() + ")'><i data-feather='edit'></i></a></div></td>");
			strArtist.append("<td><div class='buttons'><a class='btn icon btn-danger' onclick='delete_data(" + a.getID_art() + ")'><i data-feather='trash-2'></i></a></div></td>");
			strArtist.append("</tr>");
		}
	    model.addAttribute("mainArtist", strArtist);

	    String strGroup = groupController.getDataList();
	    model.addAttribute("ListGroup", strGroup);
	    
        return "manage/artist";
    }
    
    @PostMapping("/manage/artist/save")
    @ResponseBody
    public ResponseEntity<String> saveData(@RequestBody ArtistDto artistDto) {
        try {
            Artist artist = new Artist();
            artist.setName(artistDto.getArtistName());
            artist.setGroup(Integer.parseInt(artistDto.getGroupId()));
            artist.setDelete("A");

            artistRepository.save(artist);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error");
        }
    }
    
    @GetMapping("/manage/artist/get/{id}")
    @ResponseBody
    public ResponseEntity<Artist> getDataById(@PathVariable Integer id) {
        return artistRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/manage/artist/update/{id}")
    @ResponseBody
    public ResponseEntity<String> updateData(@PathVariable Integer id, @RequestBody ArtistDto artistDto) {
        try {
            Artist artist = artistRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลศิลปิน"));
            artist.setName(artistDto.getArtistName());
            artist.setGroup(Integer.parseInt(artistDto.getGroupId()));
            
            artistRepository.save(artist);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/manage/artist/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteData(@PathVariable Integer id) {
        try {
            Artist artist = artistRepository.findById(id).orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลศิลปิน"));
            artist.setDelete("D");
            
            artistRepository.save(artist);
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    public String getDataList() {
	    List<ArtistBean> mainArtist = artistRepository.getDataAll();
	    StringBuilder strArtist = new StringBuilder();
	    for (ArtistBean a : mainArtist) {
	    	strArtist.append("<option value='" + a.getID_art() + "'>" + a.getA_name() + "</option>");
	    }
	    
	    return strArtist.toString();
    }
}