package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de auditoria de solo lectura. No existe ninguna ruta ni metodo
 * para modificar o borrar registros.
 */
@Controller
public class AuditoriaController {

    private static final int TAMANO_PAGINA = 50;
    private static final int VENTANA_PAGINAS = 5;

    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UsuarioService usuarioService;

    public AuditoriaController(RegistroAuditoriaRepository registroAuditoriaRepository,
                               UsuarioService usuarioService) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/auditoria")
    public String lista(@RequestParam(value = "solicitudId", required = false) String solicitudId,
                        @RequestParam(value = "desde", required = false) String desde,
                        @RequestParam(value = "hasta", required = false) String hasta,
                        @RequestParam(value = "page", required = false) String page,
                        Authentication autenticacion,
                        Model model) {
        Long solicitudAplicada = numeroOpcional(solicitudId);
        LocalDate desdeAplicado = fechaOpcional(desde);
        LocalDate hastaAplicado = fechaOpcional(hasta);
        int pagina = paginaOpcional(page);
        Page<RegistroAuditoriaEntity> resultado = buscarPagina(pagina, solicitudAplicada, desdeAplicado, hastaAplicado);
        if (pagina > 0 && resultado.getTotalPages() > 0 && pagina >= resultado.getTotalPages()) {
            pagina = resultado.getTotalPages() - 1;
            resultado = buscarPagina(pagina, solicitudAplicada, desdeAplicado, hastaAplicado);
        }

        List<Integer> numeros = new ArrayList<>();
        int primeraVentana = Math.max(0, resultado.getNumber() - VENTANA_PAGINAS);
        int ultimaVentana = Math.min(resultado.getTotalPages() - 1, resultado.getNumber() + VENTANA_PAGINAS);
        for (int numero = primeraVentana; numero <= ultimaVentana; numero++) {
            numeros.add(numero);
        }

        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Historial de auditoria");
        model.addAttribute("registros", resultado.getContent());
        model.addAttribute("pagina", resultado.getNumber());
        model.addAttribute("totalPaginas", resultado.getTotalPages());
        model.addAttribute("totalRegistros", resultado.getTotalElements());
        model.addAttribute("numerosDePagina", numeros);
        model.addAttribute("solicitudId", solicitudAplicada == null ? "" : solicitudAplicada.toString());
        model.addAttribute("desde", desdeAplicado == null ? "" : desdeAplicado.toString());
        model.addAttribute("hasta", hastaAplicado == null ? "" : hastaAplicado.toString());
        return "auditoria/lista";
    }

    private Page<RegistroAuditoriaEntity> buscarPagina(int pagina, Long solicitudAplicada,
                                                      LocalDate desdeAplicado, LocalDate hastaAplicado) {
        return registroAuditoriaRepository.buscar(
                solicitudAplicada == null ? 0L : solicitudAplicada,
                solicitudAplicada == null,
                desdeAplicado == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : desdeAplicado.atStartOfDay(),
                desdeAplicado == null,
                hastaAplicado == null ? LocalDateTime.of(9999, 12, 31, 0, 0)
                        : hastaAplicado.plusDays(1).atStartOfDay(),
                hastaAplicado == null,
                PageRequest.of(pagina, TAMANO_PAGINA));
    }

    private int paginaOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            int pagina = Integer.parseInt(valor.trim());
            return pagina < 0 ? 0 : pagina;
        } catch (NumberFormatException excepcion) {
            return 0;
        }
    }

    private Long numeroOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private LocalDate fechaOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException excepcion) {
            return null;
        }
    }
}
