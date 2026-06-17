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
public class OrderVisibilityMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderVisibilityMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureColumn(
                    "buyer_visible",
                    "ALTER TABLE orders ADD COLUMN buyer_visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '买家侧是否可见：1可见，0隐藏' AFTER finished_at"
            );
            ensureColumn(
                    "seller_visible",
                    "ALTER TABLE orders ADD COLUMN seller_visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '卖家侧是否可见：1可见，0隐藏' AFTER buyer_visible"
            );
            jdbcTemplate.execute("UPDATE orders SET buyer_visible = 1 WHERE buyer_visible IS NULL");
            jdbcTemplate.execute("UPDATE orders SET seller_visible = 1 WHERE seller_visible IS NULL");
        } catch (Exception ex) {
            log.warn("Failed to ensure orders visibility columns for user-side hide behavior", ex);
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

