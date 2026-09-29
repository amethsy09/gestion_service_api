package com.example.gestionservice.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String entity, UUID id) {
        super(entity + " introuvable avec l'identifiant : " + id);
    }
    public ResourceNotFoundException(String entity, String field, String value) {
        super(entity + " introuvable avec " + field + " : " + value);
    }
}
