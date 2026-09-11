package com.example.gestionservice.exception;

public class WalletCommunicationException extends RuntimeException {
    public WalletCommunicationException(String message) {
        super(message);
    }
    public WalletCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
