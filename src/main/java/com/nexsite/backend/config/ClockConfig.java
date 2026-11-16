package com.nexsite.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Relógio injetável: nos testes dá para trocar por um relógio falso e "avançar o tempo"
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
