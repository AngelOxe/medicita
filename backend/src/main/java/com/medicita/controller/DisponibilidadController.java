package com.medicita.controller;

import com.medicita.dto.response.DisponibilidadResponse;
import com.medicita.service.DisponibilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    /** Ejemplo: GET /api/disponibilidad?servicioId=1&fecha=2026-11-23 */
    @GetMapping
    public DisponibilidadResponse obtener(
            @RequestParam Long servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return disponibilidadService.obtener(servicioId, fecha);
    }
}
