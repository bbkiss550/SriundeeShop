package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.CostDetailBean;
import com.sriundee.preorder.entity.CostDetail;

@Repository
public interface CostDetailRepository extends JpaRepository<CostDetail, Integer> {

    @Query(value = """
            SELECT qcd.*,
                   o.ID_pay_method AS ID_pay_method
            FROM q_cost_detail qcd
            LEFT JOIN t_order o ON o.ID_order = qcd.ID_order
            WHERE qcd.ID_cost = :ID_cost
            """, nativeQuery = true)
    List<CostDetailBean> getDataByCost(@Param("ID_cost") Integer IDCost);

    @Query(value = "SELECT COUNT(*) FROM q_cost_detail WHERE ID_cost = :ID_cost", nativeQuery = true)
    Integer countDataByCost(@Param("ID_cost") Integer IDCost);

    @Query(value = "SELECT COUNT(*) FROM q_cost_detail WHERE ID_cost = :ID_cost AND (ID_order_status IS NULL OR ID_order_status <> 2)", nativeQuery = true)
    Integer countNotPressedByCost(@Param("ID_cost") Integer IDCost);

    @Query(value = "SELECT COUNT(*) FROM q_cost_detail WHERE ID_cost = :ID_cost AND (ID_order_status IS NULL OR ID_order_status <> :status)", nativeQuery = true)
    Integer countNotStatusByCost(@Param("ID_cost") Integer IDCost, @Param("status") Integer status);
}
