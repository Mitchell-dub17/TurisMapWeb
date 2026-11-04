package com.turismap.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRegistroComercio {

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
    private String horario;
    private String sitioWeb;
    private Coordenadas coordenadas;

    // Control de la Solicitud
    private String planId;        // ID del plan: "presencia_basica", "socio_turismap", "socio_premium"
    private String planNombre;    // Nombre del plan para referencia
    private String propietarioUid;
    private String estado;        // "PENDIENTE", "APROBADO", "RECHAZADO"
    private String fechaRegistro; // Fecha ISO-8601
}