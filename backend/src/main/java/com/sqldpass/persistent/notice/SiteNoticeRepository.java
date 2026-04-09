package com.sqldpass.persistent.notice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteNoticeRepository extends JpaRepository<SiteNoticeEntity, Long> {

    Optional<SiteNoticeEntity> findFirstByDisplayTypeAndActiveTrueOrderByUpdatedAtDesc(NoticeDisplayType displayType);

    List<SiteNoticeEntity> findAllByOrderByUpdatedAtDesc();
}
