package com.campus.twohand.order.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderReviewMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderReviewMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureColumn(
                    "buyer_rate",
                    "ALTER TABLE orders ADD COLUMN buyer_rate VARCHAR(20) NULL COMMENT '买家评价等级' AFTER finished_at"
            );
            ensureColumn(
                    "buyer_comment",
                    "ALTER TABLE orders ADD COLUMN buyer_comment VARCHAR(500) NULL COMMENT '买家评价内容' AFTER buyer_rate"
            );
            ensureColumn(
                    "reviewed_at",
                    "ALTER TABLE orders ADD COLUMN reviewed_at DATETIME NULL COMMENT '评价时间' AFTER buyer_comment"
            );
        } catch (Exception ex) {
            log.warn("Failed to ensure orders buyer review columns", ex);
        }
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer exists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'orders'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
