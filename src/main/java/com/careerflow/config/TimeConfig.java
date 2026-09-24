package com.careerflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class TimeConfig {
    @Bean
    public Clock applicationClock() {
        // Existing API deadlines are LocalDateTime values in the server's timezone.
        return Clock.systemDefaultZone();
    }
}
