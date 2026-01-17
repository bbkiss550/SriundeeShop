package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.Menu;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {
	
    @Query(value = "SELECT ID_menu ,m_name ,m_parent ,m_ID_menu ,m_url ,m_icon FROM t_menu WHERE m_ID_menu IS NULL", nativeQuery = true)
    List<Menu> getDataAll();

    @Query(value = "SELECT ID_menu ,m_name ,m_parent ,m_ID_menu ,m_url ,m_icon FROM t_menu WHERE m_ID_menu = :ID_menu", nativeQuery = true)
    List<Menu> getMenuParent(@Param("ID_menu") Integer mParent);
}