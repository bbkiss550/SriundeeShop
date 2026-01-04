package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.sriundee.preorder.model.Group;
import com.sriundee.preorder.model.PaymentType;
import com.sriundee.preorder.model.ProductStatus;

@Repository
public interface ProductStatusRepository extends JpaRepository<ProductStatus, Integer> {
	
    @Query(value = "SELECT * FROM t_product_status", nativeQuery = true)
    List<ProductStatus> getDataAll();
}