package com.campus.twohand.chat.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatSessionProductMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionProductMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        try {
            Integer productColumnCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'chat_session'
                      AND COLUMN_NAME = 'product_id'
                    """, Integer.class);
            if (productColumnCount == null || productColumnCount == 0) {
                jdbcTemplate.execute("""
                        ALTER TABLE chat_session
                        ADD COLUMN product_id BIGINT NULL COMMENT '关联商品ID' AFTER user_high
                        """);
            }

            dropIndexIfExists("uk_chat_session_pair");
            dropIndexIfExists("uk_chat_pair");
            createIndexIfMissing("uk_chat_session_pair_product",
                    "CREATE UNIQUE INDEX uk_chat_session_pair_product ON chat_session (user_low, user_high, product_id)");
            createIndexIfMissing("idx_chat_session_product",
                    "CREATE INDEX idx_chat_session_product ON chat_session (product_id)");
        } catch (Exception ex) {
            log.warn("Failed to ensure chat_session.product_id migration", ex);
        }
    }

    private void dropIndexIfExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'chat_session'
                  AND INDEX_NAME = ?
                """, Integer.class, indexName);
        if (count != null && count > 0) {
            jdbcTemplate.execute("DROP INDEX " + indexName + " ON chat_session");
        }
    }

    private void createIndexIfMissing(String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'chat_session'
                  AND INDEX_NAME = ?
                """, Integer.class, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
