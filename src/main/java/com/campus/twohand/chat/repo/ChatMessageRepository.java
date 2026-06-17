package com.campus.twohand.chat.repo;

import com.campus.twohand.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
    
    /**
     * 获取两个用户之间的聊天记录
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE (cm.fromId = :userId1 AND cm.toId = :userId2) OR (cm.fromId = :userId2 AND cm.toId = :userId1) ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findChatHistory(@Param("userId1") Long userId1, 
                                      @Param("userId2") Long userId2, 
                                      Pageable pageable);
    
    /**
     * 获取用户的联系人列表（按最近消息时间排序）
     */
    @Query("SELECT DISTINCT CASE WHEN cm.fromId = :userId THEN cm.toId ELSE cm.fromId END as contactId FROM ChatMessage cm WHERE cm.fromId = :userId OR cm.toId = :userId GROUP BY contactId ORDER BY MAX(cm.createdAt) DESC")
    List<Long> findUserContacts(@Param("userId") Long userId);
    
    /**
     * 获取用户未读消息数
     */
    long countByToIdAndReadFlag(Long toId, Integer readFlag);
    long countByToIdAndFromIdAndReadFlag(Long toId, Long fromId, Integer readFlag);
    long countBySessionIdAndToIdAndReadFlag(Long sessionId, Long toId, Integer readFlag);
    
    /**
     * 更新消息状态
     */
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.readFlag = 1 WHERE cm.toId = :userId AND cm.fromId = :contactId AND cm.readFlag = 0")
    int updateMessageStatus(@Param("userId") Long userId, 
                           @Param("contactId") Long contactId);

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.readFlag = 1 WHERE cm.sessionId = :sessionId AND cm.toId = :userId AND cm.readFlag = 0")
    int markSessionMessagesRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    @Modifying
    @Query("""
        DELETE FROM ChatMessage cm
        WHERE (cm.fromId = :userId AND cm.toId = :contactId)
           OR (cm.fromId = :contactId AND cm.toId = :userId)
    """)
    int deleteConversation(@Param("userId") Long userId, @Param("contactId") Long contactId);

    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.sessionId = :sessionId")
    int deleteBySessionIdForConversation(@Param("sessionId") Long sessionId);
}
