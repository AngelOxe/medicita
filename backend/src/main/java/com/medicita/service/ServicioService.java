package com.medicita.service;

import com.medicita.dto.response.ServicioResponse;
import com.medicita.entity.Servicio;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.ServicioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medicita.dto.request.ServicioRequest;
import com.medicita.exception.NombreServicioDuplicadoException;

import java.util.ArrayList;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    public Page<ServicioResponse> listar(
            String search,
            Boolean activo,
            boolean soloActivos,
            Pageable pageable) {

        Specification<Servicio> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();

            // Restriccion para pacientes, calculada por el backend.
            if (soloActivos) {
                condiciones.add(cb.isTrue(root.<Boolean>get("activo")));
            }

            // Filtro opcional solicitado en la consulta.
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }

            if (search != null && !search.isBlank()) {
                String texto = search.strip().toLowerCase(Locale.ROOT);

                // Buscar los caracteres literalmente, incluso % y _.
                String patron = "%" + escaparBusqueda(texto) + "%";

                condiciones.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.<String>get("nombre")),
                                        patron,
                                        '\\'
                                ),
                                cb.like(
                                        cb.lower(root.<String>get("descripcion")),
                                        patron,
                                        '\\'
                                )
                        )
                );
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return servicioRepository.findAll(filtros, pageable)
                .map(ServicioResponse::desde);
    }

    public ServicioResponse obtenerPorId(Long id, boolean soloActivos) {
        Servicio servicio = buscarPorId(id);

        if (soloActivos && !servicio.isActivo()) {
            throw new RecursoNoEncontradoException(
                    "Servicio no encontrado"
            );
        }

        return ServicioResponse.desde(servicio);
    }

        @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        if (servicioRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new NombreServicioDuplicadoException();
        }

        Servicio servicio = new Servicio();
        aplicarDatos(servicio, request);

        Servicio guardado = servicioRepository.saveAndFlush(servicio);

        return ServicioResponse.desde(guardado);
    }

    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        Servicio servicio = buscarPorId(id);

        if (servicioRepository.existsByNombreIgnoreCaseAndIdNot(
                request.nombre(), id)) {
            throw new NombreServicioDuplicadoException();
        }

        aplicarDatos(servicio, request);

        Servicio guardado = servicioRepository.saveAndFlush(servicio);

        return ServicioResponse.desde(guardado);
    }

    @Transactional
    public void desactivar(Long id) {
        Servicio servicio = buscarPorId(id);

        servicio.setActivo(false);
        servicioRepository.saveAndFlush(servicio);
    }

    private void aplicarDatos(Servicio servicio, ServicioRequest request) {
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setDuracionMin(request.duracionMin());
        servicio.setPrecio(request.precio());
        servicio.setActivo(request.activo());
    }



    private Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Servicio no encontrado"
                ));
    }

    private String escaparBusqueda(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}