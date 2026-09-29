# ============================================================
# Dockerfile — Gestion Service API
# Multi-stage build : JDK 21 compile + JRE 21 runtime
# ============================================================

# ===== STAGE 1 : Build =====
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copier les fichiers Maven wrapper + pom.xml en premier (cache layer)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Télécharger les dépendances (layer cachée si pom.xml ne change pas)
RUN ./mvnw dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ===== STAGE 2 : Runtime =====
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copier le JAR compilé depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Changer le propriétaire
RUN chown appuser:appgroup app.jar

USER appuser

# Port exposé
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Lancer l'application avec profil prod
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
