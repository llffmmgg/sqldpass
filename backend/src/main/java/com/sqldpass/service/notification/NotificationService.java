package com.sqldpass.service.notification;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.notification.Notification;
import com.sqldpass.persistent.notification.NotificationEntity;
import com.sqldpass.persistent.notification.NotificationMapper;
import com.sqldpass.persistent.notification.NotificationRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public Notification notify(Long memberId, String type, String title, String body, String link, Long refId) {
        NotificationEntity entity = new NotificationEntity(memberId, type, title, body, link, refId);
        return NotificationMapper.toDomain(repository.save(entity));
    }

    public Page<Notification> list(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(NotificationMapper::toDomain);
    }

    public long unreadCount(Long memberId) {
        return repository.countByMemberIdAndReadAtIsNull(memberId);
    }

    @Transactional
    public void markRead(Long memberId, Long id) {
        NotificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!entity.getMemberId().equals(memberId)) {
            throw new SqldpassException(ErrorCode.FORBIDDEN);
        }
        entity.markRead();
    }

    @Transactional
    public int markAllRead(Long memberId) {
        return repository.markAllRead(memberId, LocalDateTime.now());
    }
}
