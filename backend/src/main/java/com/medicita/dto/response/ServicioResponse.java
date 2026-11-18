package com.medicita.dto.response;

import com.medicita.entity.Servicio;

import java.math.BigDecimal;

public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer duracionMin,
        BigDecimal precio,
        boolean activo
) {

    public static ServicioResponse desde(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMin(),
                servicio.getPrecio(),
                servicio.isActivo()
        );
    }
}