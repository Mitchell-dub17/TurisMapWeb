package com.turismap.admin.security;

import com.turismap.admin.model.UsuarioFirestore;
import com.turismap.admin.service.UsuarioFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioFirestoreService usuarioFirestoreService;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                    "El correo electrónico no puede estar vacío"
            );
        }

        try {
            UsuarioFirestore usuario = usuarioFirestoreService
                    .obtenerUsuarioPorEmail(email.trim())
                    .get();

            if (usuario == null) {
                throw new UsernameNotFoundException(
                        "Usuario no encontrado"
                );
            }

            if (usuario.getEmail() == null
                    || usuario.getEmail().isBlank()) {
                log.error(
                        "El usuario recuperado de Firestore no tiene correo"
                );

                throw new UsernameNotFoundException(
                        "El usuario no tiene un correo válido"
                );
            }

            if (usuario.getRol() == null
                    || usuario.getRol().isBlank()) {
                log.error(
                        "El usuario {} no tiene un rol configurado",
                        usuario.getEmail()
                );

                throw new UsernameNotFoundException(
                        "El usuario no tiene un rol asignado"
                );
            }

            String rol = usuario.getRol()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!rol.equals("ADMIN") && !rol.equals("CLIENTE")) {
                log.warn(
                        "Rol no reconocido para el usuario {}: {}",
                        usuario.getEmail(),
                        rol
                );

                throw new UsernameNotFoundException(
                        "El rol del usuario no está autorizado"
                );
            }

            return User.builder()
                    .username(usuario.getEmail().trim())
                    .password("")
                    .authorities(Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + rol)
                    ))
                    .build();

        } catch (UsernameNotFoundException ex) {
            throw ex;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            log.error(
                    "Se interrumpió la consulta del usuario en Firestore"
            );

            throw new UsernameNotFoundException(
                    "No se pudo completar la consulta del usuario",
                    ex
            );

        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause();

            if (causa instanceof UsernameNotFoundException) {
                throw (UsernameNotFoundException) causa;
            }

            log.error(
                    "Error al consultar el usuario en Firestore",
                    causa != null ? causa : ex
            );

            throw new UsernameNotFoundException(
                    "No se pudo consultar el usuario",
                    ex
            );

        } catch (CompletionException ex) {
            log.error(
                    "Error al recuperar el usuario de Firestore",
                    ex
            );

            throw new UsernameNotFoundException(
                    "No se pudo consultar el usuario",
                    ex
            );
        }
    }
}
