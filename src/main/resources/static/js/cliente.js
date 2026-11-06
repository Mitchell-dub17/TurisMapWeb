const portalCliente = {
    comercios: [],
    solicitudes: [],
    comercioActual: null,
    resenas: [],
    resenasCargadasPara: null,
    imagenesExistentes: [],
    archivosPendientes: [],
    objectUrls: [],
    cambiosInformacionPendientes: false,
    cambiosGaleriaPendientes: false
};
const imagenComercioVacia = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 640 360'%3E%3Crect width='640' height='360' fill='%23e2e8f0'/%3E%3Cpath d='M240 210h160v90H240zM220 210l30-70h140l30 70' fill='none' stroke='%2394a3b8' stroke-width='12'/%3E%3C/svg%3E";

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');
    const rol = (localStorage.getItem('userTipo') || '').toLowerCase();
    if (!token || rol !== 'cliente') {
        cerrarSesionCliente();
        return;
    }

    function nombrePlan(solicitud) {
        if (solicitud.planNombre) return solicitud.planNombre;
        const nombres = {
            presencia_basica: 'Presencia Básica',
            socio_turismap: 'Socio TurisMap',
            socio_premium: 'Socio Premium'
        };
        return nombres[solicitud.planId] || solicitud.planId || 'No especificado';
    }

    document.getElementById('nombre-cliente').textContent =
        localStorage.getItem('userName') || 'socio';
    document.getElementById('btn-cerrar-sesion').addEventListener('click', event => {
        event.preventDefault();
        cerrarSesionCliente();
    });
    document.getElementById('btn-volver-negocios').addEventListener('click', mostrarListaNegocios);
    document.getElementById('btn-guardar-fotos-cli').addEventListener('click', guardarGaleriaCliente);
    document.getElementById('btnAgregarUrl').addEventListener('click', agregarUrlGaleria);
    document.getElementById('inputUrlDirecta').addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            agregarUrlGaleria();
        }
    });
    document.getElementById('cli-input-fotos').addEventListener('change', event => {
        agregarArchivosGaleria(Array.from(event.target.files || []));
        event.target.value = '';
    });
    inicializarDropzone();
    inicializarPestanas();
    document.getElementById('formInformacionCompleta').addEventListener('submit', guardarInformacion);
    document.getElementById('formInformacionCompleta').addEventListener('input', () => {
        portalCliente.cambiosInformacionPendientes = true;
    });
    await cargarDatosPortal();
});

