package com.campus.twohand.verify.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentVerifySchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StudentVerifySchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            allowNullableProofUrl();
            dropSchoolStudentNoUniqueIndexes();
            createSchoolStudentNoIndexIfMissing();
        } catch (Exception ex) {
            log.warn("Failed to ensure student_verify schema for repeat verification applications", ex);
        }
    }

    private void allowNullableProofUrl() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'student_verify'
                  AND COLUMN_NAME = 'proof_url'
                """, Integer.class);
        if (exists == null || exists == 0) {
            jdbcTemplate.execute("ALTER TABLE student_verify ADD COLUMN proof_url VARCHAR(255) NULL COMMENT '认证材料地址，可为空' AFTER real_name");
            return;
        }

        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'student_verify'
                  AND COLUMN_NAME = 'proof_url'
                """, String.class);
        if (!"YES".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("ALTER TABLE student_verify MODIFY COLUMN proof_url VARCHAR(255) NULL COMMENT '认证材料地址，可为空'");
        }
    }

    private void dropSchoolStudentNoUniqueIndexes() {
        List<String> indexNames = jdbcTemplate.queryForList("""
                SELECT INDEX_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'student_verify'
                  AND NON_UNIQUE = 0
                  AND INDEX_NAME <> 'PRIMARY'
                GROUP BY INDEX_NAME
                HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'school,student_no'
                """, String.class);
        for (String indexName : indexNames) {
            jdbcTemplate.execute("DROP INDEX " + indexName + " ON student_verify");
        }
    }

    private void createSchoolStudentNoIndexIfMissing() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'student_verify'
                  AND INDEX_NAME = 'idx_student_verify_school_student_no'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_student_verify_school_student_no ON student_verify (school, student_no)");
        }
    }
}
