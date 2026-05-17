package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.CustomerNameBean;
import com.sriundee.preorder.bean.GroupWebsiteBean;
import com.sriundee.preorder.bean.OrderListBean;
import com.sriundee.preorder.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
	
	@Query(value = "SELECT * FROM q_group_website WHERE c_delete = 'A' AND ID_pro = :ID_pro", nativeQuery = true)
    List<GroupWebsiteBean> getDataByID_pro(@Param("ID_pro") Integer IDproduct);
	
	@Query(value = "SELECT * FROM q_customer_name ORDER BY o_customer_name", nativeQuery = true)
    List<CustomerNameBean> getCsutomerList();

	@Query(value = """
			SELECT COALESCE(MAX(CAST(SUBSTRING(o_order_code, 7) AS UNSIGNED)), 0)
			FROM t_order
			WHERE o_order_code LIKE CONCAT(:prefix, '%')
			""", nativeQuery = true)
	Integer getMaxOrderCodeRunning(@Param("prefix") String prefix);

	@Query(value = """
			SELECT o.ID_order,
			       DATE_FORMAT(o.o_order_date, '%Y-%m-%d') AS o_order_date,
			       o.o_order_code,
			       o.o_customer_name,
			       o.ID_pay_method,
			       o.pm_name,
			       o.o_send_cost,
			       o.o_discount,
			       o.o_price_total,
			       o.o_price_pledge,
			       o.o_price_balance,
			       o.o_net,
			       o.o_remark,
			       GROUP_CONCAT(DISTINCT os.os_name ORDER BY os.ID_order_status SEPARATOR '||') AS order_status_names,
			       GROUP_CONCAT(DISTINCT os.os_color ORDER BY os.ID_order_status SEPARATOR '||') AS order_status_colors
			FROM q_order o
			LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
			LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
			WHERE (:orderDate IS NULL OR :orderDate = '' OR o.o_order_date = :orderDate)
			  AND (:customerName IS NULL OR :customerName = '' OR o.o_customer_name LIKE CONCAT('%', :customerName, '%'))
			  AND (:payMethod IS NULL OR o.ID_pay_method = :payMethod)
			  AND (:orderStatus IS NULL OR EXISTS (
			      SELECT 1
			      FROM t_order_detail od_filter
			      WHERE od_filter.ID_order = o.ID_order
			        AND od_filter.ID_order_status = :orderStatus
			  ))
			GROUP BY o.ID_order,
			         o.o_order_date,
			         o.o_order_code,
			         o.o_customer_name,
			         o.ID_pay_method,
			         o.pm_name,
			         o.o_send_cost,
			         o.o_discount,
			         o.o_price_total,
			         o.o_price_pledge,
			         o.o_price_balance,
			         o.o_net,
			         o.o_remark
			ORDER BY o.o_order_date DESC, o.ID_order DESC
			""", nativeQuery = true)
    List<OrderListBean> getOrderList(
    		@Param("orderDate") String orderDate,
    		@Param("customerName") String customerName,
    		@Param("payMethod") Integer payMethod,
    		@Param("orderStatus") Integer orderStatus);
}
