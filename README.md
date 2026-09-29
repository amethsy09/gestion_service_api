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
- L'initiation des paiements via le Wallet externe (banque1_api)
- La gestion des paiements échoués, des retries et de la compensation

**Ce microservice ne gère PAS :** les utilisateurs, l'authentification, les comptes bancaires, le solde, le PIN.
Ces responsabilités appartiennent au **Wallet externe (banque1_api)** et à **auth_api**.

---

## Architecture

```
                    ┌──────────────────┐
                    │    auth_api      │
                    │  OTP / Login / JWT│
                    └────────┬─────────┘
                             │ JWT (sub = telephone)
                             │
                             ▼
                    ┌──────────────────┐
                    │ Angular frontend │
                    └────────┬─────────┘
                             │ Bearer JWT
                             ▼
                    ┌──────────────────────┐
                    │  gestion-service     │
                    │  services / demandes │
                    │  prestations / tâches│
                    │  paiements           │
                    └────────┬─────────────┘
                             │ OpenFeign (X-Internal-Api-Key)
                             ▼
                    ┌──────────────────────┐
                    │  banque1_api / Wallet │
                    │  comptes / solde / PIN│
                    │  transactions         │
                    └──────────────────────┘
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
WALLET_INTERNAL_API_KEY: votre_cle_interne_pour_le_wallet
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
| V10 | `gestion_account` | Identité locale (telephone → UUID interne + rôles) |

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

Pour adapter l'URL de production, définir la variable d'environnement :
```bash
CORS_ALLOWED_ORIGINS=https://votre-frontend.com,https://admin.votre-frontend.com
```

### Authentification

Le frontend doit :

1. Obtenir un **JWT** d'auth_api (pas de `/login` ici).
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

---

## Authentification et identité

### JWT (émis par auth_api — source de vérité)

```json
{
  "sub": "771234567",
  "iat": "...",
  "exp": "..."
}
```

- `sub` contient le **téléphone** de l'utilisateur.
- Le JWT ne contient **pas** `accountId` ni `roles`.
- auth_api n'est pas modifié.
- Angular ne modifie pas le JWT.
- gestion-service ne génère jamais de JWT.

### Résolution d'identité (gestion-service)

```
JWT (sub = telephone)
    ↓
JwtService.extractTelephone(claims)  →  telephone
    ↓
AccountSecurityResolver.resolve(telephone)
    ↓
gestion_account  (requête par telephone)
    ↓
UUID interne + rôles (ADMIN / RESPONSIBLE / USER)
    ↓
JwtAuthenticationPrincipal → SecurityContext
```

#### Table `gestion_account` (V10)

| Colonne | Type | Description |
|---|---|---|
| `id` | UUID | Identifiant interne (PK) |
| `telephone` | VARCHAR(20) | Téléphone (UNIQUE) — extrait du JWT `sub` |
| `wallet_account_id` | BIGINT | Identifiant du compte chez le Wallet (type `long`) |
| `role` | VARCHAR(30) | Rôle métier local (ROLE_USER / ROLE_ADMIN / ROLE_RESPONSIBLE) |
| `active` | BOOLEAN | Compte activé |
| `created_at` | TIMESTAMP | Audit |
| `updated_at` | TIMESTAMP | Audit |

#### Première authentification

Lorsqu'un JWT valide arrive avec un `sub` (téléphone) inconnu de `gestion_account`, un compte est créé automatiquement avec :
- `role = ROLE_USER`
- `active = true`

Il est ensuite possible de promouvoir un utilisateur via une procédure contrôlée (USER → ADMIN / USER → RESPONSIBLE).

#### Rôles Spring Security

Les authorities sont créées via :

```java
new SimpleGrantedAuthority(role.name())  // ex: "ROLE_ADMIN"
```

Utiliser dans les contrôleurs :

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'RESPONSIBLE')")
```

---

## Ownership des demandes

Chaque `ServiceRequest` est associée à l'UUID interne de `gestion_account`. L'ownership est toujours vérifié depuis le `SecurityContext` :

- Un **USER** ne voit que ses propres demandes.
- Un **ADMIN** peut consulter toutes les demandes.
- L'`accountId` provient exclusivement du principal JWT, jamais du body de la requête.

---

## Paiement

### Flux normal

```
Angular → POST /api/v1/service-requests/{id}/pay
  {
    "pin": "1234"
  }
  Authorization: Bearer <JWT>
        ↓
gestion-service → WalletClient (OpenFeign + X-Internal-Api-Key)
        ↓
banque1_api /api/v1/internal/payments/service
        ↓
Débit du compte bancaire (identifié par telephone)
```

### Idempotence

Chaque tentative génère un `idempotencyKey` unique (UUID). Le Wallet détecte les clés en double et refuse le second débit.

### Gestion timeout

Si le réseau coupe après l'envoi au Wallet mais avant la réponse :
1. La tentative reste en statut `PROCESSING`
2. L'utilisateur appelle `POST /api/v1/service-requests/{id}/payment/sync`
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

