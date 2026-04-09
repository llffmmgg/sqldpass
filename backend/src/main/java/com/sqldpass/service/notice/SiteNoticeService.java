package com.sqldpass.service.notice;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.persistent.notice.NoticeDisplayType;
import com.sqldpass.persistent.notice.SiteNoticeEntity;
import com.sqldpass.persistent.notice.SiteNoticeMapper;
import com.sqldpass.persistent.notice.SiteNoticeRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteNoticeService {

    private final SiteNoticeRepository repository;

    public Optional<SiteNotice> getActive(NoticeDisplayType displayType) {
        return repository.findFirstByDisplayTypeAndActiveTrueOrderByUpdatedAtDesc(displayType)
                .map(SiteNoticeMapper::toDomain);
    }

    public List<SiteNotice> listAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(SiteNoticeMapper::toDomain)
                .toList();
    }

    @Transactional
    public SiteNotice create(NoticeDisplayType displayType, String title, String body, boolean active) {
        SiteNoticeEntity entity = new SiteNoticeEntity(displayType, title, body, active);
        return SiteNoticeMapper.toDomain(repository.save(entity));
    }

    @Transactional
    public SiteNotice update(Long id, NoticeDisplayType displayType, String title, String body, boolean active) {
        SiteNoticeEntity entity = repository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.NOTICE_NOT_FOUND));
        entity.update(displayType, title, body, active);
        return SiteNoticeMapper.toDomain(entity);
    }

    @Transactional
    public SiteNotice setActive(Long id, boolean active) {
        SiteNoticeEntity entity = repository.findById(id)
                .orElseThrow(() -> new SqldpassException(ErrorCode.NOTICE_NOT_FOUND));
        entity.changeActive(active);
        return SiteNoticeMapper.toDomain(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new SqldpassException(ErrorCode.NOTICE_NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
