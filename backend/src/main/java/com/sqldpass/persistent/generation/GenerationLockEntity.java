package com.sqldpass.persistent.generation;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generation_lock")
public class GenerationLockEntity {

    @Id
    private int id = 1;

    @Column(nullable = false, length = 20)
    private String status = "IDLE";

    private LocalDateTime startedAt;

    @Column(columnDefinition = "TEXT")
    private String result;

    public void acquire() {
        this.status = "RUNNING";
        this.result = null;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(String resultJson) {
        this.status = "COMPLETED";
        this.result = resultJson;
        this.startedAt = null;
    }

    public void fail(String error) {
        this.status = "FAILED";
        this.result = error;
        this.startedAt = null;
    }

    public void reset() {
        this.status = "IDLE";
        this.result = null;
        this.startedAt = null;
    }

    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    public boolean isStale() {
        return isRunning() && startedAt != null && startedAt.isBefore(LocalDateTime.now().minusMinutes(30));
    }
}