### Communication Wallet

```java
@FeignClient(name = "wallet-service", url = "${wallet-service.url}", configuration = FeignConfig.class)
public interface WalletClient {
    WalletPaymentResponse pay(WalletPaymentRequest request);
    WalletPaymentStatusResponse getPaymentStatus(UUID idempotencyKey);
}
```

**Sécurité :**
- Le PIN est transmis au Wallet et jamais stocké
- Le PIN n'est jamais loggé (logger Feign configuré en `BASIC`)
- L'`X-Internal-Api-Key` est envoyé via un interceptor Feign
- Le téléphone est envoyé au Wallet pour identifier le compte (pas d'UUID)
- Le Wallet reste propriétaire de : comptes, solde, PIN, transactions

---

## Matrice des permissions

| Endpoint | USER | RESPONSIBLE | ADMIN |
|---|---|---|---|
| GET /services | ✅ | ✅ | ✅ |
| GET /services/active | ✅ | ✅ | ✅ |
| GET /services/{id} | ✅ | ✅ | ✅ |
| POST /services | ❌ | ❌ | ✅ |
| PUT /services/{id} | ❌ | ❌ | ✅ |
| DELETE /services/{id} | ❌ | ❌ | ✅ |
| GET /specialties | ✅ | ✅ | ✅ |
| POST /specialties | ❌ | ❌ | ✅ |
| PUT/DELETE /specialties | ❌ | ❌ | ✅ |
| POST /service-requests | ✅ | ✅ | ✅ |
| GET /service-requests/my | ✅ | ✅ | ✅ |
| GET /service-requests/{id} | ✅ (ownership) | ✅ (ownership) | ✅ |
| DELETE /service-requests/{id} | ✅ (ownership) | ✅ (ownership) | ✅ |
| GET /admin/service-requests | ❌ | ❌ | ✅ |
| POST /prestations | ❌ | ✅ | ✅ |
| PUT /prestations/{id}/plan | ❌ | ✅ | ✅ |
| PUT /prestations/{id}/start | ❌ | ✅ | ✅ |
| PUT /prestations/{id}/complete | ❌ | ✅ | ✅ |
| PUT /prestations/{id}/cancel | ❌ | ✅ | ✅ |
| POST/DELETE /prestations/{id}/resources | ❌ | ✅ | ✅ |
| POST /resources | ❌ | ✅ | ✅ |
| PUT /resources/{id} | ❌ | ✅ | ✅ |
| DELETE /resources/{id} | ❌ | ❌ | ✅ |
| POST /responsibles | ❌ | ❌ | ✅ |
| PUT/DELETE /responsibles | ❌ | ❌ | ✅ |
| POST /prestations/{id}/tasks | ❌ | ✅ | ✅ |
| PATCH /tasks/{id}/status | ❌ | ✅ | ✅ |
| POST /tasks/{id}/assign/{id} | ❌ | ✅ | ✅ |

---

## Variables d'environnement

| Variable | Description | Exemple |
|---|---|---|
| `JWT_SECRET` | Clé secrète partagée avec auth_api (min 32 caractères) | `my_super_secret_key_32_chars` |
| `JWT_EXPIRATION` | Durée de validité du JWT (ms) | `86400000` (24h) |
| `WALLET_SERVICE_URL` | URL du Wallet (banque1_api) | `http://localhost:8081` |
| `WALLET_INTERNAL_API_KEY` | Clé d'API interne pour service-to-service | `change_me_long_random_string` |
| `CORS_ALLOWED_ORIGINS` | Origines CORS autorisées (virgules) | `https://app.example.com` |
| `DATABASE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/gestion_service_db` |
| `DATABASE_USERNAME` | Nom d'utilisateur PostgreSQL | `postgres` |
| `DATABASE_PASSWORD` | Mot de passe PostgreSQL | `********` |

### Sécurité des secrets

- `JWT_SECRET` — ne doit jamais être commité ni loggé
- `DB_PASSWORD` — ne doit jamais être commité
- `WALLET_INTERNAL_API_KEY` — ne doit jamais être commité ni loggé
- Aucun JWT, PIN ou secret n'apparaît jamais dans les logs

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
├── client/          # WalletClient (OpenFeign) + DTOs inter-services
├── controller/      # REST controllers
├── dto/
│   ├── request/     # DTOs entrants
│   └── response/    # DTOs sortants
├── entity/          # Entités JPA (GestionAccount, ServiceRequest, Prestation, etc.)
├── enums/           # Enums domaine (Role, Status, etc.)
├── exception/        # Exceptions + GlobalExceptionHandler
├── mapper/          # Mappers MapStruct
├── repository/      # Spring Data JPA
├── security/        # JwtService, JwtAuthenticationFilter, AccountSecurityResolver,
│                     # JwtAuthenticationPrincipal, SecurityConfig
└── service/
    └── impl/        # Implémentations des services
```
