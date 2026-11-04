package com.turismap.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaComercio {
    private String id;
    private String nombre;
    private String descripcion;
    private Integer orden;
    private Boolean activo;
}
