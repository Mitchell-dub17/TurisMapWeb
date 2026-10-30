// ==========================================
// 1. SESIÓN Y NAVEGACIÓN
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'admin') {
        cerrarSesion();
        return;
    }

    inicializarDropzone();
    inicializarSoporteUrlDirectaAdmin();
    cargarComercios();
    cargarMunicipios();
});

function cambiarVista(vista) {
    const nuevaVista = document.getElementById('vista-' + vista);

    if (!nuevaVista) {
        console.warn('No existe la vista solicitada:', vista);
        return;
    }

    document.querySelectorAll('[id^="vista-"]').forEach(el => {
        el.classList.remove('vista-activa', 'fade-in');
        el.classList.add('vista-oculta');
    });

    document.querySelectorAll('.sidebar nav a').forEach(el => {
        el.classList.remove('active');
    });

    nuevaVista.classList.remove('vista-oculta');
    nuevaVista.classList.add('vista-activa', 'fade-in');

    const btnActivo = document.getElementById('nav-' + vista);
    if (btnActivo) {
        btnActivo.classList.add('active');
    }

    if (vista === 'municipios') {
        cargarMunicipios();
    } else if (vista === 'usuarios') {
        cargarUsuarios();
    } else if (vista === 'negocios' || vista === 'registrados') {
        cargarComercios();
    }
}

async function fetchAutenticado(url, opciones = {}) {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'admin') {
        cerrarSesion();
        throw new Error('La sesión administrativa no está disponible.');
    }

    const headers = new Headers(opciones.headers || {});
    headers.set('Authorization', `Bearer ${token}`);

    if (opciones.body && !(opciones.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const respuesta = await fetch(url, { ...opciones, headers });

    if (respuesta.status === 401 || respuesta.status === 403) {
        cerrarSesion();
        throw new Error('Sesión no autorizada o expirada.');
    }

    if (!respuesta.ok) {
        throw new Error(`Error HTTP: ${respuesta.status}`);
    }

    return respuesta;
}

function cerrarSesion() {
    localStorage.clear();
    window.location.replace('/login');
}

window.cambiarVista = cambiarVista;
window.cerrarSesion = cerrarSesion;
window.fetchAutenticado = fetchAutenticado;


// ==========================================
// 2. MANEJO DE ARCHIVOS Y FOTOGRAFÍAS (HÍBRIDO)
// ==========================================
let archivosParaSubir = [];
let imagenesExistentes = [];

function inicializarDropzone() {
    const dropzone = document.getElementById('dropzone');
    const inputFotos = document.getElementById('input-fotos');

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
        agregarArchivos(archivos);
    });

    inputFotos.addEventListener('change', () => {
        const archivos = Array.from(inputFotos.files);
        agregarArchivos(archivos);
        inputFotos.value = '';
    });
}

function inicializarSoporteUrlDirectaAdmin() {
    const btn = document.getElementById('btnAdminAgregarUrl');
    const input = document.getElementById('adminInputUrlDirecta');

    if (!btn || !input) return;

    btn.addEventListener('click', () => {
        const url = input.value.trim();

        if (!url) {
            alert('Por favor ingresa o pega una URL válida.');
            return;
        }

        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            alert('La URL debe comenzar con http:// o https://');
            return;
        }

        // Se incorpora directamente a las imágenes del comercio
        imagenesExistentes.push(url);
        renderizarMiniaturas();
        sincronizarMockupConPrimeraFoto();

        input.value = '';
    });
}

function agregarArchivos(nuevosArchivos) {
    archivosParaSubir = [...archivosParaSubir, ...nuevosArchivos];
    renderizarMiniaturas();
    sincronizarMockupConPrimeraFoto();
}

window.eliminarArchivo = function(index) {
    archivosParaSubir.splice(index, 1);
    renderizarMiniaturas();
    sincronizarMockupConPrimeraFoto();
};

window.eliminarFotoExistente = function(index) {
    imagenesExistentes.splice(index, 1);
    renderizarMiniaturas();
    sincronizarMockupConPrimeraFoto();
};

