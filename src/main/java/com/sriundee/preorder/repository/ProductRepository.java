package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.bean.ProductOrderSummaryBean;
import com.sriundee.preorder.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	@Query(value = "SELECT * FROM q_product WHERE p_delete = 'A' ORDER BY ID_product DESC", nativeQuery = true)
	List<ProductBean> getDataAll();

	@Query(value = "SELECT * FROM q_product WHERE p_delete = 'A' AND ID_product = :ID_product", nativeQuery = true)
	List<ProductBean> getDataAllByID(@Param("ID_product") Integer ID_pro);

	@Query(value = """
			SELECT *
			FROM q_product
			WHERE p_delete = 'A'
			ORDER BY
			    CASE ID_pro_status WHEN 1 THEN 1 WHEN 2 THEN 2 ELSE 3 END,
			    TO_DATE(NULLIF(p_end_date, ''), 'DD/MM/YYYY') NULLS FIRST,
			    ID_product DESC
			""", nativeQuery = true)
	List<ProductBean> getDataOrder();

	@Query(value = """
			SELECT *
			FROM q_product
			WHERE p_delete = 'A'
			ORDER BY
			    COALESCE(TO_DATE(NULLIF(p_end_date, ''), 'DD/MM/YYYY'), TO_DATE(NULLIF(p_send_date, ''), 'DD/MM/YYYY')) NULLS LAST,
			    ID_product DESC
			""", nativeQuery = true)
	List<ProductBean> getScheduleProducts();

	@Query(value = """
			SELECT p.ID_product,
			       w.w_name,
			       v.v_name,
			       c.c_name,
			       COALESCE(SUM(od.od_qty), 0) AS product_qty
			FROM t_product p
			JOIN t_cover c ON c.ID_pro = p.ID_product
			JOIN t_website w ON w.ID_web = c.ID_web
			JOIN t_version v ON v.ID_ver = c.ID_ver
			JOIN t_order_detail od ON od.ID_cover = c.ID_cover
			JOIN t_order o ON o.ID_order = od.ID_order
			WHERE p.p_delete = 'A'
			  AND p.ID_product = :ID_product
			  AND COALESCE(c.c_delete, 'A') = 'A'
			  AND COALESCE(o.id_active_status, 'A') = 'A'
			GROUP BY p.ID_product, w.w_name, v.v_name, c.c_name
			ORDER BY w.w_name, v.v_name, c.c_name
			""", nativeQuery = true)
	List<ProductOrderSummaryBean> getOrderSummaryByProduct(@Param("ID_product") Integer ID_product);
}
