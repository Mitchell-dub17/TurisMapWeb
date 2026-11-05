package com.turismap.admin.model;

import java.util.List;

public record PlanSolicitud(
        String id,
        String nombre,
        String descripcion,
        List<String> beneficios,
        Double precio,
        String moneda,
        String periodicidad,
        Integer orden
) {
}
