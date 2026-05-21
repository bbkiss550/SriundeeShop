package com.sriundee.preorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.LogVersion;

@Repository
public interface LogVersionRepository extends JpaRepository<LogVersion, Integer> {

    LogVersion findFirstByOrderByIdDesc();
}
