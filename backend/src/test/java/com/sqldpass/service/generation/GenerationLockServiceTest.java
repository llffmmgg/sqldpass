package com.sqldpass.service.generation;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sqldpass.controller.admin.dto.GenerationStatusResponse;
import com.sqldpass.persistent.generation.GenerationLockEntity;
import com.sqldpass.persistent.generation.GenerationLockRepository;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GenerationLockServiceTest {

    @Mock
    private GenerationLockRepository generationLockRepository;

    @InjectMocks
    private GenerationLockService generationLockService;

    @Test
    @DisplayName("acquire throws when generation is already running and not stale")
    void acquire_running() {
        GenerationLockEntity lock = newLock();
        setField(lock, "status", "RUNNING");
        setField(lock, "startedAt", LocalDateTime.now().minusMinutes(1));

        given(generationLockRepository.findForUpdate()).willReturn(Optional.of(lock));

        assertThatThrownBy(() -> generationLockService.acquire())
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GENERATION_ALREADY_RUNNING);
    }

    @Test
    @DisplayName("getStatus returns IDLE when the lock row does not exist")
    void getStatus_missing() {
        given(generationLockRepository.findById(1)).willReturn(Optional.empty());

        GenerationStatusResponse response = generationLockService.getStatus();

        assertThat(response.status()).isEqualTo("IDLE");
        assertThat(response.result()).isNull();
    }

    @Test
    @DisplayName("getStatus marks a stale running lock as failed")
    void getStatus_stale() {
        GenerationLockEntity lock = newLock();
        setField(lock, "status", "RUNNING");
        setField(lock, "startedAt", LocalDateTime.now().minusMinutes(31));

        given(generationLockRepository.findById(1)).willReturn(Optional.of(lock));

        GenerationStatusResponse response = generationLockService.getStatus();

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.result()).contains("30");
        assertThat(lock.getStatus()).isEqualTo("FAILED");
    }

    private static GenerationLockEntity newLock() {
        try {
            var constructor = GenerationLockEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setField(GenerationLockEntity entity, String name, Object value) {
        try {
            var field = GenerationLockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