function renderizarMiniaturas() {
    const contenedor = document.getElementById('contenedor-miniaturas');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    // Fotos existentes o URLs agregadas directamente
    imagenesExistentes.forEach((url, idx) => {
        const div = document.createElement('div');
        div.className = 'miniatura-item';
        div.innerHTML = `
            <img src="${url}" alt="Foto existente" onerror="this.src='https://placehold.co/100x100?text=Error+URL'">
            <button type="button" class="miniatura-eliminar" onclick="eliminarFotoExistente(${idx})">&times;</button>
        `;
        contenedor.appendChild(div);
    });

    // Fotos locales cargadas desde el explorador
    archivosParaSubir.forEach((file, idx) => {
        const previewUrl = URL.createObjectURL(file);
        const div = document.createElement('div');
        div.className = 'miniatura-item';
        div.innerHTML = `
            <img src="${previewUrl}" alt="Previa local">
            <button type="button" class="miniatura-eliminar" onclick="eliminarArchivo(${idx})">&times;</button>
        `;
        contenedor.appendChild(div);
    });
}

function sincronizarMockupConPrimeraFoto() {
    const mockupImg = document.getElementById('prev-imagen');
    if (!mockupImg) return;

    if (archivosParaSubir.length > 0) {
        mockupImg.src = URL.createObjectURL(archivosParaSubir[0]);
    } else if (imagenesExistentes.length > 0) {
        mockupImg.src = imagenesExistentes[0];
    } else {
        mockupImg.src = 'https://placehold.co/400x220?text=Sin+Imagen';
    }
}


// ==========================================
// 3. COMERCIOS (AGREGAR / EDITAR / ELIMINAR)
// ==========================================
let comerciosCache = [];

async function cargarComercios() {
    try {
        const respuesta = await fetchAutenticado('/api/comercios');
        comerciosCache = await respuesta.json();
        renderComercios();
    } catch (error) {
        console.error('Error cargando comercios:', error);
    }
}

