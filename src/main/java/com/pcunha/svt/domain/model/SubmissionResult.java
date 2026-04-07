package com.pcunha.svt.domain.model;

public record SubmissionResult(Boolean ok, String error) {
    public static SubmissionResult success() {
        return new SubmissionResult(true, null);
    }

    public static SubmissionResult error(String error) {
        return new SubmissionResult(false, error);
    }
}
