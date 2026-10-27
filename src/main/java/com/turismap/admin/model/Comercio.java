package com.turismap.admin.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.cloud.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comercio {
    private String id;
    
    @NotBlank(message = "El propietario UID es obligatorio")
    private String propietarioUid;
    
    private String lugarId;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;
    
    private String direccion;
    private String telefono;
    private String sitioWeb;
    private String horario;
    
    @NotBlank(message = "El estado es obligatorio")
    private String estado; // pendiente, publicado
    
    private Double precioDesde;
    private Double calificacionPromedio;
    private Integer cantidadResenas;
    
    private List<String> servicios;
    private List<String> imagenUrls;
    
    private Boolean destacado;
    
    private Timestamp creadoEn;
    private Timestamp actualizadoEn;
}
