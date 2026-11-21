package com.medicita.service;

import com.medicita.entity.Horario;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioRepository;
import com.medicita.repository.ServicioRepository;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Borrado definitivo de horarios. */
class HorarioServiceTest {

    @Test
    void eliminarBorraElHorarioCuandoNoQuedanCitasSinCobertura() {
        var horarios = mock(HorarioRepository.class);
        var citas = mock(CitaRepository.class);
        var existente = new Horario();
        when(horarios.findById(4L)).thenReturn(Optional.of(existente));
        when(horarios.findByActivoTrue()).thenReturn(List.of());
        when(citas.buscarConfirmadasFuturas(any())).thenReturn(List.of());

        new HorarioService(horarios, mock(ServicioRepository.class), citas, ZoneId.of("America/Mexico_City"))
                .eliminar(4L);

        verify(horarios).delete(existente);
    }
}
