package com.sriundee.preorder.service;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CostCodeService {

    private final JdbcTemplate jdbcTemplate;

    public CostCodeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public synchronized String nextCode(LocalDate recordDate) {
        String year = String.format("%02d", recordDate.getYear() % 100);
        String prefix = "CT-" + year + "-";
        Integer latestRunning = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(c_cost_code FROM 7) AS INTEGER)), 0)
                FROM t_cost
                WHERE c_cost_code LIKE ?
                """, Integer.class, prefix + "%");
        int nextRunning = (latestRunning == null ? 0 : latestRunning) + 1;
        return prefix + String.format("%06d", nextRunning);
    }
}
