package com.sriundee.preorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer> {
    @Modifying
    @Query(value = "DELETE FROM t_income WHERE ID_order = :ID_order", nativeQuery = true)
    int deleteByOrderId(@Param("ID_order") Integer orderId);
}
