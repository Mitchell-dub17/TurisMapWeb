package com.turismap.admin.config;

import com.turismap.admin.model.CategoriaComercio;
import com.turismap.admin.model.Lugar;
import com.turismap.admin.model.PlanComercio;
import com.turismap.admin.model.UsuarioFirestore;
import com.turismap.admin.service.CategoriaComercioService;
import com.turismap.admin.service.LugarService;
import com.turismap.admin.service.PlanComercioService;
import com.turismap.admin.service.UsuarioFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioFirestoreService usuarioFirestoreService;
    private final LugarService lugarService;
    private final PlanComercioService planComercioService;
    private final CategoriaComercioService categoriaComercioService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando carga de datos de prueba...");
        
        // Crear usuarios de prueba
        crearUsuariosDePrueba();
        
        // Crear lugares de prueba (municipios de la Ruta de las Flores)
        crearLugaresDePrueba();
        
        // Crear planes de comercio
        crearPlanesDeComercio();
        
        // Crear categorías de comercio
        crearCategoriasDeComercio();
        
        log.info("Carga de datos de prueba completada");
    }

    private void crearUsuariosDePrueba() {
        // Admin
        UsuarioFirestore admin = UsuarioFirestore.builder()
                .email("admin@turismap.com")
                .nombre("Diego Zepeda")
                .rol("admin")
                .build();
        
        usuarioFirestoreService.obtenerUsuarioPorEmail(admin.getEmail())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return usuarioFirestoreService.crearUsuario("admin-001", admin)
                                .thenAccept(u -> log.info("Usuario admin creado: {}", u.getEmail()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
        
        // Cliente
        UsuarioFirestore cliente = UsuarioFirestore.builder()
                .email("cliente@turismap.com")
                .nombre("Cliente de Prueba")
                .rol("cliente")
                .build();
        
        usuarioFirestoreService.obtenerUsuarioPorEmail(cliente.getEmail())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return usuarioFirestoreService.crearUsuario("cliente-001", cliente)
                                .thenAccept(u -> log.info("Usuario cliente creado: {}", u.getEmail()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
    }

    private void crearLugaresDePrueba() {
        // Concepción de Ataco
        Lugar ataco = Lugar.builder()
                .id("ataco")
                .nombre("Concepción de Ataco")
                .subtitulo("El Pueblo de los Textiles")
                .ubicacionCorta("Ruta de las Flores, Sonsonate")
                .ubicacionLarga("Concepción de Ataco, Departamento de Sonsonate, El Salvador")
                .elevacion("1,240 msnm")
                .clima("19°C · Fresco durante todo el año")
                .historia("Fundado en el siglo XVI, Ataco es conocido por su rica tradición textil y arquitectura colonial.")
                .mejorTemporadaTitulo("Octubre a Marzo")
                .mejorTemporadaDesc("Temporada seca ideal para explorar")
                .categoria("TODOS")
                .calificacion(4.8)
                .cantidadResenas(1250)
                .ubicacionCoordenadas(createCoordenadas(13.8333, -89.7167))
                .directorioStats(createDirectorioStats(
                        "Joyero de la Ruta",
                        "Conoce la artesanía textil más fina de El Salvador",
                        "🧵", "Textiles Artesanales",
                        "☕", "Café de Altura",
                        "🏛️", "Arquitectura Colonial",
                        "🎭", "Festividades Anuales"
                ))
                .build();
        
        lugarService.obtenerLugarPorId(ataco.getId())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return lugarService.crearLugar(ataco)
                                .thenAccept(l -> log.info("Lugar creado: {}", l.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
        
        // Juayúa
        Lugar juayua = Lugar.builder()
                .id("juayua")
                .nombre("Juayúa")
                .subtitulo("Capital del Sabor")
                .ubicacionCorta("Ruta de las Flores, Sonsonate")
                .ubicacionLarga("Juayúa, Departamento de Sonsonate, El Salvador")
                .elevacion("1,035 msnm")
                .clima("21°C · Templado")
                .historia("Famoso por su festival gastronómico y cascadas cercanas.")
                .mejorTemporadaTitulo("Fines de semana")
                .mejorTemporadaDesc("Festival gastronómico cada fin de semana")
                .categoria("TODOS")
                .calificacion(4.7)
                .cantidadResenas(980)
                .ubicacionCoordenadas(createCoordenadas(13.8333, -89.7500))
                .directorioStats(createDirectorioStats(
                        "Paraíso Gastronómico",
                        "Disfruta de la mejor comida tradicional salvadoreña",
                        "🍽️", "Festival Gastronómico",
                        "💧", "Cascadas Cercanas",
                        "🌶️", "Comida Típica",
                        "🎪", "Mercado Artesanal"
                ))
                .build();
        
        lugarService.obtenerLugarPorId(juayua.getId())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return lugarService.crearLugar(juayua)
                                .thenAccept(l -> log.info("Lugar creado: {}", l.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
        
        // Apaneca
        Lugar apaneca = Lugar.builder()
                .id("apaneca")
                .nombre("Apaneca")
                .subtitulo("Tierra del Café")
                .ubicacionCorta("Ruta de las Flores, Ahuachapán")
                .ubicacionLarga("Apaneca, Departamento de Ahuachapán, El Salvador")
                .elevacion("1,440 msnm")
                .clima("18°C · Frío")
                .historia("Pueblo más alto de El Salvador, famoso por sus fincas de café.")
                .mejorTemporadaTitulo("Todo el año")
                .mejorTemporadaDesc("Ideal para tours de café y senderismo")
                .categoria("TODOS")
                .calificacion(4.6)
                .cantidadResenas(750)
                .ubicacionCoordenadas(createCoordenadas(13.8667, -89.7833))
                .directorioStats(createDirectorioStats(
                        "Cuna del Café",
                        "Descubre el proceso del café de altura",
                        "☕", "Fincas de Café",
                        "🌲", "Senderismo",
                        "🌬️", "Clima Fresco",
                        "🏔️", "Vistas Panorámicas"
                ))
                .build();
        
        lugarService.obtenerLugarPorId(apaneca.getId())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return lugarService.crearLugar(apaneca)
                                .thenAccept(l -> log.info("Lugar creado: {}", l.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
        
        // Salcoatitán
        Lugar salcoatitan = Lugar.builder()
                .id("salcoatitan")
                .nombre("Salcoatitán")
                .subtitulo("Pueblo de los Artistas")
                .ubicacionCorta("Ruta de las Flores, Sonsonate")
                .ubicacionLarga("Salcoatitán, Departamento de Sonsonate, El Salvador")
                .elevacion("1,050 msnm")
                .clima("20°C · Fresco")
                .historia("Conocido por sus galerías de arte y ambiente bohemio.")
                .mejorTemporadaTitulo("Noviembre a Febrero")
                .mejorTemporadaDesc("Temporada de arte y cultura")
                .categoria("TODOS")
                .calificacion(4.5)
                .cantidadResenas(520)
                .ubicacionCoordenadas(createCoordenadas(13.8000, -89.7333))
                .directorioStats(createDirectorioStats(
                        "Refugio Artístico",
                        "Galerías y talleres de arte local",
                        "🎨", "Galerías de Arte",
                        "🖼️", "Talleres Artesanales",
                        "🎭", "Cultura Local",
                        "📸", "Fotografía"
                ))
                .build();
        
        lugarService.obtenerLugarPorId(salcoatitan.getId())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return lugarService.crearLugar(salcoatitan)
                                .thenAccept(l -> log.info("Lugar creado: {}", l.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
        
        // Nahuizalco
        Lugar nahuizalco = Lugar.builder()
                .id("nahuizalco")
                .nombre("Nahuizalco")
                .subtitulo("Raíces Indígenas")
                .ubicacionCorta("Ruta de las Flores, Sonsonate")
                .ubicacionLarga("Nahuizalco, Departamento de Sonsonate, El Salvador")
                .elevacion("980 msnm")
                .clima("22°C · Templado")
                .historia("Pueblo con fuerte herencia indígena y mercado nocturno tradicional.")
                .mejorTemporadaTitulo("Diciembre a Abril")
                .mejorTemporadaDesc("Mejor época para el mercado nocturno")
                .categoria("TODOS")
                .calificacion(4.4)
                .cantidadResenas(480)
                .ubicacionCoordenadas(createCoordenadas(13.7833, -89.7000))
                .directorioStats(createDirectorioStats(
                        "Herencia Viva",
                        "Experiencia auténtica de cultura indígena",
                        "🌙", "Mercado Nocturno",
                        "🪶", "Artesanías Nativas",
                        "🎪", "Tradición Ancestral",
                        "🌿", "Medicina Natural"
                ))
                .build();
        
        lugarService.obtenerLugarPorId(nahuizalco.getId())
                .thenCompose(existing -> {
                    if (existing == null) {
                        return lugarService.crearLugar(nahuizalco)
                                .thenAccept(l -> log.info("Lugar creado: {}", l.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
    }

    private Map<String, Double> createCoordenadas(double latitud, double longitud) {
        Map<String, Double> coords = new HashMap<>();
        coords.put("latitud", latitud);
        coords.put("longitud", longitud);
        return coords;
    }

    private Map<String, Object> createDirectorioStats(String titulo, String descripcion,
            String icon1, String text1, String icon2, String text2,
            String icon3, String text3, String icon4, String text4) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tituloGeneral", titulo);
        stats.put("descripcionGeneral", descripcion);
        stats.put("stat1Icon", icon1);
        stats.put("stat1Texto", text1);
        stats.put("stat2Icon", icon2);
        stats.put("stat2Texto", text2);
        stats.put("stat3Icon", icon3);
        stats.put("stat3Texto", text3);
        stats.put("stat4Icon", icon4);
        stats.put("stat4Texto", text4);
        return stats;
    }

    private void crearPlanesDeComercio() {
        // Presencia Básica
        PlanComercio presenciaBasica = PlanComercio.builder()
                .nombre("Presencia Básica")
                .descripcion("Propuesta inicial de referencia. El alcance y las condiciones se confirmarán antes de activar el plan.")
                .beneficios(Arrays.asList(
                        "Perfil del negocio en el directorio de TurisMap.",
                        "Información de contacto, ubicación, categoría y horario.",
                        "Asociación del negocio con su municipio."
                ))
                .orden(1)
                .activo(true)
                .build();

        planComercioService.obtenerPlanPorId("presencia_basica")
                .thenCompose(existing -> {
                    if (existing == null) {
                        return planComercioService.crearPlan("presencia_basica", presenciaBasica)
                                .thenAccept(p -> log.info("Plan creado: {}", p.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();

        // Socio TurisMap
        PlanComercio socioTurismap = PlanComercio.builder()
                .nombre("Socio TurisMap")
                .descripcion("Propuesta inicial de referencia para negocios que buscan mayor presencia. El alcance y las condiciones se confirmarán antes de activar el plan.")
                .beneficios(Arrays.asList(
                        "Incluye la presencia del negocio en el directorio.",
                        "Opción de mostrar una galería de imágenes del negocio.",
                        "Posibilidad de destacar el negocio en contenidos y guías de TurisMap."
                ))
                .orden(2)
                .activo(true)
                .build();

        planComercioService.obtenerPlanPorId("socio_turismap")
                .thenCompose(existing -> {
                    if (existing == null) {
                        return planComercioService.crearPlan("socio_turismap", socioTurismap)
                                .thenAccept(p -> log.info("Plan creado: {}", p.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();

        // Socio Premium
        PlanComercio socioPremium = PlanComercio.builder()
                .nombre("Socio Premium")
                .descripcion("Propuesta inicial de referencia para negocios que buscan una promoción más amplia. El alcance y las condiciones se confirmarán antes de activar el plan.")
                .beneficios(Arrays.asList(
                        "Incluye las opciones de presencia y promoción de los otros planes.",
                        "Posibilidad de una ubicación destacada en espacios del directorio.",
                        "Posibilidad de participar en campañas y contenidos promocionales."
                ))
                .orden(3)
                .activo(true)
                .build();

        planComercioService.obtenerPlanPorId("socio_premium")
                .thenCompose(existing -> {
                    if (existing == null) {
                        return planComercioService.crearPlan("socio_premium", socioPremium)
                                .thenAccept(p -> log.info("Plan creado: {}", p.getNombre()));
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .join();
    }

    private void crearCategoriasDeComercio() {
        // Cafetería / Bar
        CategoriaComercio cafeteriaBar = CategoriaComercio.builder()
                .id("cafeteria_bar")
                .nombre("Cafetería / Bar")
                .descripcion("Establecimientos que ofrecen café, bebidas y aperitivos")
                .orden(1)
                .activo(true)
                .build();

        categoriaComercioService.actualizarCategoria("cafeteria_bar", cafeteriaBar)
                .thenAccept(c -> log.info("Categoría actualizada/creada: {}", c.getNombre()))
                .join();

        // Restaurante / Comida Típica
        CategoriaComercio restauranteComida = CategoriaComercio.builder()
                .id("restaurante_comida")
                .nombre("Restaurante / Comida Típica")
                .descripcion("Restaurantes y locales de comida tradicional salvadoreña")
                .orden(2)
                .activo(true)
                .build();

        categoriaComercioService.actualizarCategoria("restaurante_comida", restauranteComida)
                .thenAccept(c -> log.info("Categoría actualizada/creada: {}", c.getNombre()))
                .join();

        // Hostal / Hotel / Cabañas
        CategoriaComercio hotelCabañas = CategoriaComercio.builder()
                .id("hotel_cabanas")
                .nombre("Hostal / Hotel / Cabañas")
                .descripcion("Alojamiento turístico: hostales, hoteles y cabañas")
                .orden(3)
                .activo(true)
                .build();

        categoriaComercioService.actualizarCategoria("hotel_cabanas", hotelCabañas)
                .thenAccept(c -> log.info("Categoría actualizada/creada: {}", c.getNombre()))
                .join();

        // Artesanías / Souvenirs
        CategoriaComercio artesanias = CategoriaComercio.builder()
                .id("artesanias_souvenirs")
                .nombre("Artesanías / Souvenirs")
                .descripcion("Tiendas de artesanías locales y souvenirs turísticos")
                .orden(4)
                .activo(true)
                .build();

        categoriaComercioService.actualizarCategoria("artesanias_souvenirs", artesanias)
                .thenAccept(c -> log.info("Categoría actualizada/creada: {}", c.getNombre()))
                .join();

        // Tours / Aventura
        CategoriaComercio toursAventura = CategoriaComercio.builder()
                .id("tours_aventura")
                .nombre("Tours / Aventura")
                .descripcion("Operadores de turismo, tours guiados y actividades de aventura")
                .orden(5)
                .activo(true)
                .build();

        categoriaComercioService.actualizarCategoria("tours_aventura", toursAventura)
                .thenAccept(c -> log.info("Categoría actualizada/creada: {}", c.getNombre()))
                .join();
    }
}
