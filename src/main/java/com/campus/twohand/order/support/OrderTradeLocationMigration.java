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
public class OrderTradeLocationMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderTradeLocationMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(1)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'orders'
                      AND COLUMN_NAME = 'trade_location'
                    """,
                    Integer.class
            );
            if (exists == null || exists == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE orders ADD COLUMN trade_location VARCHAR(255) NULL COMMENT '交易地点快照' AFTER address_id"
                );
            }
            jdbcTemplate.execute(
                    """
                    UPDATE orders o
                    LEFT JOIN product p ON p.id = o.product_id
                    SET o.trade_location = COALESCE(
                      NULLIF(TRIM(o.trade_location), ''),
                      NULLIF(TRIM(p.address_text), '')
                    )
                    WHERE o.trade_location IS NULL OR TRIM(o.trade_location) = ''
                    """
            );
        } catch (Exception ex) {
            log.warn("Failed to ensure orders.trade_location column for trade location snapshot", ex);
        }
    }
}
