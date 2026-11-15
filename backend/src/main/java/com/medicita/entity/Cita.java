package com.medicita.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fin;

    @Column(
        name = "precio_reservado",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal precioReservado;

    @Column(nullable = false, length = 20)
    private String estado = "confirmada";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    public Cita() {
    }

    @PrePersist
    private void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getPaciente() {
        return paciente;
    }

    public void setPaciente(Usuario paciente) {
        this.paciente = paciente;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public void setInicio(OffsetDateTime inicio) {
        this.inicio = inicio;
    }

    public OffsetDateTime getFin() {
        return fin;
    }

    public void setFin(OffsetDateTime fin) {
        this.fin = fin;
    }

    public BigDecimal getPrecioReservado() {
        return precioReservado;
    }

    public void setPrecioReservado(BigDecimal precioReservado) {
        this.precioReservado = precioReservado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}