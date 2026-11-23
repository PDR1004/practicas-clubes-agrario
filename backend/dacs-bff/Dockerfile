FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

# Confia en el certificado local de clubes.local para las llamadas HTTPS internas
# (por ejemplo la validacion del issuer de Keycloak), sin desactivar la validacion TLS.
COPY --from=certs cert.pem /tmp/clubes-local.pem
RUN keytool -importcert -noprompt -alias clubes-local \
    -file /tmp/clubes-local.pem \
    -cacerts -storepass changeit \
 && rm /tmp/clubes-local.pem

WORKDIR /app
COPY --from=build /app/target/dacs-bff-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9001
ENTRYPOINT ["java", "-jar", "app.jar"]
