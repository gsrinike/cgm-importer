# CGMES Utility Service

Spring Boot service for parsing CGMES RDF, running SHACL validations, and viewing coordinate-bearing resources on a European map.

## Build

```bash
mvn clean package
```

The Maven build produces `target/cgmes-utility-service-0.1.0.jar`.

## Run

```bash
java -jar target/cgmes-utility-service-0.1.0.jar
```

Open:

- Web map: <http://localhost:8080/>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Static OpenAPI YAML: <http://localhost:8080/openapi.yaml>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

## REST API

- `POST /api/cgmes/load` multipart field `files`: load CGMES RDF/XML, Turtle, N-Triples, N3, TriG, or ZIP files.
- `GET /api/cgmes/summary`: return counts for the loaded model.
- `GET /api/cgmes/features`: return GeoJSON features for resources with `latitude`/`longitude`, `lat`/`lon`, or `xPosition`/`yPosition` properties.
- `POST /api/cgmes/shacl` multipart field `shapes`: validate the loaded model with SHACL shapes.

## Observability

The service includes structured application logging and OpenTelemetry Spring Boot instrumentation. Configure OTLP export with the standard OpenTelemetry environment variables, for example:

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 java -jar target/cgmes-utility-service-0.1.0.jar
```
