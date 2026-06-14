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
public class RecordKodController {

    @Autowired
    private MenuController menuService;

    @Autowired
    private ArtistController artistController;

    @Autowired
    private WebsiteController websiteController;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @GetMapping("/recordkod")
    public String index(Model model) {
        String menuList = menuService.getMenuList(9, 23);
        model.addAttribute("mainMenus", menuList);

        String listArtist = artistController.getDataList();
        model.addAttribute("listArtist", listArtist);

        String listWebsite = websiteController.getDataList();
        model.addAttribute("listWebsite", listWebsite);

        StringBuilder listDetail = buildOrderDetailTable(null, null, null);
        model.addAttribute("listOrderDetail", listDetail);

        return "recordkod";
    }

    @GetMapping("/recordkod/search")
    @ResponseBody
    public String search(
            @RequestParam(value = "artistId", required = false) Integer artistId,
            @RequestParam(value = "websiteId", required = false) Integer websiteId,
            @RequestParam(value = "customerName", required = false) String customerName) {

        StringBuilder listDetail = buildOrderDetailTable(artistId, websiteId, customerName);
        return listDetail.toString();
    }

    private StringBuilder buildOrderDetailTable(Integer artistId, Integer websiteId, String customerName) {
        StringBuilder listDetail = new StringBuilder();

        String custName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : null;
        List<OrderDetailBean> orderdetailList = orderDetailRepository.getDataByAllFilter(artistId, websiteId, custName);

        for (OrderDetailBean od : orderdetailList) {
            String safeCustomer = esc(od.geto_customer_name());
            String safeProduct = esc(od.getp_name());
            String safeWeb = esc(od.getw_name());
            String safeVersion = esc(od.getv_name());
            String safeCover = esc(od.getc_name());
            String safeStatus = esc(od.getos_name());
            String safeColor = esc(od.getos_color());

            listDetail.append("<tr>");
            // Checkbox พร้อม data-attributes เก็บข้อมูลทุก field
            listDetail.append("<td>"
                    + "<input type='checkbox' class='form-check-input row-check' id='check_" + od.getID_order_detail()
                    + "'"
                    + " value='" + od.getID_order_detail() + "'"
                    + " data-customer='" + safeCustomer + "'"
                    + " data-product='" + safeProduct + "'"
                    + " data-web='" + safeWeb + "'"
                    + " data-version='" + safeVersion + "'"
                    + " data-cover='" + safeCover + "'"
                    + " data-qty='" + od.getod_qty() + "'"
                    + " data-status='" + safeStatus + "'"
                    + " data-color='" + safeColor + "'"
                    + "></td>");
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

    /** Escape single quotes สำหรับ HTML attribute */
    private String esc(String s) {
        if (s == null)
            return "";
        return s.replace("'", "&#39;");
    }
}
