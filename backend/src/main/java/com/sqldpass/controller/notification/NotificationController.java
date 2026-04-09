package com.sqldpass.controller.notification;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.notification.dto.NotificationResponse;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "알림", description = "사용자 개인 인앱 알림")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록 (로그인 필수)")
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Long memberId = requireMemberId(request);
        Page<NotificationResponse> result = notificationService.list(memberId, page, size)
                .map(NotificationResponse::from);
        return Map.of(
                "items", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", result.getNumber(),
                "size", result.getSize());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미읽 알림 개수")
    public Map<String, Long> unreadCount(HttpServletRequest request) {
        Long memberId = requireMemberId(request);
        return Map.of("count", notificationService.unreadCount(memberId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    public Map<String, Object> read(HttpServletRequest request, @PathVariable Long id) {
        Long memberId = requireMemberId(request);
        notificationService.markRead(memberId, id);
        return Map.of("ok", true);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    public Map<String, Object> readAll(HttpServletRequest request) {
        Long memberId = requireMemberId(request);
        int updated = notificationService.markAllRead(memberId);
        return Map.of("updated", updated);
    }

    private static Long requireMemberId(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
