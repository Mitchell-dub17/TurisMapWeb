package com.turismap.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPlan {

    private String id;

    // Información Personal del Solicitante
    private String nombreCompleto;
    private String email;
    private String telefono;

    // Información del Negocio / Establecimiento
    private String nombreNegocio;
    private String municipio;
    private String categoria;
    private String direccion;
    private String descripcion;

    // Control de la Solicitud
    private String plan;          // "BASICO"
    private String estado;        // "PENDIENTE", "APROBADO", "RECHAZADO"
    private String fechaRegistro; // Ej: "31/10/2026, 03:30 PM"
}