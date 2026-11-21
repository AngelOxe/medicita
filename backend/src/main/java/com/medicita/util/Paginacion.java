package com.medicita.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Construye el Pageable cuidando los limites del contrato: page >= 0 y size entre 1 y 100. */
public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static Pageable de(int page, int size, Sort orden) {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO);
        return PageRequest.of(pagina, tamano, orden);
    }
}
