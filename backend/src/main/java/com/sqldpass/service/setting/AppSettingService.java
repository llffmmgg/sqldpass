package com.sqldpass.service.setting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sqldpass.persistent.setting.AppSettingEntity;
import com.sqldpass.persistent.setting.AppSettingRepository;

import lombok.RequiredArgsConstructor;

/**
 * 어드민에서 런타임 토글 가능한 단순 설정 조회/저장.
 *
 * <p>현재는 결제창 공개 정책(checkout_open_to_all) 한 항목만 사용.
 * 토글 호출량이 매우 적어 별도 캐시 없이 매 호출 DB 조회.
 */
@Service
@RequiredArgsConstructor
public class AppSettingService {

    public static final String KEY_CHECKOUT_OPEN_TO_ALL = "payment.checkout_open_to_all";

    private final AppSettingRepository repository;

    /**
     * 결제창을 전체 사용자에게 노출할지 여부.
     * 설정 row 가 없으면 기본 true(모두 노출) — 마이그레이션 누락 시 안전한 fallback.
     */
    @Transactional(readOnly = true)
    public boolean isCheckoutOpenToAll() {
        return repository.findById(KEY_CHECKOUT_OPEN_TO_ALL)
                .map(e -> Boolean.parseBoolean(e.getSettingValue()))
                .orElse(true);
    }

    /** upsert. */
    @Transactional
    public void setCheckoutOpenToAll(boolean openToAll) {
        String value = Boolean.toString(openToAll);
        repository.findById(KEY_CHECKOUT_OPEN_TO_ALL)
                .ifPresentOrElse(
                        e -> e.updateValue(value),
                        () -> repository.save(new AppSettingEntity(KEY_CHECKOUT_OPEN_TO_ALL, value))
                );
    }
}
