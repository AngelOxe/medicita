package com.medicita.repository;

import com.medicita.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Una cuenta con citas o cambios de estado registrados no se borra: se desactiva. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM citas WHERE paciente_id = :id) "
            + "OR EXISTS (SELECT 1 FROM historial_estados WHERE cambiado_por = :id)", nativeQuery = true)
    boolean tieneActividad(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM password_reset_tokens WHERE usuario_id = :id", nativeQuery = true)
    void borrarTokensDeRecuperacion(@Param("id") Long id);
}
