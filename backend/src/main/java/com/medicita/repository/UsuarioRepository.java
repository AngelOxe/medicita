package com.medicita.repository;

import com.medicita.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** El listado trae el rol en la misma consulta (evita el problema N+1). */
    @Override
    @EntityGraph(attributePaths = "rol")
    Page<Usuario> findAll(Specification<Usuario> filtros, Pageable paginacion);

    long countByRolNombreAndActivoTrue(String rol);
}
