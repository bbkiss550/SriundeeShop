package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.LotDetailBean;
import com.sriundee.preorder.entity.LotDetail;

@Repository
public interface LotDetailRepository extends JpaRepository<LotDetail, Integer> {

    List<LotDetail> findByLot(Integer lot);

    Integer countByLot(Integer lot);

    @Query(value = """
            SELECT ld.ID_lot_detail,
                   l.ID_lot,
                   l.l_lot_number,
                   l.l_create_date,
                   l.l_delete,
                   q.ID_order_detail,
                   q.ID_order,
                   q.o_customer_name,
                   q.ID_pro,
                   q.p_name,
                   q.ID_type,
                   q.t_name,
                   q.ID_art,
                   q.a_name,
                   q.ID_web,
                   q.w_name,
                   q.ID_ver,
                   q.v_name,
                   q.ID_cover,
                   q.c_name,
                   q.od_price_total,
                   q.od_price_pledge,
                   q.od_price_balance,
                   q.od_qty,
                   o.ID_pay_method,
                   q.ID_order_status,
                   q.os_name
            FROM t_lot_detail ld
            JOIN t_lot l ON l.ID_lot = ld.ID_lot
            JOIN q_order_detail q ON q.ID_order_detail = ld.ID_order_detail
            LEFT JOIN t_order o ON o.ID_order = q.ID_order
            WHERE ld.ID_lot = :ID_lot
            ORDER BY ld.ID_lot_detail ASC
            """, nativeQuery = true)
    List<LotDetailBean> getDataByLot(@Param("ID_lot") Integer IDLot);
}
