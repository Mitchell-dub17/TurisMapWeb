package com.turismap.admin.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class Lugar {
    private String id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String subtitulo;
    private String ubicacionCorta;
    private String ubicacionLarga;
    private String elevacion;
    private String clima;
    private String historia;
    private String mejorTemporadaTitulo;
    private String mejorTemporadaDesc;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria; // TODOS, CAFE, MIRADOR, ARTESANIA, RESTAURANTE
    
    private Double calificacion;
    private Integer cantidadResenas;
    
    private Map<String, Double> ubicacionCoordenadas; // {latitud, longitud}
    
    private String imagenPortadaUrl;
    
    private Map<String, Object> directorioStats; // {tituloGeneral, descripcionGeneral, stat1Icon, stat1Texto, etc}
    
    private List<Atractivo> atractivos;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Atractivo {
        private String id;
        private String etiqueta;
        private Long etiquetaColorHex;
        private String titulo;
        private String descripcion;
        private String accion;
        private String imagenUrl;
    }
}
