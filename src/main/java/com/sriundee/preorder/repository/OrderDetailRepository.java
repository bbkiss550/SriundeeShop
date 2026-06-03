package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.OrderDetailBean;
import com.sriundee.preorder.bean.OrderSummaryBean;
import com.sriundee.preorder.entity.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
	
	@Query(value = "SELECT * FROM q_order_detail WHERE ID_order IS NULL AND ID_user = :ID_user", nativeQuery = true)
    List<OrderDetailBean> getCartIsNull(@Param("ID_user") Integer userId);

	@Query(value = "SELECT * FROM q_order_detail where ID_order_detail = :IDod", nativeQuery = true)
    List<OrderDetailBean> getCartByID(@Param("IDod") Integer IDod);
	
	@Query(value = "SELECT COALESCE(sum(od_price_total), 0) as sum_price_total,COALESCE(sum(od_price_pledge), 0) as sum_price_pledge,COALESCE(sum(od_price_balance), 0) as sum_price_balance FROM t_order_detail WHERE ID_order IS NULL AND ID_user = :ID_user", nativeQuery = true)
	List<OrderSummaryBean> getCartSummary(@Param("ID_user") Integer userId);
	
	@Query(value = "SELECT * FROM t_order_detail WHERE ID_order IS NULL AND ID_user = :ID_user", nativeQuery = true)
    List<OrderDetail> getDataIsNull(@Param("ID_user") Integer userId);

	List<OrderDetail> findByOrder(Integer orderId);
	
	@Query(value = "SELECT * FROM q_order_detail where ID_Order IS NOT NULL", nativeQuery = true)
    List<OrderDetailBean> getDataByIDOrderIsNull();

	@Query(value = """
			SELECT q.*, o.ID_pay_method AS ID_pay_method, os.os_color AS os_color
			FROM q_order_detail q
			LEFT JOIN t_order o ON o.ID_order = q.ID_order
			LEFT JOIN t_order_status os ON os.ID_order_status = q.ID_order_status
			WHERE q.ID_order = :ID_order
			ORDER BY q.ID_order_detail
			""", nativeQuery = true)
	List<OrderDetailBean> getDataByOrder(@Param("ID_order") Integer orderId);

	@Query(value = """
			SELECT q.*, os.os_color AS os_color
			FROM q_order_detail q
			JOIN t_order_status os ON os.ID_order_status = q.ID_order_status
			WHERE q.ID_Order IS NOT NULL
			AND (:ID_order_status IS NULL OR q.ID_order_status = :ID_order_status)
			AND (:a_name IS NULL OR :a_name = '' OR q.a_name ILIKE ('%' || :a_name || '%'))
			AND (:ID_web IS NULL OR q.ID_web = :ID_web)
			AND (:lot_number IS NULL OR :lot_number = '' OR EXISTS (
				SELECT 1
				FROM t_lot_detail ld
				JOIN t_lot l ON l.ID_lot = ld.ID_lot
				WHERE ld.ID_order_detail = q.ID_order_detail
				  AND l.l_delete = 'A'
				  AND l.l_lot_number ILIKE ('%' || :lot_number || '%')
			))
			""", nativeQuery = true)
	List<OrderDetailBean> getDataByFilter(
			@Param("ID_order_status") Integer IDOrderStatus,
			@Param("a_name") String artistName,
			@Param("ID_web") Integer websiteId,
			@Param("lot_number") String lotNumber);

	@Query(value = """
			SELECT q.*, os.os_color AS os_color
			FROM q_order_detail q
			JOIN t_order_status os ON os.ID_order_status = q.ID_order_status
			WHERE q.ID_Order IS NOT NULL
			AND (:ID_art IS NULL OR q.ID_art = :ID_art)
			AND (:ID_web IS NULL OR q.ID_web = :ID_web)
			AND (:customer_name IS NULL OR :customer_name = '' OR q.o_customer_name ILIKE ('%' || :customer_name || '%'))
			ORDER BY q.ID_order_detail DESC
			""", nativeQuery = true)
	List<OrderDetailBean> getDataByAllFilter(
			@Param("ID_art") Integer artistId,
			@Param("ID_web") Integer websiteId,
			@Param("customer_name") String customerName);
}
