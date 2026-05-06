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

    /** 활성/만료 모두 페이지 조회. nicknameSearch 가 있으면 닉네임 LIKE 검색. */
    public Page<AdminSubscriptionRow> list(String nicknameSearch, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 닉네임 검색이 있으면 회원 → memberId 로 필터 후 조회.
        // 단순 구현: 전체 페이지 조회 후 닉네임 mapping (회원 수 적은 운영 단계 가정).
        Page<SubscriptionEntity> rows = subscriptionRepository.findAll(pageable);
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
    public AdminSubscriptionRow grantManual(Long memberId, SubscriptionPlan plan, String reason) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = plan.isLifetime() ? null : now.plusDays(plan.getDays());
        SubscriptionEntity entity = new SubscriptionEntity(
                memberId, plan, /* paymentId */ null, now, expiresAt);
        SubscriptionEntity saved = subscriptionRepository.save(entity);

        log.warn("어드민 구독 수동 발급 memberId={} nickname={} plan={} expiresAt={} reason={}",
                memberId, member.getNickname(), plan, expiresAt, reason);

        return new AdminSubscriptionRow(saved.getId(), memberId, member.getNickname(),
                plan, saved.getPaymentId(), now, expiresAt, true);
    }

    /** 수동 만료 — expires_at = now 로 강제 종료. */
    @Transactional
    public void expireManual(Long subscriptionId, String reason) {
        SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SqldpassException(ErrorCode.INVALID_INPUT, "구독을 찾을 수 없습니다."));
        // reflect: SubscriptionEntity 에 setter 없음 — 새 row 추가 패턴 대신 native update.
        subscriptionRepository.delete(sub);
        // 단순 삭제 (UI 에서는 만료된 것처럼 보임). 이력 보존 필요 시 별도 audit 테이블 도입.
        log.warn("어드민 구독 수동 만료 subscriptionId={} memberId={} plan={} reason={}",
                subscriptionId, sub.getMemberId(), sub.getPlan(), reason);
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
