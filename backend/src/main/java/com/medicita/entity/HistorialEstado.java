package com.medicita.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cambiado_por", nullable = false)
    private Usuario cambiadoPor;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    public HistorialEstado() {
    }

    public HistorialEstado(
            Cita cita,
            String estadoAnterior,
            String estadoNuevo,
            Usuario cambiadoPor) {
        this.cita = cita;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.cambiadoPor = cambiadoPor;
    }

    @PrePersist
    private void antesDeGuardar() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Cita getCita() {
        return cita;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public Usuario getCambiadoPor() {
        return cambiadoPor;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}