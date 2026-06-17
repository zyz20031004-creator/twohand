package com.campus.twohand.chat;

import com.campus.twohand.chat.service.ChatService;
import com.campus.twohand.common.ApiResp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ApiResp<?> sendMessage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return chatService.sendMessage(request, body);
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history")
    public ApiResp<?> getChatHistory(HttpServletRequest request,
                                     @RequestParam Long sessionId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return chatService.getChatHistory(request, sessionId, page, size);
    }

    /**
     * 获取联系人列表
     */
    @GetMapping("/contacts")
    public ApiResp<?> getContacts(HttpServletRequest request, @RequestParam(required = false) Long userId) {
        return chatService.getContacts(request);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/read")
    public ApiResp<?> markAsRead(HttpServletRequest request,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam Long sessionId) {
        return chatService.markAsRead(request, sessionId);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    public ApiResp<?> getUnreadCount(HttpServletRequest request, @RequestParam(required = false) Long userId) {
        return chatService.getUnreadCount(request);
    }

    /**
     * 删除会话
     */
    @PostMapping("/delete")
    public ApiResp<?> deleteConversation(HttpServletRequest request, @RequestParam Long sessionId) {
        return chatService.deleteConversation(request, sessionId);
    }

    /**
     * 获取商品聊天信息
     */
    @GetMapping("/product")
    public ApiResp<?> getChatProduct(HttpServletRequest request,
                                     @RequestParam Long productId,
                                     @RequestParam(required = false) Long sessionId) {
        return chatService.getChatProduct(request, productId, sessionId);
    }

    /**
     * 创建或获取商品会话
     */
    @PostMapping("/session/product")
    public ApiResp<?> createOrGetProductSession(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return chatService.createOrGetProductSession(request, body);
    }

    /**
     * 更新聊天中商品价格
     */
    @PostMapping("/product/price")
    public ApiResp<?> updateChatProductPrice(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return chatService.updateChatProductPrice(request, body);
    }
}
