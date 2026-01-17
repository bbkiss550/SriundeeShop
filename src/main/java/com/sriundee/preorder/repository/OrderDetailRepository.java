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
	
	@Query(value = "SELECT * FROM q_order_detail where ID_order is null", nativeQuery = true)
    List<OrderDetailBean> getCartIsNull();

	@Query(value = "SELECT * FROM q_order_detail where ID_order_detail = :IDod", nativeQuery = true)
    List<OrderDetailBean> getCartByID(@Param("IDod") Integer IDod);
	
	@Query(value = "SELECT sum(od_price_total) as sum_price_total,sum(od_price_pledge) as sum_price_pledge,sum(od_price_balance) as sum_price_balance FROM t_order_detail where ID_order is null", nativeQuery = true)
	List<OrderSummaryBean> getCartSummary();
}