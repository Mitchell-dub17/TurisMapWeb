import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import {
    getAuth,
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signOut
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";

console.log("🟢 [TurisMap] auth.js cargado correctamente");

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

// Elementos del DOM
const loginForm = document.getElementById("loginForm");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const errorMsg = document.getElementById("errorMsg");

const registerForm = document.getElementById("registerForm");
const regNombre = document.getElementById("regNombre");
const regEmail = document.getElementById("regEmail");
const regTelefono = document.getElementById("regTelefono");
const regPassword = document.getElementById("regPassword");
const regPasswordConfirm = document.getElementById("regPasswordConfirm");

// ==========================================================
// 1. CONTROLADOR DE LOGIN
// ==========================================================
if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        console.log("🔵 [TurisMap] Formulario de login enviado");

        const submitBtn = document.getElementById("btnLoginSubmit") || loginForm.querySelector('button[type="submit"]');
        const email = emailInput ? emailInput.value.trim() : "";
        const password = passwordInput ? passwordInput.value : "";

        if (!email || !password) {
            mostrarError("Introduce tu correo y contraseña.");
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Verificando credenciales...";
        }

        ocultarError();

        try {
            localStorage.removeItem("token");
            localStorage.removeItem("userTipo");
            localStorage.removeItem("userName");
            localStorage.removeItem("userCorreo");
            localStorage.removeItem("userTelefono");

            console.log("🟡 [TurisMap] Autenticando con Firebase:", email);
            const userCredential = await signInWithEmailAndPassword(auth, email, password);

            console.log("🟡 [TurisMap] Obteniendo token de Firebase...");
            const firebaseIdToken = await userCredential.user.getIdToken();

            console.log("🟡 [TurisMap] Validando credenciales con Spring Boot (/api/auth/login)...");
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({ token: firebaseIdToken })
            });

            const responseText = await response.text();
            let data = {};

            if (responseText) {
                try {
                    data = JSON.parse(responseText);
                } catch {
                    data = { message: responseText };
                }
            }

            if (!response.ok) {
                const message = typeof data.message === "string"
                    ? data.message
                    : typeof data === "string"
                        ? data
                        : `Error del servidor (código ${response.status}).`;

                throw new Error(message);
            }

            if (!data.token || typeof data.tipo !== "string" || typeof data.redirectUrl !== "string") {
                throw new Error("El servidor devolvió una respuesta incompleta.");
            }

            const rol = data.tipo.toLowerCase();
            console.log("🟢 [TurisMap] Login exitoso. Rol detectado:", rol);

            localStorage.setItem("token", data.token);
            localStorage.setItem("userTipo", rol);
            localStorage.setItem("userName", data.nombre || "");
            localStorage.setItem("userCorreo", data.correo || "");
            localStorage.setItem("userTelefono", data.telefono || "");

            const returnToPlanes = new URLSearchParams(window.location.search).get("returnTo") === "planes";
            window.location.assign(returnToPlanes ? "/#negocios" : data.redirectUrl);

        } catch (error) {
            console.error("🔴 [TurisMap] Error durante el login:", error);

            localStorage.clear();

            try {
                await signOut(auth);
            } catch (signOutError) {
                console.error("Error cerrando sesión en Firebase:", signOutError);
            }

            mostrarError(obtenerMensajeError(error));

        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = "Ingresar a TurisMap";
            }
        }
    });
}

// ==========================================================
// 2. CONTROLADOR DE REGISTRO
// ==========================================================
if (registerForm) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const submitBtn = document.getElementById("btnRegisterSubmit") || registerForm.querySelector('button[type="submit"]');
        const nombre = regNombre.value.trim();
        const email = regEmail.value.trim();
        const telefono = regTelefono.value.trim();
        const password = regPassword.value;
        const confirmPassword = regPasswordConfirm.value;
        ocultarError();

        if (password !== confirmPassword) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        if (password.length < 6) {
            mostrarError("La contraseña debe tener mínimo 6 caracteres.");
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Registrando usuario...";
        }

        let cuentaFirebaseCreada = false;
        try {
            const userCredential = await createUserWithEmailAndPassword(auth, email, password);
            cuentaFirebaseCreada = true;

            const response = await fetch("/api/auth/registro", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({
                    token: await userCredential.user.getIdToken(),
                    nombre: nombre,
                    telefono: telefono
                })
            });

            if (!response.ok) {
                const errorBackend = await response.text();
                throw new Error(errorBackend || "No se pudo registrar en Firestore.");
            }

            await signOut(auth);
            localStorage.removeItem("token");
            localStorage.removeItem("userTipo");
            localStorage.removeItem("userName");
            localStorage.removeItem("userCorreo");
            localStorage.removeItem("userTelefono");
            emailInput.value = email;
            regPassword.value = "";
            regPasswordConfirm.value = "";
            cambiarPestaña('login');
            alert("Cuenta creada. Ahora inicia sesión con tu correo y contraseña para continuar con la solicitud.");

        } catch (error) {
            console.error("🔴 [TurisMap] Error en registro:", error);
            if (cuentaFirebaseCreada) {
                try {
                    await signOut(auth);
                } catch (signOutError) {
                    console.error("Error cerrando la sesión temporal de Firebase:", signOutError);
                }
            }
            mostrarError(obtenerMensajeError(error));
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = "Crear Cuenta";
            }
        }
    });
}

// ==========================================================
// UTILIDADES DE INTERFAZ Y ERRORES
// ==========================================================
function mostrarError(mensaje) {
    if (errorMsg) {
        errorMsg.textContent = `❌ ${mensaje}`;
        errorMsg.style.display = "block";
    } else {
        alert(mensaje);
    }
}

function ocultarError() {
    if (errorMsg) {
        errorMsg.textContent = "";
        errorMsg.style.display = "none";
    }
}

function obtenerMensajeError(error) {
    const codigo = error?.code;

    switch (codigo) {
        case "auth/invalid-credential":
        case "auth/wrong-password":
        case "auth/user-not-found":
            return "Correo o contraseña incorrectos.";
        case "auth/email-already-in-use":
            return "Este correo ya está registrado en Firebase.";
        case "auth/invalid-email":
            return "El formato del correo electrónico no es válido.";
        case "auth/too-many-requests":
            return "Demasiados intentos fallidos. Inténtalo más tarde.";
        case "auth/network-request-failed":
            return "No se pudo conectar con Firebase. Comprueba tu conexión a Internet.";
        case "auth/user-disabled":
            return "Esta cuenta de usuario ha sido inhabilitada.";
        default:
            return error?.message || "No se pudo completar la operación. Inténtalo nuevamente.";
    }
}

window.cambiarPestaña = function(pestaña) {
    const fLogin = document.getElementById('loginForm');
    const fRegister = document.getElementById('registerForm');
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');

    ocultarError();

    if (pestaña === 'register') {
        fLogin.style.display = "none";
        fRegister.style.display = "flex";
        tabLogin.classList.remove('active');
        tabRegister.classList.add('active');
    } else {
        fRegister.style.display = "none";
        fLogin.style.display = "flex";
        tabRegister.classList.remove('active');
        tabLogin.classList.add('active');
    }
};