package com.pcunha.svt.domain.model;

import java.time.Duration;

public final class TimeFormatter {
    private TimeFormatter() {}

    public static String format(long millis) {
        if (millis <= 0) return "0:00";
        Duration d = Duration.ofMillis(millis);
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%d:%02d", m, s);
    }
}
