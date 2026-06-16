package com.smartinventory.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique order numbers in the format: ORD-YYYYMMDD-HHMMSS-XXXX
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int seq = sequence.getAndIncrement() % 9999;
        return String.format("ORD-%s-%04d", timestamp, seq);
    }
}
