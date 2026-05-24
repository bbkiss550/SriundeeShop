package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	@Query(value = "SELECT * FROM q_product WHERE p_delete = 'A'", nativeQuery = true)
	List<ProductBean> getDataAll();

	@Query(value = "SELECT * FROM q_product WHERE p_delete = 'A' AND ID_product = :ID_product", nativeQuery = true)
	List<ProductBean> getDataAllByID(@Param("ID_product") Integer ID_pro);

	@Query(value = "SELECT * FROM q_product WHERE p_delete = 'A' ORDER BY ID_pro_status ASC, ID_product DESC", nativeQuery = true)
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
}
