package com.sqldpass.service.notice;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.domain.notice.SiteNotice;
import com.sqldpass.persistent.notice.NoticeDisplayType;
import com.sqldpass.persistent.notice.SiteNoticeRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class SiteNoticeServiceTest {

    @Autowired
    private SiteNoticeService siteNoticeService;

    @Autowired
    private SiteNoticeRepository siteNoticeRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        siteNoticeRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("create persists a notice and getActive returns it")
    void createAndGetActive() {
        SiteNotice created = siteNoticeService.create(
                NoticeDisplayType.BANNER, "Important", "Notice body", true);

        entityManager.flush();
        entityManager.clear();

        Optional<SiteNotice> active = siteNoticeService.getActive(NoticeDisplayType.BANNER);

        assertThat(created.getId()).isNotNull();
        assertThat(active).isPresent();
        assertThat(active.get().getTitle()).isEqualTo("Important");
        assertThat(active.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("update changes the notice fields and increments version")
    void update() {
        SiteNotice created = siteNoticeService.create(
                NoticeDisplayType.BANNER, "Initial", "Before", false);

        SiteNotice updated = siteNoticeService.update(
                created.getId(), NoticeDisplayType.MODAL, "Updated", "After", true);

        assertThat(updated.getDisplayType()).isEqualTo(NoticeDisplayType.MODAL);
        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getBody()).isEqualTo("After");
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("setActive toggles active state and increments version")
    void setActive() {
        SiteNotice created = siteNoticeService.create(
                NoticeDisplayType.BANNER, "Initial", "Before", false);

        SiteNotice updated = siteNoticeService.setActive(created.getId(), true);

        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("delete throws when the notice does not exist")
    void delete_notFound() {
        assertThatThrownBy(() -> siteNoticeService.delete(999L))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }
}
