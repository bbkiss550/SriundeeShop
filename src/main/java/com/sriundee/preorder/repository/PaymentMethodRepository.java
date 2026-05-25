package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.PaymentMethod;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
	
    @Query(value = "SELECT * FROM t_payment_method WHERE COALESCE(pm_delete, 'A') = 'A' ORDER BY ID_pay_method ASC", nativeQuery = true)
    List<PaymentMethod> getDataAll();
}
