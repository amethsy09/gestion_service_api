package com.example.gestionservice.config;

import com.example.gestionservice.exception.WalletCommunicationException;
import feign.Logger;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Feign pour le WalletClient.
 * - Niveau de log BASIC (jamais FULL — évite de logger le PIN)
 * - ErrorDecoder personnalisé pour mapper les erreurs Wallet en exceptions métier
 */
@Slf4j
@Configuration
public class FeignConfig {

    /**
     * BASIC : log méthode + URL + status. NEVER FULL (corps contient le PIN).
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder walletErrorDecoder() {
        return new WalletErrorDecoder();
    }

    public static class WalletErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            int status = response.status();
            log.warn("WalletClient erreur HTTP {} pour {}", status, methodKey);

            return switch (status) {
                case 400 -> new WalletCommunicationException(
                        "Requête invalide vers le Wallet (400) — vérifier les paramètres");
                case 401 -> new WalletCommunicationException(
                        "Non autorisé par le Wallet (401) — JWT invalide ou expiré");
                case 402 -> new WalletCommunicationException(
                        "Solde insuffisant (402)");
                case 403 -> new WalletCommunicationException(
                        "Accès refusé par le Wallet (403) — PIN invalide ou compte bloqué");
                case 404 -> new WalletCommunicationException(
                        "Ressource introuvable dans le Wallet (404)");
                case 409 -> new WalletCommunicationException(
                        "Paiement déjà traité (409) — idempotencyKey déjà utilisée");
                case 500, 502, 503, 504 -> new WalletCommunicationException(
                        "Le Wallet est temporairement indisponible (status=" + status + ")");
                default -> defaultDecoder.decode(methodKey, response);
            };
        }
    }
}
