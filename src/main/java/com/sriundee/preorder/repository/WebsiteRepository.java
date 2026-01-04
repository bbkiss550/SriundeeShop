package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.model.Website;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, Integer> {
	
    @Query(value = "SELECT * FROM t_website WHERE w_delete = 'A'", nativeQuery = true)
    List<Website> getDataAll();
}