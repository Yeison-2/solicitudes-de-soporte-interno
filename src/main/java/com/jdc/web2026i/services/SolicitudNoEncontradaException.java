package com.jdc.web2026i.services;

/**
 * Se lanza cuando se solicita una solicitud que no existe.
 */
public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException() {
        super("La solicitud indicada no existe.");
    }
}
