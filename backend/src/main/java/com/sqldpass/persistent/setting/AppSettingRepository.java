package com.sqldpass.persistent.setting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, String> {
}
