# Gestion Service API

Microservice backend de gestion de services et prestations, développé avec Java 21 et Spring Boot 3.4.1.

---

## Présentation

Ce microservice est responsable de :

- La gestion d'un catalogue de services
- La création et le suivi de demandes de service
- La création et la gestion de prestations
- L'affectation de responsables et ressources humaines aux prestations
- La gestion des spécialités des ressources
- La création et le suivi des tâches
- L'initiation des paiements via le Wallet externe
- La gestion des paiements échoués, des retries et de la compensation

**Ce microservice ne gère PAS :** les utilisateurs, l'authentification, les comptes, le solde, le PIN.  
Ces responsabilités appartiennent au **Wallet externe**.

---

## Architecture

```
Controller → Service → Repository → PostgreSQL
                ↓
         WalletClient (OpenFeign)
                ↓
         Wallet Service (externe)
```

Chaque couche a une responsabilité unique. Les entités JPA ne sont jamais exposées directement — on utilise des DTOs séparés (Request / Response).

---

## Technologies

| Technologie | Version | Rôle |
|---|---|---|
| Java | 21 | Langage |
| Spring Boot | 3.4.1 | Framework principal |
| Spring Data JPA | — | ORM / accès données |
| Spring Security | — | Sécurité, JWT |
| Spring Validation | — | Bean Validation |
| Spring Actuator | — | Health, info |
| PostgreSQL | 16 | Base de données |
| Flyway | — | Migrations SQL |
| OpenFeign | Spring Cloud 2024.0.0 | Client HTTP Wallet |
| MapStruct | 1.6.3 | Mapping Entity ↔ DTO |
| Lombok | — | Réduction boilerplate |
| JJWT | 0.11.5 | Validation JWT |
| Springdoc OpenAPI | 2.7.0 | Swagger UI |
| Testcontainers | 1.20.4 | Tests d'intégration |
| Docker | — | Conteneurisation |

---

## Prérequis

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (optionnel si Docker)

---

## Installation

### 1. Cloner le projet

```bash
git clone <repo-url>
cd gestion-service-api
```

### 2. Configurer les secrets

```bash
cp application-secrets.yml.example application-secrets.yml
```

Éditer `application-secrets.yml` avec vos valeurs :

```yaml
DATABASE_URL: jdbc:postgresql://localhost:5432/gestion_service_db
DATABASE_USERNAME: postgres
DATABASE_PASSWORD: votre_mot_de_passe
JWT_SECRET: votre_secret_jwt_min_32_caracteres
WALLET_SERVICE_URL: http://localhost:8081
```

---

## Lancement

### Avec Docker (recommandé)

```bash
docker compose up --build
```

L'API sera disponible sur : `http://localhost:8080`

Arrêter et supprimer les volumes :
```bash
docker compose down -v
```

### Avec Maven (développement local)

Démarrer PostgreSQL, puis :

```bash
./mvnw spring-boot:run
```

Ou avec le profil dev explicite :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Base de données

- Nom : `gestion_service_db`
- Port : `5432`
- Migrations Flyway : `src/main/resources/db/migration/`

Les migrations s'exécutent automatiquement au démarrage.

| Migration | Table | Description |
|---|---|---|
| V1 | `service_catalog` | Catalogue + données initiales |
| V2 | `service_requests` | Demandes de service |
| V3 | `responsibles` | Responsables de prestation |
| V4 | `prestations` | Prestations |
| V5 | `specialties` | Spécialités + données initiales |
| V6 | `resources` | Ressources humaines |
| V7 | `prestation_resources` | Affectation ressources ↔ prestations |
| V8 | `tasks` | Tâches |
| V9 | `payment_attempts` | Historique tentatives paiement |

---

## Swagger

Accessible en mode développement uniquement :

```
http://localhost:8080/swagger-ui/index.html
```

Désactivé en production (`application-prod.yml`).

---

## Déploiement pour un frontend

### Configuration CORS (déjà activée)

Le `SecurityConfig` active le CORS pour les origines suivantes :

- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `http://localhost:5173` (Vite)
- `http://localhost:8080` (Spring Boot)

Pour adapter l'URL de production, définir la variable d'environnement :
```bash
CORS_ALLOWED_ORIGINS=https://votre-frontend.com,https://admin.votre-frontend.com
```

### Authentification

Le frontend doit :

1. Obtenir un **JWT** du Wallet externe (pas de `/login` ici).
2. Ajouter le header `Authorization: Bearer <JWT_TOKEN>` à chaque requête.
3. Envoyer le **PIN** dans le body de `POST /api/v1/service-requests/{id}/pay` (uniquement pour les paiements).

```javascript
// Exemple fetch
const response = await fetch('/api/v1/services', {
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  }
});
```

### Flux de paiement (frontend)

```
1. GET /api/v1/services/active          → choisir un service
2. POST /api/v1/service-requests        → créer la demande (JWT requis)
3. POST /api/v1/service-requests/{id}/pay  → payer (body: { "pin": "1234" })
4. Si timeout → POST /api/v1/service-requests/{id}/payment/sync
5. Si échec → POST /api/v1/service-requests/{id}/retry-payment
```

### Headers spéciaux

| Header | Où l'envoyer | Description |
|---|---|---|
| `Authorization` | Toutes les requêtes (sauf public) | `Bearer <JWT>` |
| `Content-Type` | POST/PUT/PATCH | `application/json` |

### Codes HTTP

| Code | Signification |
|---|---|
| 200 | Succès |
| 201 | Créé (demande, prestation, tâche, ressource…) |
| 400 | Erreur de validation (body invalide) |
| 401 | Non authentifié (JWT manquant/expiré) |
| 403 | Accès refusé (mauvais rôle / ownership) |
| 404 | Ressource introuvable |
| 409 | Paiement déjà traité |
| 422 | Solde insuffisant / erreur métier |

