package com.sriundee.preorder.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.repository.OrderDetailRepository;

@Controller
public class ChangeController {

	@Autowired
	private MenuController menuService;

	@Autowired
	private OrderStatusController orderStatusController;

	@Autowired
	private ArtistController artistController;

	@Autowired
	private WebsiteController websiteController;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@GetMapping("/liststatus")
	public String index(Model model) {
		String menuList = menuService.getMenuList(8, null);
		model.addAttribute("mainMenus", menuList);

		// Dropdown สถานะ (value = ชื่อสถานะ เพื่อ filter ตรงกับ getos_name())
		String listStatus = orderStatusController.getDataListByName();
		model.addAttribute("listStatus", listStatus);

		// Dropdown ศิลปิน
		String listArtist = artistController.getDataList();
		model.addAttribute("listArtist", listArtist);

		// Dropdown เว็บ
		String listWebsite = websiteController.getDataList();
		model.addAttribute("listWebsite", listWebsite);

		StringBuilder listDetail = buildOrderDetailTable(null, null, null, null);
		model.addAttribute("listOrderDeatil", listDetail);

		return "liststatus";
	}

	@GetMapping("/liststatus/search")
	@ResponseBody
	public String search(
			@RequestParam(value = "artistId", required = false) Integer artistId,
			@RequestParam(value = "websiteId", required = false) Integer websiteId,
			@RequestParam(value = "statusName", required = false) String statusName,
			@RequestParam(value = "customerName", required = false) String customerName) {

		StringBuilder listDetail = buildOrderDetailTable(artistId, websiteId, statusName, customerName);
		return listDetail.toString();
	}

	private StringBuilder buildOrderDetailTable(Integer artistId, Integer websiteId,
			String statusName, String customerName) {
		StringBuilder listDetail = new StringBuilder();

		// ใช้ query เดียวรองรับทุก filter (customerName ทำ LIKE ใน SQL)
		String custName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : null;
		List<OrderDetailBean> orderdetailList = orderDetailRepository.getDataByAllFilter(artistId, websiteId, custName);

		for (OrderDetailBean od : orderdetailList) {
			// กรองสถานะฝั่ง Java (ถ้ามีการเลือก)
			if (statusName != null && !statusName.isEmpty() && !od.getos_name().equals(statusName)) {
				continue;
			}
			listDetail.append("<tr>");
			listDetail.append("<td>" + od.geto_customer_name() + "</td>");
			listDetail.append("<td>" + od.getp_name() + "</td>");
			listDetail.append("<td>" + od.getw_name() + "</td>");
			listDetail.append("<td>" + od.getv_name() + "</td>");
			listDetail.append("<td>" + od.getc_name() + "</td>");
			listDetail.append("<td>" + od.getod_qty() + "</td>");
			listDetail.append("<td>");
			listDetail.append("<button type='button' class='btn btn-outline-" + od.getos_color() + " btn-sm'>");
			listDetail.append(od.getos_name());
			listDetail.append("</button>");
			listDetail.append("</td>");
			listDetail.append("</tr>");
		}

		return listDetail;
	}
}