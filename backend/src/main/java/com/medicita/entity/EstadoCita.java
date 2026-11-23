package com.medicita.entity;

/**
 * Estados de una cita. Los nombres van en minusculas a proposito: son exactamente
 * los valores del contrato de API y de la columna citas.estado en la base de datos.
 */
public enum EstadoCita {
    confirmada,
    completada,
    cancelada,
    no_asistio
}
