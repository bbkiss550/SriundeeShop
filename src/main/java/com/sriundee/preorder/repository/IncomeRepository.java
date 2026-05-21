package com.sriundee.preorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer> {
}
