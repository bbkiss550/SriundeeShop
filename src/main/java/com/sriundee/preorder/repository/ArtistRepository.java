package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.ArtistBean;
import com.sriundee.preorder.model.Artist;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {
	
    @Query(value = "SELECT ID_art, a_name, ID_group, g_name, a_delete FROM q_artist WHERE a_delete = 'A'", nativeQuery = true)
    List<ArtistBean> getDataAll();
}