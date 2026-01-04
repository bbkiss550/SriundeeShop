package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.sriundee.preorder.model.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
	
    @Query(value = "SELECT * FROM t_group WHERE g_delete = 'A'", nativeQuery = true)
    List<Group> getDataAll();
}