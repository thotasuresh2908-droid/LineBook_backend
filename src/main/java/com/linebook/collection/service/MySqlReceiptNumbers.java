package com.linebook.collection.service;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One counter row per day, bumped with a single atomic statement (the classic MySQL LAST_INSERT_ID sequence idiom).
 * The row lock is held until the surrounding transaction ends, so two collections on the same day take turns and can
 * never be given the same receipt number. Uses the caller's transaction (JdbcTemplate shares the JPA connection).
 */
@Component
public class MySqlReceiptNumbers implements ReceiptNumbers {

    private static final String BUMP = """
            INSERT INTO receipt_counters (receipt_date, last_seq) VALUES (?, LAST_INSERT_ID(1))
            ON DUPLICATE KEY UPDATE last_seq = LAST_INSERT_ID(last_seq + 1)
            """;

    private final JdbcTemplate jdbc;

    public MySqlReceiptNumbers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long next(LocalDate day) {
        jdbc.update(BUMP, Date.valueOf(day));
        Long seq = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (seq == null || seq < 1) {
            throw new IllegalStateException("Could not allocate a receipt number for " + day);
        }
        return seq;
    }
}
