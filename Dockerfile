# One image for the whole app: the React build is served by Spring Boot from the same origin as /api.

# 1. Frontend: Vite build
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Backend: jar with the frontend in static/. Tests run in CI (they need Postgres), not here.
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
COPY --from=frontend /app/dist/ src/main/resources/static/
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar app.jar

# 3. Runtime: JRE only, non-root
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home aurawealth
COPY --from=backend /app/app.jar app.jar
USER aurawealth
EXPOSE 8080
# The JVM defaults to a quarter of the container's memory for the heap; free hosts give 512 MB, so allow more
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