async function fetchClienteAutenticado(url, opciones = {}) {
    const token = localStorage.getItem('token');
    if (!token) {
        cerrarSesionCliente();
        throw new Error('La sesión del cliente no está disponible.');
    }
    const headers = new Headers(opciones.headers || {});
    headers.set('Authorization', `Bearer ${token}`);
    if (opciones.body && !(opciones.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const respuesta = await fetch(url, { ...opciones, headers });
    if (respuesta.status === 401 || respuesta.status === 403) {
        cerrarSesionCliente();
        throw new Error('La sesión expiró o no tiene permisos para esta operación.');
    }
    if (!respuesta.ok) {
        const detalle = await respuesta.text();
        throw new Error(detalle || `Error del servidor (${respuesta.status}).`);
    }
    return respuesta;
}

async function cargarDatosPortal() {
    const resultados = await Promise.allSettled([
        fetchClienteAutenticado('/api/comercios/mios').then(respuesta => respuesta.json()),
        fetchClienteAutenticado('/api/solicitudes/mias').then(respuesta => respuesta.json())
    ]);

    const [resultadoComercios, resultadoSolicitudes] = resultados;
    if (resultadoComercios.status === 'fulfilled' && Array.isArray(resultadoComercios.value)) {
        portalCliente.comercios = resultadoComercios.value;
        renderizarComercios();
    } else {
        mostrarMensaje('No se pudieron cargar tus comercios. Intenta actualizar la página.', true);
        document.getElementById('estado-comercios').hidden = false;
        document.getElementById('estado-comercios').textContent = 'No fue posible consultar tus comercios.';
    }

    if (resultadoSolicitudes.status === 'fulfilled' && Array.isArray(resultadoSolicitudes.value)) {
        portalCliente.solicitudes = resultadoSolicitudes.value;
        renderizarSolicitudes();
        renderizarComercios();
        if (portalCliente.comercioActual) renderizarFichaComercio(portalCliente.comercioActual);
    } else {
        document.getElementById('estado-solicitudes').hidden = false;
        document.getElementById('estado-solicitudes').textContent = 'No fue posible consultar tus solicitudes.';
    }
}

function renderizarComercios() {
    const contenedor = document.getElementById('lista-comercios');
    const estado = document.getElementById('estado-comercios');
    contenedor.replaceChildren();
    document.getElementById('conteo-comercios').textContent =
        `${portalCliente.comercios.length} ${portalCliente.comercios.length === 1 ? 'negocio' : 'negocios'}`;

    if (portalCliente.comercios.length === 0) {
        estado.hidden = false;
        estado.textContent = portalCliente.solicitudes.length
            ? 'Aún no tienes comercios publicados en tu cuenta. Tus solicitudes aparecen abajo y aquí verás cada comercio cuando el equipo lo registre y publique.'
            : 'Aún no hay comercios asociados a tu cuenta. Cuando se registre y publique uno, podrás administrarlo desde aquí.';
        return;
    }
    estado.hidden = true;

    portalCliente.comercios.forEach(comercio => {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'business-card';
        boton.dataset.comercioId = comercio.id || '';
        boton.setAttribute('aria-label', `Administrar ${comercio.nombre || 'comercio'}`);

        const imagen = document.createElement('img');
        imagen.className = 'business-card-image';
        imagen.alt = '';
        imagen.loading = 'lazy';
        imagen.src = esUrlHttp(comercio.imagenUrls?.[0])
            ? comercio.imagenUrls[0]
            : imagenComercioVacia;
        imagen.addEventListener('error', () => {
            imagen.src = imagenComercioVacia;
        }, { once: true });

        const cuerpo = document.createElement('div');
        cuerpo.className = 'business-card-body';
        const fila = document.createElement('div');
        fila.className = 'business-card-top';
        const datos = document.createElement('div');
        const nombre = document.createElement('h3');
        nombre.textContent = comercio.nombre || 'Comercio sin nombre';
        const categoria = document.createElement('p');
        categoria.textContent = comercio.categoria || 'Categoría no especificada';
        datos.append(nombre, categoria);

        const estadoComercio = document.createElement('span');
        estadoComercio.className = `status-pill ${claseEstadoComercio(comercio.estado)}`;
        estadoComercio.textContent = etiquetaEstadoComercio(comercio.estado);
        fila.append(datos, estadoComercio);

        const meta = document.createElement('div');
        meta.className = 'business-card-meta';
        const resenas = Number(comercio.cantidadResenas) || 0;
        meta.textContent = `${formatearCalificacion(comercio.calificacionPromedio)} ★  ·  ${resenas} ${resenas === 1 ? 'reseña' : 'reseñas'}`;
        cuerpo.append(fila, meta);
        boton.append(imagen, cuerpo);
        boton.addEventListener('click', () => {
            const mismoComercio = portalCliente.comercioActual?.id === comercio.id;
            if (!mismoComercio && hayCambiosPendientes() &&
                !window.confirm('Tienes cambios sin guardar. ¿Descartarlos y cambiar de comercio?')) {
                return;
            }
            seleccionarComercio(comercio);
        });
        contenedor.appendChild(boton);
    });
}

function renderizarSolicitudes() {
    const contenedor = document.getElementById('lista-solicitudes');
    const estado = document.getElementById('estado-solicitudes');
    contenedor.replaceChildren();
    const solicitudes = [...portalCliente.solicitudes].sort((a, b) =>
        new Date(b.fechaRegistro || 0) - new Date(a.fechaRegistro || 0));
    document.getElementById('conteo-solicitudes').textContent =
        `${solicitudes.length} ${solicitudes.length === 1 ? 'solicitud' : 'solicitudes'}`;

    if (!solicitudes.length) {
        estado.hidden = false;
        estado.textContent = 'No tienes solicitudes registradas.';
        return;
    }
    estado.hidden = true;

    solicitudes.forEach(solicitud => {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'request-card';
        const encabezado = document.createElement('div');
        encabezado.className = 'request-card-header';
        const nombreDatos = document.createElement('div');
        const nombre = document.createElement('h3');
        nombre.textContent = solicitud.nombreNegocio || 'Solicitud sin nombre';
        const subtitulo = document.createElement('p');
        subtitulo.className = 'request-card-subtitle';
        subtitulo.textContent = `${solicitud.categoria || 'Categoría no especificada'} · ${solicitud.municipio || 'Municipio no especificado'}`;
        nombreDatos.append(nombre, subtitulo);

        const estadoSolicitud = document.createElement('span');
        estadoSolicitud.className = `status-pill ${claseEstadoSolicitud(solicitud.estado)}`;
        estadoSolicitud.textContent = etiquetaEstadoSolicitud(solicitud.estado);
        encabezado.append(nombreDatos, estadoSolicitud);

        const detalles = document.createElement('dl');
        detalles.className = 'request-details';
        agregarDetalleSolicitud(detalles, 'Plan', nombrePlan(solicitud));
        agregarDetalleSolicitud(detalles, 'Dirección', solicitud.direccion || 'No especificada');
        agregarDetalleSolicitud(detalles, 'Teléfono', solicitud.telefono || 'No especificado');
        agregarDetalleSolicitud(detalles, 'Horario', solicitud.horario || 'No especificado');
        agregarDetalleSolicitud(detalles, 'Sitio web', solicitud.sitioWeb || 'No especificado');
        agregarDetalleSolicitud(detalles, 'Fecha de solicitud', formatearFecha(solicitud.fechaRegistro));

        tarjeta.append(encabezado, detalles);
        if (solicitud.descripcion) {
            const descripcion = document.createElement('p');
            descripcion.className = 'request-description';
            descripcion.textContent = solicitud.descripcion;
            tarjeta.appendChild(descripcion);
        }
        contenedor.appendChild(tarjeta);
    });
}

function agregarDetalleSolicitud(lista, etiqueta, valor) {
    const elemento = document.createElement('div');
    const titulo = document.createElement('dt');
    titulo.textContent = etiqueta;
    const dato = document.createElement('dd');
    dato.textContent = valor;
    elemento.append(titulo, dato);
    lista.appendChild(elemento);
}

function seleccionarComercio(comercio) {
    portalCliente.comercioActual = comercio;
    portalCliente.resenasCargadasPara = null;
    portalCliente.cambiosInformacionPendientes = false;
    portalCliente.cambiosGaleriaPendientes = false;
    document.getElementById('gestion-comercio').hidden = false;
    document.getElementById('gestion-comercio').scrollIntoView({ behavior: 'smooth', block: 'start' });
    renderizarFichaComercio(comercio);
    seleccionarPestana('resumen');
    document.querySelectorAll('.business-card').forEach(card => {
        card.classList.toggle('selected', card.dataset.comercioId === (comercio.id || ''));
    });
}

function renderizarFichaComercio(comercio) {
    const solicitud = solicitudAsociada(comercio);
    document.getElementById('gestion-nombre').textContent = comercio.nombre || 'Comercio sin nombre';
    document.getElementById('gestion-subtitulo').textContent =
        [comercio.categoria, comercio.direccion].filter(Boolean).join(' · ') || 'Ficha de tu comercio';
    const estado = document.getElementById('gestion-estado');
    estado.className = `status-pill ${claseEstadoComercio(comercio.estado)}`;
    estado.textContent = etiquetaEstadoComercio(comercio.estado);

    document.getElementById('resumen-estado').textContent = etiquetaEstadoComercio(comercio.estado);
    document.getElementById('resumen-calificacion').textContent = formatearCalificacion(comercio.calificacionPromedio);
    const cantidad = Number(comercio.cantidadResenas) || 0;
    document.getElementById('resumen-total-resenas').textContent =
        `${cantidad} ${cantidad === 1 ? 'reseña recibida' : 'reseñas recibidas'}`;
    document.getElementById('resumen-fotos').textContent =
        Array.isArray(comercio.imagenUrls) ? comercio.imagenUrls.length : '0';

    const datos = document.getElementById('datos-principales');
    datos.replaceChildren();
    [
        ['Nombre comercial', comercio.nombre],
        ['Categoría', comercio.categoria],
        ['Dirección', comercio.direccion],
        ['Teléfono', comercio.telefono],
        ['Horario', comercio.horario],
        ['Sitio web', comercio.sitioWeb],
        ['Precio desde', comercio.precioDesde == null ? '' : `$${Number(comercio.precioDesde).toFixed(2)}`],
        ['Servicios', Array.isArray(comercio.servicios) ? comercio.servicios.join(', ') : '']
    ].forEach(([etiqueta, valor]) => {
        const fila = document.createElement('div');
        const titulo = document.createElement('dt');
        titulo.textContent = etiqueta;
        const dato = document.createElement('dd');
        dato.textContent = valor || 'No especificado';
        fila.append(titulo, dato);
        datos.appendChild(fila);
    });
    const solicitudAsociadaContenedor = document.getElementById('datos-solicitud-asociada');
    const datosSolicitud = document.getElementById('datos-solicitud-comercio');
    datosSolicitud.replaceChildren();
    solicitudAsociadaContenedor.hidden = !solicitud;
    if (solicitud) {
        [
            ['Municipio', solicitud.municipio],
            ['Plan solicitado', nombrePlan(solicitud)],
            ['Estado de solicitud', etiquetaEstadoSolicitud(solicitud.estado)],
            ['Fecha de solicitud', formatearFecha(solicitud.fechaRegistro)],
            ['Solicitante', solicitud.nombreCompleto],
            ['Correo de contacto', solicitud.email]
        ].forEach(([etiqueta, valor]) => {
            const fila = document.createElement('div');
            const titulo = document.createElement('dt');
            titulo.textContent = etiqueta;
            const dato = document.createElement('dd');
            dato.textContent = valor || 'No especificado';
            fila.append(titulo, dato);
            datosSolicitud.appendChild(fila);
        });
    }

    document.getElementById('cli-nombre-completo').value = comercio.nombre || '';
    document.getElementById('cli-categoria').value = comercio.categoria || '';
    document.getElementById('cli-direccion').value = comercio.direccion || '';
    document.getElementById('cli-municipio').value = solicitud?.municipio || comercio.municipio || '';
    document.getElementById('cli-telefono').value = comercio.telefono || '';
    document.getElementById('cli-horario').value = comercio.horario || '';
    document.getElementById('cli-web').value = comercio.sitioWeb || '';
    document.getElementById('cli-precio').value = comercio.precioDesde ?? '';
    document.getElementById('cli-descripcion').value = comercio.descripcion || '';

    portalCliente.imagenesExistentes = Array.isArray(comercio.imagenUrls) ? [...comercio.imagenUrls] : [];
    portalCliente.archivosPendientes = [];
    renderizarGaleria();
}

function solicitudAsociada(comercio) {
    if (comercio.solicitudId) {
        return portalCliente.solicitudes.find(solicitud => solicitud.id === comercio.solicitudId) || null;
    }
    const nombre = normalizarNombre(comercio.nombre);
    if (!nombre) return null;
    const coincidencias = portalCliente.solicitudes.filter(solicitud =>
        normalizarNombre(solicitud.nombreNegocio) === nombre);
    return coincidencias.length === 1 ? coincidencias[0] : null;
}

function normalizarNombre(valor) {
    return String(valor || '').trim().toLocaleLowerCase('es-SV');
}

async function guardarInformacion(event) {
    event.preventDefault();
    const comercio = portalCliente.comercioActual;
    if (!comercio?.id) return;

    const boton = document.getElementById('btn-guardar-informacion');
    boton.disabled = true;
    boton.textContent = 'Guardando…';
    try {
        const actualizado = {
            ...comercio,
            nombre: document.getElementById('cli-nombre-completo').value.trim(),
            direccion: document.getElementById('cli-direccion').value.trim(),
            telefono: document.getElementById('cli-telefono').value.trim(),
            horario: document.getElementById('cli-horario').value.trim(),
            sitioWeb: document.getElementById('cli-web').value.trim(),
            precioDesde: document.getElementById('cli-precio').value === ''
                ? null
                : Number(document.getElementById('cli-precio').value),
            descripcion: document.getElementById('cli-descripcion').value.trim()
        };
        delete actualizado.creadoEn;
        delete actualizado.actualizadoEn;
        const respuesta = await fetchClienteAutenticado(`/api/comercios/${encodeURIComponent(comercio.id)}`, {
            method: 'PUT',
            body: JSON.stringify(actualizado)
        });
        portalCliente.comercioActual = await respuesta.json();
        portalCliente.cambiosInformacionPendientes = false;
        portalCliente.comercios = portalCliente.comercios.map(item =>
            item.id === comercio.id ? portalCliente.comercioActual : item);
        renderizarFichaComercio(portalCliente.comercioActual);
        renderizarComercios();
        mostrarMensaje('La información del comercio se guardó correctamente.');
    } catch (error) {
        console.error('Error al guardar la información del comercio:', error);
        mostrarMensaje(error.message || 'No se pudo guardar la información.', true);
    } finally {
        boton.disabled = false;
        boton.textContent = 'Guardar información';
    }
}

function inicializarDropzone() {
    const zona = document.getElementById('cli-dropzone');
    ['dragenter', 'dragover'].forEach(nombreEvento => zona.addEventListener(nombreEvento, event => {
        event.preventDefault();
        zona.classList.add('dragover');
    }));
    ['dragleave', 'drop'].forEach(nombreEvento => zona.addEventListener(nombreEvento, event => {
        event.preventDefault();
        zona.classList.remove('dragover');
    }));
    zona.addEventListener('drop', event => agregarArchivosGaleria(Array.from(event.dataTransfer.files || [])));
}

function agregarArchivosGaleria(archivos) {
    const permitidos = archivos.filter(archivo =>
        ['image/jpeg', 'image/png', 'image/webp'].includes(archivo.type) &&
        archivo.size <= 10 * 1024 * 1024
    );
    if (permitidos.length !== archivos.length) {
        mostrarMensaje('Solo se aceptan imágenes JPG, PNG o WebP de hasta 10 MB cada una.', true);
    }
    portalCliente.archivosPendientes.push(...permitidos);
    if (permitidos.length) portalCliente.cambiosGaleriaPendientes = true;
    renderizarGaleria();
}

function agregarUrlGaleria() {
    const campo = document.getElementById('inputUrlDirecta');
    const valor = campo.value.trim();
    if (!esUrlHttp(valor)) {
        mostrarMensaje('Ingresa una URL de imagen válida que comience con http:// o https://.', true);
        return;
    }
    portalCliente.imagenesExistentes.push(valor);
    portalCliente.cambiosGaleriaPendientes = true;
    campo.value = '';
    renderizarGaleria();
}

function renderizarGaleria() {
    portalCliente.objectUrls.forEach(url => URL.revokeObjectURL(url));
    portalCliente.objectUrls = [];
    const contenedor = document.getElementById('cli-contenedor-miniaturas');
    const vacio = document.getElementById('galeria-vacia');
    contenedor.replaceChildren();
    const total = portalCliente.imagenesExistentes.length + portalCliente.archivosPendientes.length;
    vacio.hidden = total > 0;

    portalCliente.imagenesExistentes.forEach((url, indice) => {
        const imagen = document.createElement('img');
        imagen.src = url;
        imagen.alt = `Imagen ${indice + 1} del comercio`;
        agregarMiniatura(contenedor, imagen, () => {
            portalCliente.imagenesExistentes.splice(indice, 1);
            portalCliente.cambiosGaleriaPendientes = true;
            renderizarGaleria();
        });
    });
    portalCliente.archivosPendientes.forEach((archivo, indice) => {
        const url = URL.createObjectURL(archivo);
        portalCliente.objectUrls.push(url);
        const imagen = document.createElement('img');
        imagen.src = url;
        imagen.alt = `Imagen nueva ${indice + 1}`;
        agregarMiniatura(contenedor, imagen, () => {
            portalCliente.archivosPendientes.splice(indice, 1);
            portalCliente.cambiosGaleriaPendientes = true;
            renderizarGaleria();
        });
    });
}

function agregarMiniatura(contenedor, imagen, eliminar) {
    const item = document.createElement('div');
    item.className = 'miniatura-item';
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'miniatura-eliminar';
    boton.setAttribute('aria-label', 'Quitar imagen');
    boton.textContent = '×';
    boton.addEventListener('click', eliminar);
    item.append(imagen, boton);
    contenedor.appendChild(item);
}

async function guardarGaleriaCliente() {
    const comercio = portalCliente.comercioActual;
    if (!comercio?.id) return;
    const boton = document.getElementById('btn-guardar-fotos-cli');
    boton.disabled = true;
    boton.textContent = 'Guardando galería…';

    try {
        let imagenUrls = [...portalCliente.imagenesExistentes];
        if (portalCliente.archivosPendientes.length) {
            const formulario = new FormData();
            portalCliente.archivosPendientes.forEach(archivo => formulario.append('archivos', archivo));
            const subida = await fetchClienteAutenticado('/api/archivos/subir', {
                method: 'POST',
                body: formulario
            });
            const urls = await subida.json();
            if (!Array.isArray(urls)) throw new Error('El servidor no devolvió las imágenes cargadas.');
            imagenUrls = [...imagenUrls, ...urls];
        }

        const actualizado = { ...comercio, imagenUrls };
        delete actualizado.creadoEn;
        delete actualizado.actualizadoEn;
        const respuesta = await fetchClienteAutenticado(`/api/comercios/${encodeURIComponent(comercio.id)}`, {
            method: 'PUT',
            body: JSON.stringify(actualizado)
        });
        portalCliente.comercioActual = await respuesta.json();
        portalCliente.comercios = portalCliente.comercios.map(item =>
            item.id === comercio.id ? portalCliente.comercioActual : item);
        portalCliente.imagenesExistentes = [...imagenUrls];
        portalCliente.archivosPendientes = [];
        portalCliente.cambiosGaleriaPendientes = false;
        renderizarFichaComercio(portalCliente.comercioActual);
        renderizarComercios();
        mostrarMensaje('La galería de este comercio se guardó correctamente.');
    } catch (error) {
        console.error('Error al guardar la galería del comercio:', error);
        mostrarMensaje(error.message || 'No se pudo guardar la galería.', true);
    } finally {
        boton.disabled = false;
        boton.textContent = 'Guardar galería';
    }
}

async function cargarResenas(comercio) {
    const comercioId = comercio.id;
    const lista = document.getElementById('lista-resenas');
    const estado = document.getElementById('estado-resenas');
    lista.replaceChildren();
    estado.hidden = false;
    estado.textContent = 'Cargando reseñas…';
    document.getElementById('conteo-resenas').textContent = '…';
    try {
        const respuesta = await fetchClienteAutenticado(
            `/api/resenas/destino/comercio/${encodeURIComponent(comercioId)}`
        );
        const resenas = await respuesta.json();
        if (portalCliente.comercioActual?.id !== comercioId) return;
        portalCliente.resenas = resenas;
        portalCliente.resenasCargadasPara = comercioId;
        renderizarResenas();
    } catch (error) {
        if (portalCliente.comercioActual?.id !== comercioId) return;
        console.error('Error al cargar reseñas del comercio:', error);
        estado.textContent = 'No se pudieron cargar las reseñas de este comercio.';
    }
}

function renderizarResenas() {
    const lista = document.getElementById('lista-resenas');
    const estado = document.getElementById('estado-resenas');
    lista.replaceChildren();
    const resenas = Array.isArray(portalCliente.resenas) ? portalCliente.resenas : [];
    document.getElementById('conteo-resenas').textContent =
        `${resenas.length} ${resenas.length === 1 ? 'reseña' : 'reseñas'}`;
    estado.hidden = resenas.length > 0;
    if (!resenas.length) {
        estado.textContent = 'Este comercio todavía no tiene reseñas.';
        return;
    }

    resenas.forEach(resena => {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'review-card';
        const encabezado = document.createElement('div');
        encabezado.className = 'review-card-header';
        const usuario = document.createElement('strong');
        usuario.textContent = resena.usuarioUid ? `Visitante ${resena.usuarioUid.slice(0, 8)}` : 'Visitante';
        const calificacion = document.createElement('span');
        const valor = Number(resena.calificacion) || 0;
        calificacion.className = 'review-rating';
        calificacion.textContent = `${'★'.repeat(Math.max(0, Math.min(5, valor)))}${'☆'.repeat(5 - Math.max(0, Math.min(5, valor)))} ${valor}/5`;
        encabezado.append(usuario, calificacion);

        const comentario = document.createElement('p');
        comentario.textContent = resena.comentario || 'Sin comentario.';
        const fecha = document.createElement('time');
        fecha.textContent = formatearFecha(resena.creadoEn);
        tarjeta.append(encabezado, comentario, fecha);
        lista.appendChild(tarjeta);
    });
}

function inicializarPestanas() {
    document.querySelectorAll('.business-tab').forEach(boton => {
        boton.addEventListener('click', () => seleccionarPestana(boton.dataset.tab));
    });
}

function seleccionarPestana(tab) {
    document.querySelectorAll('.business-tab').forEach(boton => {
        const activa = boton.dataset.tab === tab;
        boton.classList.toggle('active', activa);
        boton.setAttribute('aria-selected', String(activa));
    });
    document.querySelectorAll('.business-tab-panel').forEach(panel => {
        panel.hidden = panel.id !== `tab-${tab}`;
    });
    if (tab === 'resenas' && portalCliente.comercioActual &&
        portalCliente.resenasCargadasPara !== portalCliente.comercioActual.id) {
        cargarResenas(portalCliente.comercioActual);
    }
}

function mostrarListaNegocios() {
    document.getElementById('gestion-comercio').hidden = true;
    document.getElementById('mis-negocios').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function hayCambiosPendientes() {
    return portalCliente.cambiosInformacionPendientes || portalCliente.cambiosGaleriaPendientes;
}

function mostrarMensaje(mensaje, error = false) {
    const elemento = document.getElementById('portal-mensaje');
    elemento.textContent = mensaje;
    elemento.classList.toggle('error', error);
    elemento.hidden = false;
    if (!error) {
        window.setTimeout(() => {
            if (elemento.textContent === mensaje) elemento.hidden = true;
        }, 5000);
    }
}

function esUrlHttp(valor) {
    try {
        return ['http:', 'https:'].includes(new URL(valor).protocol);
    } catch {
        return false;
    }
}

function etiquetaEstadoComercio(estado) {
    const normalizado = String(estado || '').toLowerCase();
    if (normalizado === 'publicado') return 'Publicado';
    if (normalizado === 'pendiente') return 'Pendiente de publicación';
    return estado || 'Sin estado';
}

function claseEstadoComercio(estado) {
    const normalizado = String(estado || '').toLowerCase();
    if (normalizado === 'publicado') return 'status-published';
    if (normalizado === 'pendiente') return 'status-unpublished';
    return 'status-unpublished';
}

function etiquetaEstadoSolicitud(estado) {
    const normalizado = String(estado || 'PENDIENTE').toUpperCase();
    if (normalizado === 'APROBADO') return 'Aprobada';
    if (normalizado === 'RECHAZADO') return 'Rechazada';
    return 'En revisión';
}

function claseEstadoSolicitud(estado) {
    const normalizado = String(estado || 'PENDIENTE').toUpperCase();
    if (normalizado === 'APROBADO') return 'status-approved';
    if (normalizado === 'RECHAZADO') return 'status-rejected';
    return 'status-pending';
}

function formatearCalificacion(valor) {
    const numero = Number(valor);
    return Number.isFinite(numero) && numero > 0 ? numero.toFixed(1) : '—';
}

function formatearFecha(valor) {
    if (!valor) return 'Fecha no disponible';
    let fecha;
    if (typeof valor === 'string' || typeof valor === 'number') {
        fecha = new Date(valor);
    } else if (typeof valor === 'object') {
        const segundos = valor.seconds ?? valor._seconds;
        fecha = Number.isFinite(Number(segundos)) ? new Date(Number(segundos) * 1000) : new Date(NaN);
    }
    return fecha && !Number.isNaN(fecha.getTime())
        ? fecha.toLocaleString('es-SV', { dateStyle: 'medium', timeStyle: 'short' })
        : 'Fecha no disponible';
}

function cerrarSesionCliente() {
    ['token', 'userTipo', 'userName', 'userCorreo', 'userTelefono'].forEach(clave =>
        localStorage.removeItem(clave));
    window.location.replace('/login');
}
