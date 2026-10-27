package com.turismap.admin.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioFirestore {
    @Email(message = "El correo debe ser válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;
    
    @NotBlank(message = "El rol es obligatorio")
    private String rol; // admin, cliente
    
    private String nombre;
    private List<String> intereses;
    private String fotoPerfil;
    private String fotoPortada;
    
    private List<String> favoritos; // IDs de lugares
    
    private Map<String, Object> preferencias;
}
