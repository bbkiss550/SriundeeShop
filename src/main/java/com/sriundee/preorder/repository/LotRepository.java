package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.LotBean;
import com.sriundee.preorder.entity.Lot;

@Repository
public interface LotRepository extends JpaRepository<Lot, Integer> {

    @Query(value = "SELECT * FROM t_lot WHERE l_lot_number = :lotNumber AND l_delete = 'A' LIMIT 1", nativeQuery = true)
    Lot getActiveByLotNumber(@Param("lotNumber") String lotNumber);

    @Query(value = """
            SELECT l.ID_lot,
                   l.l_lot_number,
                   l.l_create_date,
                   l.l_delete,
                   COUNT(ld.ID_lot_detail) AS detail_count
            FROM t_lot l
            LEFT JOIN t_lot_detail ld ON ld.ID_lot = l.ID_lot
            WHERE (:startDate IS NULL OR :startDate = '' OR l.l_create_date >= :startDate)
              AND (:endDate IS NULL OR :endDate = '' OR l.l_create_date <= :endDate)
              AND (:status IS NULL OR :status = '' OR l.l_delete = :status)
            GROUP BY l.ID_lot, l.l_lot_number, l.l_create_date, l.l_delete
            ORDER BY l.ID_lot DESC
            """, nativeQuery = true)
    List<LotBean> getLotAll(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("status") String status);
}
