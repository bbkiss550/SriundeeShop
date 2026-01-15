package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Version;

@Repository
public interface VersionRepository extends JpaRepository<Version, Integer> {
	
    @Query(value = "SELECT * FROM t_version WHERE v_delete = 'A' AND ID_pro = :ID_pro", nativeQuery = true)
    List<Version> getDataByID_pro(@Param("ID_pro") Integer IDproduct);
}