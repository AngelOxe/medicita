package com.medicita.security;

import com.medicita.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UserDetailsServiceImpl(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        return usuarios.findByEmailIgnoreCase(email.trim())
                .map(UsuarioAutenticado::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado"));
    }
}