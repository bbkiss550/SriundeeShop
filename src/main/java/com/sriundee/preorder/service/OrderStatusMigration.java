package com.sriundee.preorder.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusMigration {
    private final JdbcTemplate jdbc;

    public OrderStatusMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void migrate() {
        try {
            jdbc.execute(new ClassPathResource("db/order-status-thailand.sql")
                    .getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read order status migration", e);
        }
    }
}
