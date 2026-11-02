package com.turismap.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // Permite acceder a la landing tanto desde la raíz como desde /inicio
    @GetMapping({"/", "/inicio"})
    public String inicio() {
        return "inicio";
    }

    // Sirve la vista unificada de autenticación (Login y Crear Cuenta)
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Redirección amigable si alguien navega directamente a /registro
    @GetMapping("/registro")
    public String registro() {
        return "redirect:/login?tab=register";
    }

    // Vista del panel de administración
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    // Vista del portal exclusivo para clientes con plan Socio o Premium
    @GetMapping("/portal-cliente")
    public String portalCliente() {
        return "portal-cliente";
    }
}