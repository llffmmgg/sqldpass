package com.sqldpass.service.generation.dto;

public record GenerationEvent(String type, String message) {

    public static GenerationEvent progress(String message) {
        return new GenerationEvent("progress", message);
    }

    public static GenerationEvent error(String message) {
        return new GenerationEvent("error", message);
    }
}
