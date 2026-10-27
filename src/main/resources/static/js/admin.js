
// Verificación de sesión del panel administrativo
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'admin') {
        cerrarSesion();
        return;
    }
    
    cargarComercios(); // Cargar negocios al iniciar
});

// Navegación entre las vistas del panel
function cambiarVista(vista) {
    const nuevaVista = document.getElementById('vista-' + vista);

    if (!nuevaVista) {
        console.warn('No existe la vista solicitada:', vista);
        return;
    }

    document.querySelectorAll('.vista-activa').forEach(el => {
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
    } else if (vista === 'negocios') {
        cargarComercios();
    }
}

// Petición HTTP autenticada para las llamadas a la API.
// Uso: fetchAutenticado('/api/admin/ejemplo')
async function fetchAutenticado(url, opciones = {}) {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'admin') {
        cerrarSesion();
        throw new Error('La sesión administrativa no está disponible.');
    }

    const urlRelativaPermitida =
        typeof url === 'string' &&
        url.startsWith('/') &&
        !url.startsWith('//');

    if (!urlRelativaPermitida) {
        throw new Error('La URL de la solicitud no es válida.');
    }

    const headers = new Headers(opciones.headers || {});

    headers.set('Authorization', `Bearer ${token}`);

    if (
        opciones.body &&
        !(opciones.body instanceof FormData) &&
        !headers.has('Content-Type')
    ) {
        headers.set('Content-Type', 'application/json');
    }

    const respuesta = await fetch(url, {
        ...opciones,
        headers
    });

    if (respuesta.status === 401 || respuesta.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('userTipo');
        localStorage.removeItem('userName');
        localStorage.removeItem('userCorreo');

        window.location.replace('/login');

        throw new Error(
            respuesta.status === 401
                ? 'La sesión expiró o no es válida.'
                : 'No tienes permisos para realizar esta operación.'
        );
    }

    return respuesta;
}

// Cierre de sesión local
function cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('userTipo');
    localStorage.removeItem('userName');
    localStorage.removeItem('userCorreo');

    window.location.replace('/login');
}

// Exponer funciones para que puedan usarse desde el HTML.
window.cambiarVista = cambiarVista;
window.cerrarSesion = cerrarSesion;
window.fetchAutenticado = fetchAutenticado;

// --- LÓGICA DE LUGARES (MUNICIPIOS) ---
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
    if (!tbody) return;
    
    tbody.innerHTML = '';
    lugaresCache.forEach(lugar => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><div class="table-main-text">${lugar.nombre}</div><small style="color:var(--text-muted)">${lugar.ubicacionCorta || ''}</small></td>
            <td>${lugar.categoria || 'TODOS'}</td>
            <td>⭐ ${lugar.calificacion || 'N/A'}</td>
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
    
    if (lugar.atractivos) {
        document.getElementById('lugar-atractivos-json').value = JSON.stringify(lugar.atractivos, null, 2);
    } else {
        document.getElementById('lugar-atractivos-json').value = '';
    }

    document.getElementById('lugar-form-title').innerText = 'Editar Datos: ' + lugar.nombre;
    document.getElementById('panel-tabla-lugares').classList.add('vista-oculta');
    document.getElementById('panel-form-lugar').classList.remove('vista-oculta');
};

window.eliminarLugar = async function(id) {
    if(!confirm('¿Estás seguro de eliminar este municipio? Esto es irreversible.')) return;
    try {
        await fetchAutenticado(`/api/lugares/${id}`, { method: 'DELETE' });
        cargarMunicipios();
    } catch(e) { 
        alert('Error al eliminar el lugar.'); 
    }
};

document.getElementById('formLugar')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    const btn = this.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Guardando...';

    const id = document.getElementById('lugar-id').value;
    
    let atractivos = null;
    try {
        const atTxt = document.getElementById('lugar-atractivos-json').value.trim();
        if(atTxt) atractivos = JSON.parse(atTxt);
    } catch(e) {
        alert('El JSON de Atractivos es inválido. Corrige la sintaxis.');
        btn.disabled = false;
        btn.textContent = '💾 Guardar Registro';
        return;
    }

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

    if(id) lugarPayload.id = id;

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
        btn.textContent = '💾 Guardar Registro';
    }
});

