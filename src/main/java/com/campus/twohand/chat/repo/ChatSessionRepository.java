package com.campus.twohand.chat.repo;

import com.campus.twohand.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    boolean existsByProductId(Long productId);

    Optional<ChatSession> findByUserLowAndUserHighAndProductId(Long userLow, Long userHigh, Long productId);

    @Query("""
            SELECT cs FROM ChatSession cs
            WHERE cs.userLow = :userId OR cs.userHigh = :userId
            ORDER BY cs.lastTime DESC, cs.id DESC
            """)
    List<ChatSession> findUserSessions(@Param("userId") Long userId);
}
