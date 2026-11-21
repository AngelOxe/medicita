package com.medicita.service;

import com.medicita.entity.Servicio;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Borrado definitivo de servicios: solo si nunca tuvieron citas. */
class ServicioServiceTest {

    private ServicioRepository servicios;
    private ServicioService servicio;

    @BeforeEach
    void preparar() {
        servicios = mock(ServicioRepository.class);
        servicio = new ServicioService(servicios);
    }

    @Test
    void eliminaElServicioYLoQuitaDeLosHorarios() {
        var existente = new Servicio();
        when(servicios.findById(3L)).thenReturn(Optional.of(existente));
        when(servicios.tieneCitas(3L)).thenReturn(false);

        servicio.eliminar(3L);

        verify(servicios).quitarDeHorarios(3L);
        verify(servicios).delete(existente);
    }

    @Test
    void conCitasRegistradasNoSeElimina() {
        when(servicios.findById(3L)).thenReturn(Optional.of(new Servicio()));
        when(servicios.tieneCitas(3L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar(3L));
        verify(servicios, never()).delete(any(Servicio.class));
    }
}
