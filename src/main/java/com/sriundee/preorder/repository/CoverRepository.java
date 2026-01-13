package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.CoverBean;
import com.sriundee.preorder.entity.Cover;

@Repository
public interface CoverRepository extends JpaRepository<Cover, Integer> {
	
	@Query(value = "SELECT * FROM q_cover WHERE c_delete = 'A' AND ID_pro = :ID_pro", nativeQuery = true)
    List<CoverBean> getDataAll(@Param("ID_pro") Integer IDproduct);
}