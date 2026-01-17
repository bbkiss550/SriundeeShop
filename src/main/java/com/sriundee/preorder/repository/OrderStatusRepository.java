package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.entity.OrderStatus;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Integer> {
	
    @Query(value = "SELECT * FROM t_order_status", nativeQuery = true)
    List<OrderStatus> getDataAll();
}