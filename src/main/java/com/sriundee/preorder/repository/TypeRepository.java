package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Type;

@Repository
public interface TypeRepository extends JpaRepository<Type, Integer> {
	
    @Query(value = "SELECT * FROM t_type WHERE t_delete = 'A' ORDER BY ID_type ASC", nativeQuery = true)
    List<Type> getDataAll();

    @Query(value = "SELECT * FROM t_type WHERE t_delete = 'A' ORDER BY t_name ASC, ID_type ASC", nativeQuery = true)
    List<Type> getDropdownData();
}
