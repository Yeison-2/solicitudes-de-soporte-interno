package com.jdc.web2026i.services;

/**
 * Se lanza cuando el usuario en sesion no tiene permiso sobre un recurso.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tiene permiso para acceder a este recurso.");
    }
}
