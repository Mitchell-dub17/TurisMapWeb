import {
    initializeApp
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";

import {
    getAuth,
    signInWithEmailAndPassword,
    signOut
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";

// Configuración web de Firebase
const firebaseConfig = {
    apiKey: "AIzaSyCRdPUMD1oKb-MM3kvfTP-zQAmqfvSYc8A",
    authDomain: "turismap-2026.firebaseapp.com",
    projectId: "turismap-2026",
    storageBucket: "turismap-2026.firebasestorage.app",
    messagingSenderId: "768764285403",
    appId: "1:768764285403:web:8a0c95f0422b12796317fe",
    measurementId: "G-Y09R5HXPR5"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

const loginForm = document.getElementById("loginForm");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const errorMsg = document.getElementById("errorMsg");

if (loginForm && emailInput && passwordInput && errorMsg) {

    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const submitBtn = loginForm.querySelector(
            'button[type="submit"]'
        );

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        if (!email || !password) {
            mostrarError("Introduce tu correo y contraseña.");
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Autenticando...";
        }

        ocultarError();

        try {
            // Limpiar credenciales anteriores para evitar reutilizar
            // un token de una sesión previa.
            localStorage.removeItem("token");
            localStorage.removeItem("userTipo");
            localStorage.removeItem("userName");
            localStorage.removeItem("userCorreo");

            // 1. Autenticar al usuario con Firebase Authentication.
            const userCredential = await signInWithEmailAndPassword(
                auth,
                email,
                password
            );

            // 2. Obtener el ID token de Firebase.
            const firebaseIdToken =
                await userCredential.user.getIdToken();

            // 3. Validar la identidad y obtener el JWT de TurisMap.
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({
                    token: firebaseIdToken
                })
            });

            // El backend puede devolver JSON o texto plano.
            const responseText = await response.text();

            let data = {};

            if (responseText) {
                try {
                    data = JSON.parse(responseText);
                } catch {
                    data = {
                        message: responseText
                    };
                }
            }

            if (!response.ok) {
                const message =
                    typeof data.message === "string"
                        ? data.message
                        : typeof data === "string"
                            ? data
                            : `El servidor respondió con HTTP ${response.status}.`;

                throw new Error(message);
            }

            // 4. Comprobar que la respuesta contiene los datos esperados.
            if (
                !data.token ||
                typeof data.tipo !== "string" ||
                typeof data.redirectUrl !== "string"
            ) {
                throw new Error(
                    "El servidor devolvió una respuesta de autenticación incompleta."
                );
            }

            const rol = data.tipo.toLowerCase();

            // Solo permitir los roles definidos para TurisMap.
            if (rol !== "admin" && rol !== "cliente") {
                throw new Error(
                    "El servidor devolvió un rol no reconocido."
                );
            }

            // Solo permitir redirecciones locales conocidas.
            const rutasPermitidas = {
                admin: "/admin",
                cliente: "/portal-cliente"
            };

            const rutaEsperada = rutasPermitidas[rol];

            if (data.redirectUrl !== rutaEsperada) {
                throw new Error(
                    "La ruta de redirección recibida no es válida."
                );
            }

            // 5. Guardar el JWT emitido por Spring Boot.
            // No guardar el ID token de Firebase como JWT del backend.
            localStorage.setItem("token", data.token);
            localStorage.setItem("userTipo", rol);
            localStorage.setItem("userName", data.nombre || "");
            localStorage.setItem("userCorreo", data.correo || "");

            // 6. Navegar al portal correspondiente.
            window.location.assign(rutaEsperada);

        } catch (error) {
            console.error("Error durante el inicio de sesión:", error);

            // Eliminar cualquier credencial incompleta.
            localStorage.removeItem("token");
            localStorage.removeItem("userTipo");
            localStorage.removeItem("userName");
            localStorage.removeItem("userCorreo");

            // Cerrar la sesión de Firebase si se había iniciado.
            try {
                await signOut(auth);
            } catch (signOutError) {
                console.error(
                    "No se pudo cerrar la sesión de Firebase:",
                    signOutError
                );
            }

            mostrarError(obtenerMensajeError(error));

        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = "Ingresar al Sistema";
            }
        }
    });

} else {
    console.error(
        "No se encontraron todos los elementos necesarios del formulario de login."
    );
}

function mostrarError(mensaje) {
    errorMsg.textContent = `❌ ${mensaje}`;
    errorMsg.style.display = "block";
}

function ocultarError() {
    errorMsg.textContent = "";
    errorMsg.style.display = "none";
}

function obtenerMensajeError(error) {
    const codigo = error?.code;

    switch (codigo) {
        case "auth/invalid-credential":
        case "auth/wrong-password":
        case "auth/user-not-found":
            return "Correo o contraseña incorrectos.";

        case "auth/invalid-email":
            return "El formato del correo electrónico no es válido.";

        case "auth/too-many-requests":
            return "Se han realizado demasiados intentos. Inténtalo más tarde.";

        case "auth/network-request-failed":
            return "No se pudo conectar con Firebase. Comprueba tu conexión.";

        case "auth/user-disabled":
            return "Esta cuenta de Firebase está deshabilitada.";

        default:
            return error?.message ||
                "No se pudo iniciar sesión. Inténtalo nuevamente.";
    }
}
