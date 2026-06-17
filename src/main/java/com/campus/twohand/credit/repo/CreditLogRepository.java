package com.campus.twohand.credit.repo;

import com.campus.twohand.credit.entity.CreditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditLogRepository extends JpaRepository<CreditLog, Long> {
    Page<CreditLog> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    Page<CreditLog> findByUserIdAndChangeValGreaterThanOrderByIdDesc(Long userId, Integer changeVal, Pageable pageable);
    Page<CreditLog> findByUserIdAndChangeValLessThanOrderByIdDesc(Long userId, Integer changeVal, Pageable pageable);
    boolean existsByUserIdAndReasonAndBizTypeAndBizId(Long userId, String reason, String bizType, Long bizId);
    boolean existsByUserIdAndReason(Long userId, String reason);

    @Query("""
            select count(c) > 0
            from CreditLog c
            where c.userId = :userId
              and c.bizType = :bizType
              and c.bizId = :bizId
              and c.reason in ('ORDER_FINISH', 'ORDER_FINISHED')
            """)
    boolean existsOrderFinishedReward(@Param("userId") Long userId,
                                      @Param("bizType") String bizType,
                                      @Param("bizId") Long bizId);
}
