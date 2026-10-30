// cliente.js - Control del Portal de Socios / Clientes de TurisMap

let comercioClienteActual = null;
let archivosClienteParaSubir = [];
let imagenesClienteExistentes = [];

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');
    const rol = (localStorage.getItem('userTipo') || '').toLowerCase();

    if (!token || (rol !== 'cliente' && rol !== 'admin')) {
        cerrarSesionCliente();
        return;
    }

    inicializarDropzoneCliente();
    inicializarSoporteUrlDirecta();
    await cargarDatosComercioCliente();
});

// Petición HTTP autenticada con JWT
async function fetchClienteAutenticado(url, opciones = {}) {
    const token = localStorage.getItem('token');
    const headers = new Headers(opciones.headers || {});
    headers.set('Authorization', `Bearer ${token}`);

    // Si se envía FormData (archivos a Storage), el navegador asigna automáticamente el boundary
    if (opciones.body && !(opciones.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const respuesta = await fetch(url, { ...opciones, headers });

    if (respuesta.status === 401 || respuesta.status === 403) {
        cerrarSesionCliente();
        throw new Error('Sesión no autorizada o expirada');
    }

    if (!respuesta.ok) {
        throw new Error(`Error en servidor: ${respuesta.status}`);
    }

    return respuesta;
}

// Cargar el negocio asociado al usuario logueado desde Firestore
async function cargarDatosComercioCliente() {
    try {
        const res = await fetchClienteAutenticado('/api/comercios');
        const todos = await res.json();

        // Asociar por UID guardado o tomar el primero si es entorno de pruebas
        comercioClienteActual = todos.find(c => c.propietarioUid === localStorage.getItem('userUid'))
            || todos[0];

        if (!comercioClienteActual) {
            document.getElementById('cliente-comercio-titulo').textContent = 'Bienvenido a TurisMap';
            document.getElementById('cliente-comercio-subtitulo').textContent = 'Tu negocio aún no ha sido dado de alta en Firestore.';
            return;
        }

        renderizarDatosCliente(comercioClienteActual);
    } catch (error) {
        console.error('Error al conectar con Firestore:', error);
    }
}

function renderizarDatosCliente(com) {
    // Títulos superiores
    document.getElementById('cliente-comercio-titulo').textContent = `¡Hola, ${com.nombre}!`;
    document.getElementById('cliente-comercio-subtitulo').textContent = `Administra el perfil de tu negocio en la Ruta de las Flores.`;

    // Formulario de edición rápida
    document.getElementById('cli-comercio-id').value = com.id || '';
    document.getElementById('cli-telefono').value = com.telefono || '';
    document.getElementById('cli-oferta').value = com.descripcion || '';

    // Ficha completa
    document.getElementById('cli-nombre-completo').value = com.nombre || '';
    document.getElementById('cli-categoria').value = com.categoria || 'Comercio';
    document.getElementById('cli-direccion').value = com.direccion || '';
    document.getElementById('cli-horario').value = com.horario || '';
    document.getElementById('cli-web').value = com.sitioWeb || '';
    document.getElementById('cli-precio').value = com.precioDesde || '';

    // Inicializar fotografías existentes
    imagenesClienteExistentes = Array.isArray(com.imagenUrls) ? [...com.imagenUrls] : [];
    archivosClienteParaSubir = [];
    renderizarMiniaturasCliente();

    // Actualizar modal de vista previa
    actualizarModalPreview(com);
}

// ==========================================
// MANEJO DE FOTOGRAFÍAS Y DROPZONE
// ==========================================
function inicializarDropzoneCliente() {
    const dropzone = document.getElementById('cli-dropzone');
    const inputFotos = document.getElementById('cli-input-fotos');

    if (!dropzone || !inputFotos) return;

    ['dragenter', 'dragover'].forEach(evento => {
        dropzone.addEventListener(evento, (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(evento => {
        dropzone.addEventListener(evento, (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        const archivos = Array.from(e.dataTransfer.files).filter(f => f.type.startsWith('image/'));
        agregarArchivosCliente(archivos);
    });

    inputFotos.addEventListener('change', () => {
        const archivos = Array.from(inputFotos.files);
        agregarArchivosCliente(archivos);
        inputFotos.value = '';
    });
}

// ==========================================
// SOPORTE HÍBRIDO: URLs DIRECTAS
// ==========================================
function inicializarSoporteUrlDirecta() {
    const btnAgregar = document.getElementById('btnAgregarUrl');
    const inputUrl = document.getElementById('inputUrlDirecta');

    if (!btnAgregar || !inputUrl) return;

    btnAgregar.addEventListener('click', () => {
        const url = inputUrl.value.trim();

        if (!url) {
            alert('Por favor ingresa o pega una URL válida.');
            return;
        }

        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            alert('La URL debe comenzar con http:// o https://');
            return;
        }

        // Se inserta en la lista real de URLs de imágenes
        imagenesClienteExistentes.push(url);
        renderizarMiniaturasCliente();
        sincronizarMockupCliente();

        inputUrl.value = '';
    });
}

function agregarArchivosCliente(nuevosArchivos) {
    archivosClienteParaSubir = [...archivosClienteParaSubir, ...nuevosArchivos];
    renderizarMiniaturasCliente();
    sincronizarMockupCliente();
}

window.eliminarArchivoCliente = function(index) {
    archivosClienteParaSubir.splice(index, 1);
    renderizarMiniaturasCliente();
    sincronizarMockupCliente();
};

window.eliminarFotoExistenteCliente = function(index) {
    imagenesClienteExistentes.splice(index, 1);
    renderizarMiniaturasCliente();
    sincronizarMockupCliente();
};

function renderizarMiniaturasCliente() {
    const contenedor = document.getElementById('cli-contenedor-miniaturas');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    // Fotos existentes o URLs agregadas directamente
    imagenesClienteExistentes.forEach((url, idx) => {
        const div = document.createElement('div');
        div.className = 'miniatura-item';
        div.innerHTML = `
            <img src="${url}" alt="Foto existente" onerror="this.src='https://placehold.co/100x100?text=Error+URL'">
            <button type="button" class="miniatura-eliminar" onclick="eliminarFotoExistenteCliente(${idx})">&times;</button>
        `;
        contenedor.appendChild(div);
    });

    // Fotos físicas seleccionadas pendientes de subir
    archivosClienteParaSubir.forEach((file, idx) => {
        const previewUrl = URL.createObjectURL(file);
        const div = document.createElement('div');
        div.className = 'miniatura-item';
        div.innerHTML = `
            <img src="${previewUrl}" alt="Previa local">
            <button type="button" class="miniatura-eliminar" onclick="eliminarArchivoCliente(${idx})">&times;</button>
        `;
        contenedor.appendChild(div);
    });
}

function sincronizarMockupCliente() {
    const imgElement = document.getElementById('mprev-img');
    if (!imgElement) return;

    if (archivosClienteParaSubir.length > 0) {
        imgElement.src = URL.createObjectURL(archivosClienteParaSubir[0]);
    } else if (imagenesClienteExistentes.length > 0) {
        imgElement.src = imagenesClienteExistentes[0];
    } else {
        imgElement.src = 'https://placehold.co/400x220?text=Sin+Imagen';
    }
}

// Guardar fotografías en Firestore (y Firebase Storage si hay archivos físicos)
window.guardarGaleriaCliente = async function() {
    if (!comercioClienteActual || !comercioClienteActual.id) {
        alert('No se encontró el identificador del comercio.');
        return;
    }

    const btn = document.getElementById('btn-guardar-fotos-cli');
    btn.disabled = true;
    btn.textContent = archivosClienteParaSubir.length > 0 ? 'Subiendo archivos a Firebase...' : 'Guardando en base de datos...';

    try {
        let todasLasUrls = [...imagenesClienteExistentes];

        // 1. Subir archivos binarios solo si el usuario seleccionó archivos de su computadora
        if (archivosClienteParaSubir.length > 0) {
            const formData = new FormData();
            archivosClienteParaSubir.forEach(archivo => {
                formData.append('archivos', archivo);
            });

            const resSubida = await fetchClienteAutenticado('/api/archivos/subir', {
                method: 'POST',
                body: formData
            });

            const nuevasUrls = await resSubida.json();
            todasLasUrls = [...todasLasUrls, ...nuevasUrls];
        }

        // 2. Actualizar el documento en Firestore directamente
        const payload = {
            ...comercioClienteActual,
            imagenUrls: todasLasUrls
        };
        delete payload.creadoEn;
        delete payload.actualizadoEn;

        await fetchClienteAutenticado(`/api/comercios/${comercioClienteActual.id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });

        comercioClienteActual.imagenUrls = todasLasUrls;
        imagenesClienteExistentes = [...todasLasUrls];
        archivosClienteParaSubir = [];
        renderizarMiniaturasCliente();
        actualizarModalPreview(comercioClienteActual);

        alert('Galería actualizada y guardada exitosamente.');
    } catch (error) {
        console.error('Error al guardar galería:', error);
        alert('Hubo un error al guardar los cambios.');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Guardar Fotografías';
    }
};

// ==========================================
// ACTUALIZACIÓN DE DATOS Y FORMULARIOS
// ==========================================
document.getElementById('formActualizacionRapida')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-guardar-cliente');
    btn.disabled = true;
    btn.textContent = 'Guardando...';

    if (!comercioClienteActual || !comercioClienteActual.id) {
        btn.disabled = false;
        btn.textContent = 'Guardar Cambios';
        return;
    }

    const payload = {
        ...comercioClienteActual,
        telefono: document.getElementById('cli-telefono').value,
        descripcion: document.getElementById('cli-oferta').value
    };
    delete payload.creadoEn;
    delete payload.actualizadoEn;

    try {
        await fetchClienteAutenticado(`/api/comercios/${comercioClienteActual.id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });

        comercioClienteActual = payload;
        actualizarModalPreview(comercioClienteActual);
        alert('Información operativa actualizada.');
    } catch (err) {
        console.error('Error al actualizar datos:', err);
        alert('Hubo un error al actualizar los datos en Firebase.');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Guardar Cambios';
    }
});

document.getElementById('formInformacionCompleta')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    if (!comercioClienteActual || !comercioClienteActual.id) return;

    const payload = {
        ...comercioClienteActual,
        nombre: document.getElementById('cli-nombre-completo').value,
        direccion: document.getElementById('cli-direccion').value,
        horario: document.getElementById('cli-horario').value,
        sitioWeb: document.getElementById('cli-web').value,
        precioDesde: parseFloat(document.getElementById('cli-precio').value) || 0
    };
    delete payload.creadoEn;
    delete payload.actualizadoEn;

    try {
        await fetchClienteAutenticado(`/api/comercios/${comercioClienteActual.id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });

        comercioClienteActual = payload;
        actualizarModalPreview(comercioClienteActual);
        alert('Ficha completa guardada.');
    } catch (err) {
        alert('Error al guardar la información.');
    }
});

// Navegación entre secciones
window.cambiarSeccionCliente = function(seccion) {
    ['general', 'informacion', 'galeria', 'resenas'].forEach(s => {
        const sec = document.getElementById('seccion-cliente-' + s);
        const nav = document.getElementById('cnav-' + s);
        if (sec) sec.classList.add('vista-oculta');
        if (nav) nav.classList.remove('active');
    });

    const secActiva = document.getElementById('seccion-cliente-' + seccion);
    const navActivo = document.getElementById('cnav-' + seccion);
    if (secActiva) secActiva.classList.remove('vista-oculta');
    if (navActivo) navActivo.classList.add('active');
};

// Modal de vista previa móvil
window.abrirModalVistaPrevia = function() {
    document.getElementById('modalPreview').classList.add('active');
};

window.cerrarModalVistaPrevia = function() {
    document.getElementById('modalPreview').classList.remove('active');
};

function actualizarModalPreview(com) {
    document.getElementById('mprev-nombre').textContent = com.nombre || 'Mi Negocio';
    document.getElementById('mprev-categoria').textContent = com.categoria || 'Local';
    document.getElementById('mprev-desc').textContent = com.descripcion || 'Sin descripción';
    document.getElementById('mprev-precio').textContent = com.precioDesde ? `Desde $${parseFloat(com.precioDesde).toFixed(2)}` : 'Desde $0.00';

    sincronizarMockupCliente();
}

window.cerrarSesionCliente = function() {
    localStorage.clear();
    window.location.replace('/login');
};