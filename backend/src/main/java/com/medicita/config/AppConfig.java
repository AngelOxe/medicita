package com.medicita.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class AppConfig {

    /** Zona horaria unica del consultorio: todas las fechas de negocio se interpretan aqui. */
    @Bean
    public ZoneId zonaConsultorio(
            @Value("${app.timezone:America/Mexico_City}") String zona) {
        return ZoneId.of(zona);
    }
}
