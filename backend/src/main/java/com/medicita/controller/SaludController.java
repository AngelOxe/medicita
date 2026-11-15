package com.medicita.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salud")
public class SaludController {

    @GetMapping
    public SaludResponse comprobar() {
        return new SaludResponse("OK", "MediCita");
    }

    public record SaludResponse(String estado, String aplicacion) {
    }
}