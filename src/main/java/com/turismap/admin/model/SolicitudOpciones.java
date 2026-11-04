package com.turismap.admin.model;

import java.util.List;

public record SolicitudOpciones(
        List<OpcionSolicitud> municipios,
        List<OpcionSolicitud> categorias,
        List<PlanSolicitud> planes
) {
}