---

## Authentification

Le JWT est émis par le Wallet externe. Ce microservice le valide uniquement.

```
Authorization: Bearer <JWT_TOKEN>
```

Le JWT doit contenir :
- `accountId` (UUID) — identifiant du compte client
- `roles` — liste des rôles (`USER`, `ADMIN`, `RESPONSIBLE`)

### Rôles et permissions

| Rôle | Permissions |
|---|---|
| `ROLE_USER` | Créer/consulter ses demandes, payer |
| `ROLE_ADMIN` | Gérer le catalogue, responsables, ressources, spécialités, voir toutes les demandes |
| `ROLE_RESPONSIBLE` | Gérer prestations, affecter ressources, créer et suivre les tâches |

---

## API — Endpoints principaux

### Catalogue de services — `/api/v1/services`

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| `GET` | `/api/v1/services` | Public | Liste paginée |
| `GET` | `/api/v1/services/active` | Public | Services actifs |
| `GET` | `/api/v1/services/{id}` | Public | Détail |
| `POST` | `/api/v1/services` | ADMIN | Créer |
| `PUT` | `/api/v1/services/{id}` | ADMIN | Modifier |
| `DELETE` | `/api/v1/services/{id}` | ADMIN | Supprimer |

### Demandes de service — `/api/v1/service-requests`

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| `POST` | `/api/v1/service-requests` | USER | Créer (accountId depuis JWT) |
| `GET` | `/api/v1/service-requests/my` | USER | Mes demandes |
| `GET` | `/api/v1/service-requests/{id}` | USER | Détail (ownership vérifié) |
| `DELETE` | `/api/v1/service-requests/{id}` | USER | Annuler |
| `GET` | `/api/v1/admin/service-requests` | ADMIN | Toutes les demandes |

### Paiement — `/api/v1/service-requests/{id}`

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/{id}/pay` | Initier paiement via Wallet |
| `POST` | `/{id}/retry-payment` | Réessayer après échec |
| `POST` | `/{id}/payment/sync` | Synchroniser après timeout |

### Prestations — `/api/v1/prestations`

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/prestations` | Créer |
| `PUT` | `/{id}/plan` | Planifier (PAID → PLANNED) |
| `PUT` | `/{id}/start` | Démarrer (PLANNED → IN_PROGRESS) |
| `PUT` | `/{id}/complete` | Terminer (IN_PROGRESS → COMPLETED) |
| `PUT` | `/{id}/cancel` | Annuler |
| `POST` | `/{prestationId}/resources/{resourceId}` | Affecter une ressource |
| `DELETE` | `/{prestationId}/resources/{resourceId}` | Retirer une ressource |

### Tâches

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/prestations/{prestationId}/tasks` | Créer une tâche |
| `PATCH` | `/api/v1/tasks/{id}/status` | Mettre à jour le statut |
| `POST` | `/api/v1/tasks/{taskId}/assign/{resourceId}` | Affecter à une ressource |

---

## Paiement

### Flux normal

```
Client → POST /pay (pin) → GestionServiceAPI → WalletClient → Wallet
                                                                  ↓
                                              SUCCESS → PAID (ServiceRequest + Prestation)
                                              FAILED  → FAILED (retry possible)
```

### Idempotence

Chaque tentative génère un `idempotencyKey` unique (UUID). Si l'utilisateur clique plusieurs fois sur "Payer", le Wallet détecte la clé en doublon et refuse le second débit.

### Gestion timeout

Si le réseau coupe après l'envoi au Wallet mais avant la réponse :
1. La tentative reste en statut `PROCESSING`
2. L'utilisateur appelle `POST /{id}/payment/sync`
3. Le système interroge le Wallet via `idempotencyKey`
4. Si `SUCCESS` → met à jour localement sans nouveau débit
5. Si `FAILED` → autorise un retry

### Retry

```
POST /api/v1/service-requests/{id}/retry-payment
{
  "pin": "1234"
}
```

Règles :
- La dernière tentative doit être `FAILED`
- Un nouveau `idempotencyKey` est généré
- `attemptNumber` est incrémenté
- L'historique des tentatives est conservé

---

## Communication Wallet

```java
@FeignClient(name = "wallet-service", url = "${wallet-service.url}")
public interface WalletClient {
    WalletPaymentResponse pay(WalletPaymentRequest request);
    WalletPaymentStatusResponse getPaymentStatus(UUID idempotencyKey);
}
```

**Sécurité :**
- Le PIN est transmis au Wallet et jamais stocké
- Le PIN n'est jamais loggé (logger Feign configuré en `BASIC`)
- L'`accountId` est toujours vérifié contre le JWT avant paiement

---

## Tests

```bash
# Tests unitaires uniquement
./mvnw test -Dtest="*ServiceTest"

# Tous les tests (unitaires + intégration Testcontainers)
./mvnw verify
```

Les tests d'intégration démarrent automatiquement un conteneur PostgreSQL via Testcontainers.

---

## Actuator

```
GET /actuator/health
GET /actuator/info
```

---

## Structure du projet

```
src/main/java/com/example/gestionservice/
├── config/          # OpenApiConfig, FeignConfig
├── controller/      # REST controllers
├── client/          # WalletClient (OpenFeign) + DTOs inter-services
├── dto/
│   ├── request/     # DTOs entrants
│   └── response/    # DTOs sortants
├── entity/          # Entités JPA
├── enums/           # Enums domaine
├── exception/       # Exceptions + GlobalExceptionHandler
├── mapper/          # Mappers MapStruct
├── repository/      # Spring Data JPA
├── security/        # JwtService, JwtAuthenticationFilter, SecurityConfig
└── service/
    └── impl/        # Implémentations des services
```
