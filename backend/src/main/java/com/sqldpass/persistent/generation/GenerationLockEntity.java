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

    @Column(nullable = false)
    private boolean running;

    private LocalDateTime startedAt;

    @Column(columnDefinition = "TEXT")
    private String result;

    public void acquire() {
        this.running = true;
        this.startedAt = LocalDateTime.now();
        this.result = null;
    }

    public void completeWithResult(String resultJson) {
        this.running = false;
        this.startedAt = null;
        this.result = resultJson;
    }

    public void release() {
        this.running = false;
        this.startedAt = null;
    }

    public boolean isStale() {
        return running && startedAt != null && startedAt.isBefore(LocalDateTime.now().minusMinutes(30));
    }
}
