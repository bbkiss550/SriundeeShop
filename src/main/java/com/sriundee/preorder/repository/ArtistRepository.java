package com.sriundee.preorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.bean.ArtistBean;
import com.sriundee.preorder.entity.Artist;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {
	
    @Query(value = "SELECT * FROM q_artist WHERE a_delete = 'A' ORDER BY ID_art ASC", nativeQuery = true)
    List<ArtistBean> getDataAll();

    @Query(value = "SELECT * FROM q_artist WHERE a_delete = 'A' ORDER BY a_name ASC, ID_art ASC", nativeQuery = true)
    List<ArtistBean> getDropdownData();
}