// --- LÓGICA DE USUARIOS ---
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
        const esAdmin = usuario.rol.toLowerCase() === 'admin';
        const badgeClass = esAdmin ? 'badge-admin' : 'badge-user';
        const btnHtml = esAdmin 
            ? '-' 
            : `<button class="btn-danger" onclick="ascenderAdmin('${usuario.email}')">Ascender a Admin</button>`;

        tr.innerHTML = `
            <td><div class="table-main-text">${usuario.nombre || 'Sin nombre'}</div></td>
            <td>${usuario.email}</td>
            <td><span class="badge ${badgeClass}">${usuario.rol}</span></td>
            <td>${btnHtml}</td>
        `;
        tbody.appendChild(tr);
    });
}

window.ascenderAdmin = async function(email) {
    if(!confirm(`¿Estás seguro de ascender a ${email} como Administrador?`)) return;
    try {
        await fetchAutenticado(`/api/usuarios/${email}/rol`, {
            method: 'PUT',
            body: JSON.stringify({ nuevoRol: 'admin' })
        });
        alert('Usuario ascendido a administrador en la base de datos.');
        cargarUsuarios();
    } catch (error) {
        console.error('Error al ascender:', error);
        alert('Error al modificar el rol del usuario.');
    }
};

// --- LÓGICA DE AGREGAR NEGOCIO ---
document.getElementById('formAgregarNegocio')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    const btn = this.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Guardando...';

    const comercio = {
        nombre: document.getElementById('nombre').value,
        lugarId: document.getElementById('municipio').value,
        categoria: document.getElementById('tipo').value,
        descripcion: document.getElementById('descripcion').value,
        direccion: document.getElementById('direccion').value,
        telefono: document.getElementById('telefono').value,
        sitioWeb: document.getElementById('sitioWeb').value,
        horario: document.getElementById('horario').value,
        precioDesde: parseFloat(document.getElementById('precioDesde').value) || 0,
        servicios: document.getElementById('llamativo').value.split(',').map(s => s.trim()).filter(Boolean),
        imagenUrls: document.getElementById('imagenUrls').value.split(',').map(s => s.trim()).filter(Boolean),
        propietarioUid: document.getElementById('propietarioUid').value,
        estado: 'pendiente',
        destacado: false
    };

    try {
        await fetchAutenticado('/api/comercios', {
            method: 'POST',
            body: JSON.stringify(comercio)
        });
        alert('Negocio registrado exitosamente en la base de datos.');
        this.reset();
        cargarComercios();
    } catch (error) {
        console.error('Error al guardar negocio:', error);
        alert('Error al registrar el negocio.');
    } finally {
        btn.disabled = false;
        btn.textContent = '✨ Guardar en Firebase';
    }
});

// --- LÓGICA DE COMERCIOS (NEGOCIOS) ---
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
                <button class="btn-danger" onclick="eliminarComercio('${com.id}')">Eliminar</button>
                ${com.estado !== 'publicado' ? `<button class="btn-secondary" onclick="publicarComercio('${com.id}')">Publicar</button>` : ''}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.eliminarComercio = async function(id) {
    if(!confirm('¿Estás seguro de eliminar este comercio?')) return;
    try {
        await fetchAutenticado(`/api/comercios/${id}`, { method: 'DELETE' });
        cargarComercios();
    } catch(e) { 
        alert('Error al eliminar comercio'); 
    }
};

window.publicarComercio = async function(id) {
    if(!confirm('¿Publicar este comercio para que sea visible a los turistas?')) return;
    try {
        await fetchAutenticado(`/api/comercios/${id}/publicar`, { method: 'PUT' });
        cargarComercios();
    } catch(e) { 
        alert('Error al publicar comercio'); 
    }
};
