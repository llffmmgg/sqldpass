package com.sqldpass.service.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.member.MemberEntity;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.payment.SubscriptionEntity;
import com.sqldpass.persistent.payment.SubscriptionHistoryAction;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.persistent.payment.SubscriptionRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 어드민 — 구독 조회/수동 발급/수동 만료.
 *
 * 수동 발급은 보상·이벤트·환불 후 재발급 등 운영 케이스용.
 * paymentId 는 음수 ("-1") 사용해 PG 결제 row 와 구분 (감사 로그 추적 용이).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionHistoryService historyService;

    /**
     * 활성/만료 모두 페이지 조회 (archived 는 기본 제외).
     * archived row 는 운영자가 통계 집계에서 분리(테스트 결제 정리 등)한 것이므로
     * 어드민 화면에서도 보이지 않게 한다. nicknameSearch 가 있으면 닉네임 LIKE 검색.
     */
    public Page<AdminSubscriptionRow> list(String nicknameSearch, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 닉네임 검색이 있으면 회원 → memberId 로 필터 후 조회.
        // 단순 구현: archived 제외 페이지 조회 후 닉네임 mapping (회원 수 적은 운영 단계 가정).
        Page<SubscriptionEntity> rows = subscriptionRepository.findByArchivedAtIsNull(pageable);
        List<Long> memberIds = rows.getContent().stream().map(SubscriptionEntity::getMemberId).distinct().toList();
        Map<Long, String> nicknames = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(MemberEntity::getId, MemberEntity::getNickname));

        LocalDateTime now = LocalDateTime.now();
        List<AdminSubscriptionRow> mapped = rows.getContent().stream()
                .map(s -> new AdminSubscriptionRow(
                        s.getId(), s.getMemberId(),
                        nicknames.getOrDefault(s.getMemberId(), "(탈퇴)"),
                        s.getPlan(), s.getPaymentId(),
                        s.getPurchasedAt(), s.getExpiresAt(),
                        s.isActive(now)))
                .filter(r -> nicknameSearch == null || nicknameSearch.isBlank()
                        || r.nickname().toLowerCase().contains(nicknameSearch.toLowerCase()))
                .toList();

        return new PageImpl<>(mapped, pageable, rows.getTotalElements());
    }

    /**
     * 수동 발급 — 운영자 보상·환불 재발급 등.
     * paidAt = now, expiresAt = now + plan.days (UNLIMITED 면 null).
     * paymentId 컬럼은 nullable 가 아니므로 placeholder (-1) 저장.
     */
    @Transactional
    public AdminSubscriptionRow grantManual(Long memberId, SubscriptionPlan plan, String reason, Long actorAdminId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = plan.isLifetime() ? null : now.plusDays(plan.getDays());
        SubscriptionEntity entity = new SubscriptionEntity(
                memberId, plan, /* paymentId */ null, now, expiresAt);
        SubscriptionEntity saved = subscriptionRepository.save(entity);

        historyService.record(memberId, plan, SubscriptionHistoryAction.GRANTED,
                reason, actorAdminId, /* paymentId */ null);

        log.warn("어드민 구독 수동 발급 memberId={} nickname={} plan={} expiresAt={} reason={} actor={}",
                memberId, member.getNickname(), plan, expiresAt, reason, actorAdminId);

        return new AdminSubscriptionRow(saved.getId(), memberId, member.getNickname(),
                plan, saved.getPaymentId(), now, expiresAt, true);
    }

    /** 수동 만료 — expires_at = now 로 강제 종료. row 는 보존하고 history 에 EXPIRED 기록. */
    @Transactional
    public void expireManual(Long subscriptionId, String reason, Long actorAdminId) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.INVALID_INPUT, "구독을 찾을 수 없습니다."));
        sub.revoke(LocalDateTime.now());

        historyService.record(sub.getMemberId(), sub.getPlan(),
                SubscriptionHistoryAction.EXPIRED, reason, actorAdminId, sub.getPaymentId());

        log.warn("어드민 구독 수동 만료 subscriptionId={} memberId={} plan={} reason={} actor={}",
                subscriptionId, sub.getMemberId(), sub.getPlan(), reason, actorAdminId);
    }

    /**
     * 만료된 구독을 통계 집계에서 분리(archive). row 는 보존하고 history 에 ARCHIVED 기록.
     *
     * 활성 구독은 거부 — 권한이 살아있는 row 가 통계에서 사라지면 운영 혼란.
     * 멱등 — 이미 archived 면 조용히 통과.
     *
     * 주 용도: 어드민 본인 테스트 결제를 매출 통계에서 빼는 것.
     */
    @Transactional
    public void archive(Long subscriptionId, String reason, Long actorAdminId) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.INVALID_INPUT, "구독을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (sub.isActive(now)) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT,
                    "활성 구독은 삭제할 수 없습니다. 먼저 만료 처리하세요.");
        }
        if (sub.isArchived()) {
            return; // 멱등
        }
        sub.archive(now);

        historyService.record(sub.getMemberId(), sub.getPlan(),
                SubscriptionHistoryAction.ARCHIVED, reason, actorAdminId, sub.getPaymentId());

        log.warn("어드민 구독 삭제(archive) subscriptionId={} memberId={} plan={} reason={} actor={}",
                subscriptionId, sub.getMemberId(), sub.getPlan(), reason, actorAdminId);
    }

    public record AdminSubscriptionRow(
            Long id,
            Long memberId,
            String nickname,
            SubscriptionPlan plan,
            Long paymentId,
            LocalDateTime purchasedAt,
            LocalDateTime expiresAt,
            boolean active
    ) {}
}
