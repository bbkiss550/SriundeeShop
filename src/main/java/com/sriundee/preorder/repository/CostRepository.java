package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.CostPressBean;
import com.sriundee.preorder.entity.Cost;

@Repository
public interface CostRepository extends JpaRepository<Cost, Integer> {

    @Query(value = """
            SELECT *
            FROM q_cost
            WHERE ID_type_cost = 1
            AND (:startDate IS NULL OR :startDate = '' OR c_create_date >= :startDate)
            AND (:endDate IS NULL OR :endDate = '' OR c_create_date <= :endDate)
            AND (:status IS NULL OR :status = '' OR c_delete = :status)
            ORDER BY ID_cost DESC
            """, nativeQuery = true)
    List<CostPressBean> getPressCostAll(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("status") String status);

    @Query(value = """
            SELECT *
            FROM q_cost
            WHERE ID_type_cost = 2
            AND (:startDate IS NULL OR :startDate = '' OR c_create_date >= :startDate)
            AND (:endDate IS NULL OR :endDate = '' OR c_create_date <= :endDate)
            AND (:status IS NULL OR :status = '' OR c_delete = :status)
            ORDER BY ID_cost DESC
            """, nativeQuery = true)
    List<CostPressBean> getShippingCostAll(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("status") String status);
}