function renderComercios() {
    const tbody = document.getElementById('tabla-comercios-body');
    if (!tbody) return;
    tbody.innerHTML = '';

    comerciosCache.forEach(com => {
        const tr = document.createElement('tr');
        const estadoClase = com.estado === 'publicado' ? 'badge-admin' : 'badge-user';
        tr.innerHTML = `
            <td><div class="table-main-text">${com.nombre}</div></td>
            <td>${com.categoria}</td>
            <td><span class="badge ${estadoClase}">${com.estado}</span></td>
            <td>
                <button class="btn-secondary" style="margin-right: 5px;" onclick="editarComercio('${com.id}')">Editar</button>
                <button class="btn-danger" onclick="eliminarComercio('${com.id}')">Eliminar</button>
                ${com.estado !== 'publicado' ? `<button class="btn-secondary" style="margin-left: 5px;" onclick="publicarComercio('${com.id}')">Publicar</button>` : ''}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.cancelarEdicionComercio = function() {
    document.getElementById('formAgregarNegocio').reset();
    document.getElementById('comercio-id').value = '';
    archivosParaSubir = [];
    imagenesExistentes = [];
    renderizarMiniaturas();
    sincronizarMockupConPrimeraFoto();
    document.getElementById('btn-cancelar-comercio').style.display = 'none';
    document.getElementById('btn-submit-comercio').textContent = 'Guardar en Firebase';
};

window.editarComercio = function(id) {
    const com = comerciosCache.find(c => c.id === id);
    if (!com) return;

    cambiarVista('negocios');

    document.getElementById('comercio-id').value = com.id;
    document.getElementById('nombre').value = com.nombre || '';
    document.getElementById('municipio').value = com.lugarId || '';
    document.getElementById('tipo').value = com.categoria || '';
    document.getElementById('descripcion').value = com.descripcion || '';
    document.getElementById('direccion').value = com.direccion || '';
    document.getElementById('telefono').value = com.telefono || '';
    document.getElementById('sitioWeb').value = com.sitioWeb || '';
    document.getElementById('horario').value = com.horario || '';
    document.getElementById('precioDesde').value = com.precioDesde || '';
    document.getElementById('llamativo').value = (com.servicios || []).join(', ');
    document.getElementById('propietarioUid').value = com.propietarioUid || '';

    // Cargar fotos guardadas
    imagenesExistentes = Array.isArray(com.imagenUrls) ? [...com.imagenUrls] : [];
    archivosParaSubir = [];
    renderizarMiniaturas();
    sincronizarMockupConPrimeraFoto();

    document.getElementById('btn-cancelar-comercio').style.display = 'inline-block';
    document.getElementById('btn-submit-comercio').textContent = 'Guardar Cambios';

    window.scrollTo({ top: 0, behavior: 'smooth' });
};

document.getElementById('formAgregarNegocio')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-comercio');
    const id = document.getElementById('comercio-id').value;

    btn.disabled = true;
    btn.textContent = archivosParaSubir.length > 0 ? 'Subiendo archivos y guardando...' : 'Guardando cambios...';

    try {
        let todasLasUrls = [...imagenesExistentes];

        // 1. Subir archivos binarios solo si hay fotos pendientes en la cola
        if (archivosParaSubir.length > 0) {
            const formData = new FormData();
            archivosParaSubir.forEach(archivo => {
                formData.append('archivos', archivo);
            });

            const resSubida = await fetchAutenticado('/api/archivos/subir', {
                method: 'POST',
                body: formData
            });

            const nuevasUrls = await resSubida.json();
            todasLasUrls = [...todasLasUrls, ...nuevasUrls];
        }

        // 2. Construir el payload del comercio
        let comercio = {};
        if (id) {
            const original = comerciosCache.find(c => c.id === id);
            if (original) {
                comercio = { ...original };
                delete comercio.creadoEn;
                delete comercio.actualizadoEn;
            }
        } else {
            comercio.estado = 'pendiente';
            comercio.destacado = false;
        }

        comercio.nombre = document.getElementById('nombre').value;
        comercio.lugarId = document.getElementById('municipio').value;
        comercio.categoria = document.getElementById('tipo').value;
        comercio.descripcion = document.getElementById('descripcion').value;
        comercio.direccion = document.getElementById('direccion').value;
        comercio.telefono = document.getElementById('telefono').value;
        comercio.sitioWeb = document.getElementById('sitioWeb').value;
        comercio.horario = document.getElementById('horario').value;
        comercio.precioDesde = parseFloat(document.getElementById('precioDesde').value) || 0;
        comercio.servicios = document.getElementById('llamativo').value.split(',').map(s => s.trim()).filter(Boolean);
        comercio.propietarioUid = document.getElementById('propietarioUid').value;
        comercio.imagenUrls = todasLasUrls;

        // 3. Guardar directamente en Firestore
        const url = id ? `/api/comercios/${id}` : '/api/comercios';
        const method = id ? 'PUT' : 'POST';

        await fetchAutenticado(url, {
            method: method,
            body: JSON.stringify(comercio)
        });

        alert(id ? 'Comercio actualizado correctamente.' : 'Negocio registrado con imágenes exitosamente.');
        cancelarEdicionComercio();
        await cargarComercios();

        if (id) {
            cambiarVista('registrados');
        }
    } catch (error) {
        console.error('Error al procesar el negocio:', error);
        alert('Error al guardar el comercio en la base de datos.');
    } finally {
        btn.disabled = false;
        btn.textContent = document.getElementById('comercio-id').value ? 'Guardar Cambios' : 'Guardar en Firebase';
    }
});

window.eliminarComercio = async function(id) {
    if (!confirm('¿Deseas eliminar este comercio?')) return;
    try {
        await fetchAutenticado(`/api/comercios/${id}`, { method: 'DELETE' });
        cargarComercios();
    } catch (e) {
        alert('Error al eliminar comercio');
    }
};

window.publicarComercio = async function(id) {
    if (!confirm('¿Publicar este comercio para la aplicación móvil?')) return;
    try {
        await fetchAutenticado(`/api/comercios/${id}/publicar`, { method: 'PUT' });
        cargarComercios();
    } catch (e) {
        alert('Error al publicar comercio');
    }
};


// ==========================================
// 4. MUNICIPIOS Y ATRACTIVOS
// ==========================================
let lugaresCache = [];

async function cargarMunicipios() {
    try {
        const respuesta = await fetchAutenticado('/api/lugares');
        lugaresCache = await respuesta.json();
        renderMunicipios();
    } catch (error) {
        console.error('Error cargando municipios:', error);
    }
}

function renderMunicipios() {
    const tbody = document.getElementById('tabla-municipios-body');
    const selectMunicipio = document.getElementById('municipio');

    if (selectMunicipio) {
        selectMunicipio.innerHTML = '<option value="">Selecciona ubicación...</option>';
        lugaresCache.forEach(lugar => {
            const option = document.createElement('option');
            option.value = lugar.id;
            option.textContent = lugar.nombre;
            selectMunicipio.appendChild(option);
        });
    }

    if (!tbody) return;
    tbody.innerHTML = '';
    lugaresCache.forEach(lugar => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><div class="table-main-text">${lugar.nombre}</div><small style="color:var(--text-muted)">${lugar.ubicacionCorta || ''}</small></td>
            <td>${lugar.categoria || 'TODOS'}</td>
            <td>${lugar.calificacion ? lugar.calificacion.toFixed(1) : 'N/A'}</td>
            <td>${lugar.clima || 'N/A'}</td>
            <td>
                <button class="btn-secondary" onclick="editarLugarCompleto('${lugar.id}')">Ver/Editar</button>
                <button class="btn-danger" style="margin-left: 5px;" onclick="eliminarLugar('${lugar.id}')">Eliminar</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.abrirFormLugar = function() {
    document.getElementById('formLugar').reset();
    document.getElementById('lugar-id').value = '';
    document.getElementById('lugar-form-title').innerText = 'Registrar Nuevo Municipio';
    document.getElementById('atractivos-container').innerHTML = '';
    agregarAtractivoFila();
    document.getElementById('panel-tabla-lugares').classList.add('vista-oculta');
    document.getElementById('panel-form-lugar').classList.remove('vista-oculta');
};

window.cerrarFormLugar = function() {
    document.getElementById('panel-form-lugar').classList.add('vista-oculta');
    document.getElementById('panel-tabla-lugares').classList.remove('vista-oculta');
};

window.editarLugarCompleto = function(id) {
    const lugar = lugaresCache.find(l => l.id === id);
    if (!lugar) return;

    document.getElementById('lugar-id').value = lugar.id || '';
    document.getElementById('lugar-nombre').value = lugar.nombre || '';
    document.getElementById('lugar-subtitulo').value = lugar.subtitulo || '';
    document.getElementById('lugar-categoria').value = lugar.categoria || 'TODOS';
    document.getElementById('lugar-imagen').value = lugar.imagenPortadaUrl || '';
    document.getElementById('lugar-historia').value = lugar.historia || '';
    document.getElementById('lugar-ubiCorta').value = lugar.ubicacionCorta || '';
    document.getElementById('lugar-ubiLarga').value = lugar.ubicacionLarga || '';
    document.getElementById('lugar-elevacion').value = lugar.elevacion || '';
    document.getElementById('lugar-clima').value = lugar.clima || '';

    if (lugar.ubicacionCoordenadas) {
        document.getElementById('lugar-lat').value = lugar.ubicacionCoordenadas.latitud || '';
        document.getElementById('lugar-lng').value = lugar.ubicacionCoordenadas.longitud || '';
    } else {
        document.getElementById('lugar-lat').value = '';
        document.getElementById('lugar-lng').value = '';
    }

    document.getElementById('lugar-calificacion').value = lugar.calificacion || '';
    document.getElementById('lugar-resenas').value = lugar.cantidadResenas || '';
    document.getElementById('lugar-tempTit').value = lugar.mejorTemporadaTitulo || '';
    document.getElementById('lugar-tempDesc').value = lugar.mejorTemporadaDesc || '';

    if (lugar.directorioStats) {
        document.getElementById('stat-titGral').value = lugar.directorioStats.tituloGeneral || '';
        document.getElementById('stat-descGral').value = lugar.directorioStats.descripcionGeneral || '';
        document.getElementById('st1-ico').value = lugar.directorioStats.stat1Icon || '';
        document.getElementById('st1-txt').value = lugar.directorioStats.stat1Texto || '';
        document.getElementById('st2-ico').value = lugar.directorioStats.stat2Icon || '';
        document.getElementById('st2-txt').value = lugar.directorioStats.stat2Texto || '';
        document.getElementById('st3-ico').value = lugar.directorioStats.stat3Icon || '';
        document.getElementById('st3-txt').value = lugar.directorioStats.stat3Texto || '';
        document.getElementById('st4-ico').value = lugar.directorioStats.stat4Icon || '';
        document.getElementById('st4-txt').value = lugar.directorioStats.stat4Texto || '';
    }

    const container = document.getElementById('atractivos-container');
    container.innerHTML = '';
    if (lugar.atractivos && lugar.atractivos.length > 0) {
        lugar.atractivos.forEach(atractivo => agregarAtractivoFila(atractivo));
    } else {
        agregarAtractivoFila();
    }

    document.getElementById('lugar-form-title').innerText = 'Editar Datos: ' + lugar.nombre;
    document.getElementById('panel-tabla-lugares').classList.add('vista-oculta');
    document.getElementById('panel-form-lugar').classList.remove('vista-oculta');
};

window.eliminarLugar = async function(id) {
    if (!confirm('¿Deseas eliminar este municipio?')) return;
    try {
        await fetchAutenticado(`/api/lugares/${id}`, { method: 'DELETE' });
        cargarMunicipios();
    } catch (e) {
        alert('Error al eliminar el municipio.');
    }
};

window.agregarAtractivoFila = function(atractivo = null) {
    const container = document.getElementById('atractivos-container');
    const div = document.createElement('div');
    div.className = 'atractivo-row';
    div.style.marginBottom = '15px';
    div.style.padding = '15px';
    div.style.background = '#f8fafc';
    div.style.border = '1px solid #cbd5e1';
    div.style.borderRadius = '8px';

    const idValue = atractivo?.id || '';
    const titulo = atractivo?.titulo || '';
    const descripcion = atractivo?.descripcion || '';
    const etiqueta = atractivo?.etiqueta || '';
    const accion = atractivo?.accion || 'Ver más';
    const imagenUrl = atractivo?.imagenUrl || '';
    const colorHex = atractivo?.etiquetaColorHex || 4279986484;

    div.innerHTML = `
        <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
            <strong style="color:var(--primary);">Atractivo</strong>
            <button type="button" class="btn-danger" style="padding: 4px 8px; font-size: 11px;" onclick="this.parentElement.parentElement.remove()">Eliminar</button>
        </div>
        <input type="hidden" class="atr-id" value="${idValue}">
        <input type="hidden" class="atr-color" value="${colorHex}">
        <div class="form-row" style="margin-bottom: 10px;">
            <div class="form-group flex-1"><label>Título</label><input type="text" class="atr-titulo" value="${titulo}" required></div>
            <div class="form-group flex-1"><label>Etiqueta</label><input type="text" class="atr-etiqueta" value="${etiqueta}" required></div>
            <div class="form-group flex-1"><label>Acción</label><input type="text" class="atr-accion" value="${accion}" required></div>
        </div>
        <div class="form-row" style="margin-bottom: 10px;">
            <div class="form-group flex-2"><label>Descripción</label><input type="text" class="atr-descripcion" value="${descripcion}" required></div>
        </div>
        <div class="form-row" style="margin-bottom: 0;">
            <div class="form-group flex-1"><label>URL de Imagen</label><input type="url" class="atr-imagen" value="${imagenUrl}"></div>
        </div>
    `;
    container.appendChild(div);
};

document.getElementById('formLugar')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    const btn = this.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Guardando...';

    const id = document.getElementById('lugar-id').value;
    let atractivos = [];

    document.querySelectorAll('.atractivo-row').forEach(row => {
        let atrId = row.querySelector('.atr-id').value;
        if (!atrId) atrId = 'atr_' + Math.random().toString(36).substr(2, 9);

        atractivos.push({
            id: atrId,
            etiqueta: row.querySelector('.atr-etiqueta').value,
            etiquetaColorHex: parseInt(row.querySelector('.atr-color').value) || 4279986484,
            titulo: row.querySelector('.atr-titulo').value,
            descripcion: row.querySelector('.atr-descripcion').value,
            accion: row.querySelector('.atr-accion').value,
            imagenUrl: row.querySelector('.atr-imagen').value
        });
    });

    const lugarPayload = {
        nombre: document.getElementById('lugar-nombre').value,
        subtitulo: document.getElementById('lugar-subtitulo').value,
        categoria: document.getElementById('lugar-categoria').value,
        imagenPortadaUrl: document.getElementById('lugar-imagen').value,
        historia: document.getElementById('lugar-historia').value,
        ubicacionCorta: document.getElementById('lugar-ubiCorta').value,
        ubicacionLarga: document.getElementById('lugar-ubiLarga').value,
        elevacion: document.getElementById('lugar-elevacion').value,
        clima: document.getElementById('lugar-clima').value,
        calificacion: parseFloat(document.getElementById('lugar-calificacion').value) || null,
        cantidadResenas: parseInt(document.getElementById('lugar-resenas').value) || null,
        mejorTemporadaTitulo: document.getElementById('lugar-tempTit').value,
        mejorTemporadaDesc: document.getElementById('lugar-tempDesc').value,
        ubicacionCoordenadas: {
            latitud: parseFloat(document.getElementById('lugar-lat').value) || null,
            longitud: parseFloat(document.getElementById('lugar-lng').value) || null
        },
        directorioStats: {
            tituloGeneral: document.getElementById('stat-titGral').value,
            descripcionGeneral: document.getElementById('stat-descGral').value,
            stat1Icon: document.getElementById('st1-ico').value,
            stat1Texto: document.getElementById('st1-txt').value,
            stat2Icon: document.getElementById('st2-ico').value,
            stat2Texto: document.getElementById('st2-txt').value,
            stat3Icon: document.getElementById('st3-ico').value,
            stat3Texto: document.getElementById('st3-txt').value,
            stat4Icon: document.getElementById('st4-ico').value,
            stat4Texto: document.getElementById('st4-txt').value
        },
        atractivos: atractivos
    };

    if (id) lugarPayload.id = id;

    try {
        const url = id ? `/api/lugares/${id}` : '/api/lugares';
        const method = id ? 'PUT' : 'POST';

        await fetchAutenticado(url, {
            method: method,
            body: JSON.stringify(lugarPayload)
        });

        alert('Municipio guardado correctamente.');
        cerrarFormLugar();
        cargarMunicipios();
    } catch (error) {
        console.error('Error al guardar:', error);
        alert('Hubo un error al guardar los datos.');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Guardar Registro';
    }
});


// ==========================================
// 5. GESTIÓN DE USUARIOS
// ==========================================
let usuariosCache = [];

async function cargarUsuarios() {
    try {
        const respuesta = await fetchAutenticado('/api/usuarios');
        usuariosCache = await respuesta.json();
        renderUsuarios();
    } catch (error) {
        console.error('Error cargando usuarios:', error);
    }
}

function renderUsuarios() {
    const tbody = document.getElementById('tabla-usuarios-body');
    if (!tbody) return;

    tbody.innerHTML = '';
    usuariosCache.forEach(usuario => {
        const tr = document.createElement('tr');
        const rolNormalizado = (usuario.rol || '').toLowerCase();

        tr.innerHTML = `
            <td><div class="table-main-text">${usuario.nombre || 'Sin nombre'}</div></td>
            <td>${usuario.email}</td>
            <td>
                <select onchange="cambiarRolUsuario('${usuario.id}', this.value)" style="width: 120px; padding: 6px; border-radius: 6px; border: 1px solid var(--border-color);">
                    <option value="admin" ${rolNormalizado === 'admin' ? 'selected' : ''}>Admin</option>
                    <option value="cliente" ${rolNormalizado === 'cliente' ? 'selected' : ''}>Cliente</option>
                    <option value="viajero" ${rolNormalizado === 'viajero' ? 'selected' : ''}>Viajero</option>
                </select>
            </td>
            <td>
                <button class="btn-danger" onclick="eliminarUsuarioPlataforma('${usuario.id}')">Dar de baja</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.cambiarRolUsuario = async function(id, nuevoRol) {
    if (!confirm(`¿Deseas cambiar el rol a ${nuevoRol}?`)) {
        cargarUsuarios();
        return;
    }
    try {
        await fetchAutenticado(`/api/usuarios/${id}/rol`, {
            method: 'PUT',
            body: JSON.stringify({ nuevoRol: nuevoRol })
        });
        alert('Rol modificado exitosamente.');
        cargarUsuarios();
    } catch (error) {
        console.error('Error al cambiar rol:', error);
        alert('Error al modificar el rol del usuario.');
        cargarUsuarios();
    }
};

window.eliminarUsuarioPlataforma = async function(id) {
    if (!confirm('¿Deseas dar de baja a este usuario permanentemente?')) return;
    try {
        await fetchAutenticado(`/api/usuarios/${id}`, { method: 'DELETE' });
        alert('Usuario eliminado correctamente.');
        cargarUsuarios();
    } catch (error) {
        console.error('Error al eliminar usuario:', error);
        alert('Error al eliminar el usuario.');
    }
};