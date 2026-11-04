package com.turismap.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanComercio {
    private String id;
    private String nombre;
    private String descripcion;
    private List<String> beneficios;
    private Double precio;
    private String moneda;
    private String periodicidad;
    private Integer orden;
    private Boolean activo;
}
