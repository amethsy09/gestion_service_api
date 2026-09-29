# Gestion Service

Le workspace contient le backend Spring Boot et le frontend Angular dans des dossiers distincts.

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

Le profil de développement utilise `http://localhost:8082` par défaut.

## Frontend

```bash
cd frontend
npm install
npm start
```

Angular est servi sur `http://localhost:4200` et appelle le backend à `http://localhost:8082/api/v1` par défaut. Le backend doit être démarré séparément.
