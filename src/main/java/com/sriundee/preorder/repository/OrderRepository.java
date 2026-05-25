package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
			SELECT pg_advisory_xact_lock(hashtext(:prefix))
			""", nativeQuery = true)
	Long lockOrderCodeGeneration(@Param("prefix") String prefix);

	@Query(value = """
			SELECT COALESCE(MAX(CAST(SUBSTRING(o_order_code FROM 7) AS INTEGER)), 0)
			FROM t_order
			WHERE o_order_code LIKE (:prefix || '%')
			""", nativeQuery = true)
	Integer getMaxOrderCodeRunning(@Param("prefix") String prefix);

	@Query(value = """
			SELECT o.ID_order,
			       TO_CHAR(o.o_order_date, 'YYYY-MM-DD') AS o_order_date,
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
			       STRING_AGG(DISTINCT os.os_name, '||') AS order_status_names,
			       STRING_AGG(DISTINCT os.os_color, '||') AS order_status_colors
			FROM q_order o
			LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
			LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
			WHERE (:startDate IS NULL OR :startDate = '' OR o.o_order_date >= CAST(:startDate AS DATE))
			  AND (:endDate IS NULL OR :endDate = '' OR o.o_order_date <= CAST(:endDate AS DATE))
			  AND (:customerName IS NULL OR :customerName = '' OR o.o_customer_name ILIKE ('%' || :customerName || '%'))
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
			ORDER BY o.o_order_code DESC, o.ID_order DESC
			""", nativeQuery = true)
    List<OrderListBean> getOrderList(
    		@Param("startDate") String startDate,
    		@Param("endDate") String endDate,
    		@Param("customerName") String customerName,
    		@Param("payMethod") Integer payMethod,
    		@Param("orderStatus") Integer orderStatus);

	@Query(value = """
			SELECT o.ID_order,
			       TO_CHAR(o.o_order_date, 'YYYY-MM-DD') AS o_order_date,
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
			       STRING_AGG(DISTINCT os.os_name, '||') AS order_status_names,
			       STRING_AGG(DISTINCT os.os_color, '||') AS order_status_colors
			FROM q_order o
			LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
			LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
			WHERE o.ID_pay_method = 2
			  AND COALESCE(o.o_price_balance, 0) > 0
			  AND (:startDate IS NULL OR :startDate = '' OR o.o_order_date >= CAST(:startDate AS DATE))
			  AND (:endDate IS NULL OR :endDate = '' OR o.o_order_date <= CAST(:endDate AS DATE))
			  AND (:customerName IS NULL OR :customerName = '' OR o.o_customer_name ILIKE ('%' || :customerName || '%'))
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
			ORDER BY o.o_order_code DESC, o.ID_order DESC
			""", nativeQuery = true)
    List<OrderListBean> getDepositBalanceList(
    		@Param("startDate") String startDate,
    		@Param("endDate") String endDate,
    		@Param("customerName") String customerName);

	@Modifying
	@Query(value = """
			UPDATE t_order
			SET ID_pay_method = 3,
			    o_price_balance = 0
			WHERE ID_order = :ID_order
			  AND ID_pay_method = 2
			""", nativeQuery = true)
	int receiveDepositBalance(@Param("ID_order") Integer orderId);

	@Query(value = """
			SELECT o.ID_order,
			       TO_CHAR(o.o_order_date, 'YYYY-MM-DD') AS o_order_date,
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
			       STRING_AGG(DISTINCT os.os_name, '||') AS order_status_names,
			       STRING_AGG(DISTINCT os.os_color, '||') AS order_status_colors
			FROM q_order o
			LEFT JOIN t_order_detail od ON od.ID_order = o.ID_order
			LEFT JOIN t_order_status os ON os.ID_order_status = od.ID_order_status
			WHERE o.ID_order = :ID_order
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
			""", nativeQuery = true)
	OrderListBean getOrderReceipt(@Param("ID_order") Integer orderId);
}
