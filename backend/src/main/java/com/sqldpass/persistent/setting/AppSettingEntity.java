package com.sqldpass.persistent.setting;

import com.sqldpass.persistent.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 어드민 토글성 단순 key-value 설정.
 * (예: payment.checkout_open_to_all = "true"/"false")
 */
@Getter
@Entity
@Table(name = "app_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSettingEntity extends BaseTimeEntity {

    @Id
    @Column(name = "setting_key", length = 64, nullable = false)
    private String settingKey;

    @Column(name = "setting_value", length = 255, nullable = false)
    private String settingValue;

    public AppSettingEntity(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public void updateValue(String newValue) {
        this.settingValue = newValue;
    }
}
