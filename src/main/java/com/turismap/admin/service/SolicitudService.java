package com.turismap.admin.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.turismap.admin.model.Coordenadas;
import com.turismap.admin.model.OpcionSolicitud;
import com.turismap.admin.model.PlanComercio;
import com.turismap.admin.model.PlanSolicitud;
import com.turismap.admin.model.SolicitudOpciones;
import com.turismap.admin.model.SolicitudRegistroComercio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private static final String COLECCION = "solicitudes_comercio";
    private final PlanComercioService planComercioService;

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public CompletableFuture<SolicitudOpciones> obtenerOpciones() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var firestore = getFirestore();
                List<OpcionSolicitud> municipios = firestore.collection("lugares").get().get()
                        .getDocuments().stream()
                        .filter(doc -> doc.getString("nombre") != null && !doc.getString("nombre").isBlank())
                        .map(doc -> new OpcionSolicitud(doc.getId(), doc.getString("nombre")))
                        .sorted(Comparator.comparing(OpcionSolicitud::nombre, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                List<OpcionSolicitud> categorias = firestore.collection("categorias_comercio").get().get()
                        .getDocuments().stream()
                        .filter(doc -> Boolean.TRUE.equals(doc.getBoolean("activo")))
                        .map(doc -> new OpcionSolicitud(doc.getId(), etiquetaCatalogo(doc)))
                        .sorted(Comparator.comparing(OpcionSolicitud::nombre, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                List<PlanSolicitud> planes = cargarPlanesActivos(firestore);
                return new SolicitudOpciones(municipios, categorias, planes);
            } catch (Exception e) {
                log.error("Error al cargar opciones de solicitudes desde Firestore", e);
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<List<PlanSolicitud>> obtenerPlanesActivos() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return cargarPlanesActivos(getFirestore());
            } catch (Exception e) {
                log.error("Error al cargar los planes activos desde Firestore", e);
                throw new CompletionException(e);
            }
        });
    }

    private List<PlanSolicitud> cargarPlanesActivos(Firestore firestore) throws Exception {
        return firestore.collection("planes_comercio").get().get()
                .getDocuments().stream()
                .filter(documento -> Boolean.TRUE.equals(documento.getBoolean("activo")))
                .map(this::mapearPlanSolicitud)
                .sorted(Comparator.comparing(
                                PlanSolicitud::orden,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PlanSolicitud::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private PlanSolicitud mapearPlanSolicitud(DocumentSnapshot documento) {
        List<String> beneficios = new ArrayList<>();
        Object beneficiosData = documento.get("beneficios");
        if (beneficiosData instanceof List<?> valores) {
            valores.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(valor -> !valor.isBlank())
                    .forEach(beneficios::add);
        }

        Number orden = documento.get("orden") instanceof Number valor ? valor : null;
        Number precio = documento.get("precio") instanceof Number valor ? valor : null;
        String descripcion = documento.getString("descripcion");
        return new PlanSolicitud(
                documento.getId(),
                etiquetaPlan(documento),
                descripcion == null ? "" : descripcion,
                beneficios,
                precio == null ? null : precio.doubleValue(),
                documento.getString("moneda"),
                documento.getString("periodicidad"),
                orden == null ? null : orden.intValue()
        );
    }

    private String etiquetaCatalogo(DocumentSnapshot documento) {
        String nombre = documento.getString("nombre");
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        String descripcion = documento.getString("descripcion");
        return descripcion == null || descripcion.isBlank() ? documento.getId() : descripcion;
    }

    private String etiquetaPlan(DocumentSnapshot documento) {
        String nombre = documento.getString("nombre");
        return nombre == null || nombre.isBlank() ? documento.getId() : nombre;
    }

    public CompletableFuture<String> guardarSolicitud(
            SolicitudRegistroComercio solicitud,
            String propietarioUid,
            String email
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validarSolicitud(solicitud, propietarioUid, email);
                var firestore = getFirestore();
                DocumentSnapshot plan = firestore.collection("planes_comercio")
                        .document(solicitud.getPlanId()).get().get();
                DocumentSnapshot categoria = firestore.collection("categorias_comercio")
                        .document(solicitud.getCategoria()).get().get();
                boolean municipioExiste = !firestore.collection("lugares")
                        .whereEqualTo("nombre", solicitud.getMunicipio().trim())
                        .get().get().isEmpty();

                if (!plan.exists() || !Boolean.TRUE.equals(plan.getBoolean("activo"))) {
                    throw new IllegalArgumentException("El plan seleccionado no existe o no está activo.");
                }
                if (!categoria.exists() || !Boolean.TRUE.equals(categoria.getBoolean("activo"))) {
                    throw new IllegalArgumentException("La categoría seleccionada no existe o no está activa.");
                }
                if (!municipioExiste) {
                    throw new IllegalArgumentException("Selecciona un municipio válido.");
                }

                String id = UUID.randomUUID().toString();
                solicitud.setId(id);
                solicitud.setPropietarioUid(propietarioUid);
                solicitud.setEmail(email);
                solicitud.setEstado("PENDIENTE");
                solicitud.setFechaRegistro(Instant.now().toString());
                solicitud.setPlanNombre(etiquetaPlan(plan));

                Coordenadas coordenadas = solicitud.getCoordenadas();
                Map<String, Object> coordenadasData = Map.of(
                        "latitud", coordenadas.getLatitud(),
                        "longitud", coordenadas.getLongitud()
                );
                Map<String, Object> datos = new LinkedHashMap<>();
                datos.put("id", id);
                datos.put("propietarioUid", propietarioUid);
                datos.put("nombreCompleto", solicitud.getNombreCompleto().trim());
                datos.put("email", email);
                datos.put("municipio", solicitud.getMunicipio().trim());
                datos.put("nombreNegocio", solicitud.getNombreNegocio().trim());
                datos.put("categoria", solicitud.getCategoria().trim());
                datos.put("descripcion", solicitud.getDescripcion().trim());
                datos.put("direccion", solicitud.getDireccion().trim());
                datos.put("telefono", solicitud.getTelefono().trim());
                datos.put("sitioWeb", solicitud.getSitioWeb() == null ? "" : solicitud.getSitioWeb().trim());
                datos.put("horario", solicitud.getHorario() == null ? "" : solicitud.getHorario().trim());
                datos.put("planId", solicitud.getPlanId().trim());
                datos.put("coordenadas", coordenadasData);
                datos.put("estado", "PENDIENTE");
                datos.put("fechaRegistro", solicitud.getFechaRegistro());

                firestore.collection(COLECCION).document(id).set(datos).get();
                log.info("Solicitud de comercio guardada con ID: {}, planId: {}", id, solicitud.getPlanId());
                return id;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error al guardar la solicitud de comercio en Firestore", e);
                throw new CompletionException(e);
            }
        });
    }

    private void validarSolicitud(
            SolicitudRegistroComercio solicitud,
            String propietarioUid,
            String email
    ) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Los datos de la solicitud son obligatorios.");
        }
        validarLongitud(propietarioUid, 1, 128, "La sesión no tiene un UID válido.");
        validarLongitud(email, 1, 254, "La sesión no tiene un correo válido.");
        validarLongitud(solicitud.getNombreCompleto(), 1, 120, "El nombre completo debe tener entre 1 y 120 caracteres.");
        validarLongitud(solicitud.getMunicipio(), 1, 100, "Selecciona un municipio válido.");
        validarLongitud(solicitud.getNombreNegocio(), 1, 120, "El nombre del negocio debe tener entre 1 y 120 caracteres.");
        validarLongitud(solicitud.getCategoria(), 1, 128, "Selecciona una categoría.");
        validarLongitud(solicitud.getPlanId(), 1, 128, "Selecciona un plan.");
        validarLongitud(solicitud.getDescripcion(), 1, 2000, "La descripción debe tener entre 1 y 2000 caracteres.");
        validarLongitud(solicitud.getDireccion(), 1, 300, "La dirección debe tener entre 1 y 300 caracteres.");
        validarLongitud(solicitud.getTelefono(), 1, 40, "El teléfono debe tener entre 1 y 40 caracteres.");
        validarLongitud(solicitud.getSitioWeb(), 0, 2048, "El sitio web es demasiado largo.");
        validarLongitud(solicitud.getHorario(), 0, 200, "El horario es demasiado largo.");

        if (solicitud.getSitioWeb() != null && !solicitud.getSitioWeb().isBlank()
                && !solicitud.getSitioWeb().trim().startsWith("https://")) {
            throw new IllegalArgumentException("El sitio web debe comenzar con https://.");
        }

        Coordenadas coordenadas = solicitud.getCoordenadas();
        if (coordenadas == null || coordenadas.getLatitud() == null || coordenadas.getLongitud() == null
                || !Double.isFinite(coordenadas.getLatitud()) || !Double.isFinite(coordenadas.getLongitud())
                || coordenadas.getLatitud() < -90 || coordenadas.getLatitud() > 90
                || coordenadas.getLongitud() < -180 || coordenadas.getLongitud() > 180) {
            throw new IllegalArgumentException("Las coordenadas no son válidas.");
        }
    }

    private void validarLongitud(String valor, int minimo, int maximo, String mensaje) {
        String normalizado = valor == null ? "" : valor.trim();
        if (normalizado.length() < minimo || normalizado.length() > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    public CompletableFuture<List<SolicitudRegistroComercio>> obtenerTodas() {
        return CompletableFuture.supplyAsync(() -> {
            List<SolicitudRegistroComercio> lista = new ArrayList<>();
            try {
                var querySnapshot = getFirestore().collection(COLECCION).get().get();
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    SolicitudRegistroComercio s = doc.toObject(SolicitudRegistroComercio.class);
                    s.setId(doc.getId());
                    if (s.getPlanId() == null || s.getPlanId().isBlank()) {
                        s.setPlanId(obtenerPlanId(doc.getData()));
                    }
                    lista.add(s);
                }
            } catch (Exception e) {
                log.error("Error al obtener solicitudes de Firestore", e);
                throw new CompletionException(e);
            }
            return lista;
        }).thenCompose(solicitudes -> {
            Map<String, CompletableFuture<PlanComercio>> planesPorId = new LinkedHashMap<>();
            for (SolicitudRegistroComercio solicitud : solicitudes) {
                String planId = solicitud.getPlanId();
                if (planId != null && !planId.isBlank()) {
                    planesPorId.computeIfAbsent(planId, id ->
                            planComercioService.obtenerPlanPorId(id).exceptionally(error -> {
                                log.warn("No se pudo resolver el plan {} de una solicitud", id, error);
                                return null;
                            }));
                }
            }

            return CompletableFuture.allOf(planesPorId.values().toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        solicitudes.forEach(solicitud -> {
                            CompletableFuture<PlanComercio> planFuture = planesPorId.get(solicitud.getPlanId());
                            if (planFuture != null) {
                                PlanComercio plan = planFuture.join();
                                if (plan != null && plan.getNombre() != null && !plan.getNombre().isBlank()) {
                                    solicitud.setPlanNombre(plan.getNombre());
                                }
                            }
                        });
                        return solicitudes;
                    });
        });
    }

    public CompletableFuture<List<SolicitudRegistroComercio>> obtenerPorPropietario(String propietarioUid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SolicitudRegistroComercio> solicitudes = new ArrayList<>();
                var documentos = getFirestore().collection(COLECCION)
                        .whereEqualTo("propietarioUid", propietarioUid)
                        .get().get();
                for (QueryDocumentSnapshot documento : documentos.getDocuments()) {
                    SolicitudRegistroComercio solicitud =
                            documento.toObject(SolicitudRegistroComercio.class);
                    solicitud.setId(documento.getId());
                    if (solicitud.getPlanId() == null || solicitud.getPlanId().isBlank()) {
                        solicitud.setPlanId(obtenerPlanId(documento.getData()));
                    }
                    solicitudes.add(solicitud);
                }
                return solicitudes;
            } catch (Exception e) {
                log.error("Error al consultar solicitudes del propietario {}", propietarioUid, e);
                throw new CompletionException(e);
            }
        });
    }

    private String obtenerPlanId(Map<String, Object> datos) {
        Object planId = datos.get("planId");
        if (planId instanceof String id && !id.isBlank()) {
            return id;
        }

        Object plan = datos.get("plan");
        if (plan instanceof Map<?, ?> planDatos) {
            Object id = planDatos.get("id");
            if (id instanceof String nestedId && !nestedId.isBlank()) {
                return nestedId;
            }
        }
        return null;
    }

    public CompletableFuture<Boolean> actualizarEstado(String id, String nuevoEstado) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                getFirestore().collection(COLECCION).document(id)
                        .update("estado", nuevoEstado.toUpperCase()).get();
                log.info("Solicitud {} actualizada a estado {}", id, nuevoEstado);
                return true;
            } catch (Exception e) {
                log.error("Error al actualizar estado de la solicitud {}", id, e);
                return false;
            }
        });
    }
}