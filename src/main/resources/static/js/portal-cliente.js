// Verificación de sesión del portal del cliente
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'cliente') {
        cerrarSesion();
    }
});

// Petición HTTP autenticada para las llamadas a la API.
// Uso: fetchAutenticado('/api/cliente/resenas')
async function fetchAutenticado(url, opciones = {}) {
    const token = localStorage.getItem('token');
    const rol = localStorage.getItem('userTipo');

    if (!token || rol !== 'cliente') {
        cerrarSesion();
        throw new Error('La sesión del cliente no está disponible.');
    }

    if (
        typeof url !== 'string' ||
        !url.startsWith('/') ||
        url.startsWith('//')
    ) {
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
        cerrarSesion();

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

// Exponer funciones para utilizarlas desde el HTML
window.cerrarSesion = cerrarSesion;
window.fetchAutenticado = fetchAutenticado;
