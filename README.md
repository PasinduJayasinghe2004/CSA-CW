# Smart Campus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey 2.41)** and **Apache Tomcat** for managing university campus rooms and IoT sensors.
Developed as part of the **5COSC022W Client-Server Architectures** module at the University of Westminster.

---


## API Design Overview

The Smart Campus API follows RESTful architectural principles and provides a versioned entry point at `/SmartCampusAPI/api/v1`. It manages three core resources:

| Resource | Description |
|---|---|
| **Room** | Physical campus rooms with id, name, capacity, and linked sensor IDs |
| **Sensor** | IoT devices (Temperature, CO2, Occupancy) deployed within rooms |
| **SensorReading** | Historical time-series readings captured by each sensor |

The API mirrors the physical campus layout as a logical resource hierarchy:

```
/api/v1
├── /rooms
│   ├── GET              — List all rooms
│   ├── POST             — Create a room
│   ├── GET /{roomId}    — Get a specific room
│   └── DELETE /{roomId} — Delete a room (blocked if sensors are assigned)
└── /sensors
    ├── GET              — List all sensors (supports ?type= filter)
    ├── POST             — Register a sensor (validates roomId exists)
    ├── GET /{sensorId}    — Get a specific sensor
    ├── PUT /{sensorId}    — Update a sensor
    ├── DELETE /{sensorId} — Delete a sensor
    └── /{sensorId}/readings   ← Sub-resource locator
        ├── GET              — List all readings for this sensor
        ├── POST             — Add a reading (blocked if sensor is MAINTENANCE)
        └── GET /{readingId} — Get a specific reading
```

All data is stored in-memory using `ConcurrentHashMap` via the `DataStore` class. No database is used. Three rooms and three sensors are pre-loaded as seed data on startup.

---

## Project Structure

```
SmartCampusAPI/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── DataStore.java                           # Static ConcurrentHashMap store + seed data
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java               # GET /api/v1 — metadata + HATEOAS links
    │   ├── RoomResource.java                    # /api/v1/rooms
    │   ├── SensorResource.java                  # /api/v1/sensors + sub-resource locator
    │   └── SensorReadingResource.java           # /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java           # Thrown when deleting a room with sensors
    │   ├── LinkedResourceNotFoundException.java  # Thrown when roomId does not exist
    │   └── SensorUnavailableException.java      # Thrown when sensor is in MAINTENANCE
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java     # → HTTP 409 Conflict
    │   ├── LinkedResourceNotFoundMapper.java    # → HTTP 422 Unprocessable Entity
    │   ├── SensorUnavailableMapper.java         # → HTTP 403 Forbidden
    │   └── GlobalExceptionMapper.java           # → HTTP 500 catch-all safety net
    └── filter/
        └── LoggingFilter.java                   # Logs all requests and responses
```

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 11 |
| REST Framework | JAX-RS via Jersey 2.41 |
| JSON Serialisation | Jackson (`jersey-media-json-jackson`) |
| Build Tool | Apache Maven 3 |
| Server | Apache Tomcat 10 (WAR deployment) |
| Data Storage | `ConcurrentHashMap` (in-memory, no database) |

---

## Building and Running the Server

### Prerequisites

- Java 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 10.x — [Download here](https://tomcat.apache.org/download-10.cgi)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 — Build the WAR File

```bash
mvn clean package
```

This produces `target/SmartCampusAPI.war`.

### Step 3 — Deploy to Tomcat

```bash
cp target/SmartCampusAPI.war /path/to/tomcat/webapps/
```

### Step 4 — Start Tomcat

```bash
# Linux / macOS
/path/to/tomcat/bin/startup.sh

# Windows
/path/to/tomcat/bin/startup.bat
```

### Step 5 — Verify the Server is Running

```bash
curl http://localhost:8080/SmartCampusAPI/api/v1
```

You should receive a JSON discovery response. The base URL for all endpoints is:

```
http://localhost:8080/SmartCampusAPI/api/v1
```

> The server pre-loads seed data on startup: rooms `LIB-301`, `LAB-102`, `HALL-01` and sensors `TEMP-001`, `CO2-001`, `OCC-001` (status: MAINTENANCE).

---



## Sample curl Commands

### 1. Get API Discovery Info

```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1 \
  -H "Accept: application/json"
```



### 2. Create a New Room

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "SCI-201", "name": "Science Lab B", "capacity": 25}'
```



### 3. Register a New Sensor (with roomId validation)

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-002", "type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "LIB-301"}'
```



### 4. Filter Sensors by Type

```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```



### 5. Add a Sensor Reading (updates currentValue on parent sensor)

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.3}'
```



### 6. Attempt to Delete a Room with Active Sensors — 409 Conflict

```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

### 7. Post a Reading to a MAINTENANCE Sensor — 403 Forbidden

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 15.0}'
```

### 8. Register a Sensor with a Non-Existent roomId — 422 Unprocessable Entity

```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Temperature", "status": "ACTIVE", "currentValue": 0.0, "roomId": "FAKE-999"}'
```

## Error Handling Strategy

All error responses follow a consistent JSON structure — no raw stack traces are ever exposed to clients:

```json
{
  "status":  <http_code>,
  "error":   "<ERROR_CODE>",
  "message": "<human-readable description>",
  "hint":    "<suggested corrective action>"
}
```

## Conceptual Report — Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle and In-Memory Data Management

> **Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this impacts the management of in-memory data structures.

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** — this is the per-request lifecycle. Jersey constructs the resource object, invokes the matched method, and then discards the instance once the response is sent. This means any instance fields declared inside a resource class are destroyed at the end of each request.

This has a critical implication for in-memory data management: if the `rooms` or `sensors` maps were declared as instance fields on `RoomResource` or `SensorResource`, all stored data would be lost between requests, making the API completely non-functional.

To solve this, all data in this project is held in the `DataStore` class as **static fields** — specifically `ConcurrentHashMap` instances. Because static fields belong to the class itself (loaded once by the JVM classloader), they survive across the entire application lifecycle regardless of how many resource instances are created. `ConcurrentHashMap` is chosen over a plain `HashMap` to ensure **thread safety**: under concurrent HTTP requests, multiple Jersey threads may access and modify the maps simultaneously, and `ConcurrentHashMap` prevents data corruption through its internal segment-level locking without requiring explicit `synchronized` blocks on every operation.

---

### Part 1.2 — HATEOAS and Hypermedia-Driven Design

> **Question:** Why is the provision of "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design? How does this benefit client developers compared to static documentation?

HATEOAS — Hypermedia as the Engine of Application State — is the highest maturity level of the Richardson REST Maturity Model. The principle is that API responses embed links describing what the client can do next, rather than requiring the client to construct or hard-code URLs in advance.

In the `DiscoveryResource`, the JSON response at `GET /api/v1` includes a `_links` object containing the URIs for `rooms` and `sensors`. A client application therefore only needs to know the single root URL of the API; it can navigate the entire surface by following embedded links.

Compared to static documentation, HATEOAS offers several advantages. It **reduces coupling**: if a URL structure changes server-side, clients that follow links rather than hard-coding paths adapt automatically without a code change. It improves **discoverability** — developers can explore the API programmatically, similar to following hyperlinks on a website. It provides **always-current navigation**: static documentation can go out of date, but HATEOAS links come directly from the running server and are always accurate. Finally, context-sensitive links reduce the client developer's need to mentally reconstruct URL patterns from documentation.

---

### Part 2.1 — Full Objects vs. IDs in List Responses

> **Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

`RoomResource` returns the full list of `Room` objects from `GET /api/v1/rooms`. This is the appropriate default, but the trade-off is a meaningful architectural consideration.

**Returning only IDs** minimises response payload size. A list of strings (e.g., `["LIB-301", "LAB-102"]`) is far smaller than a list of full objects. This benefits clients on constrained networks and reduces server-side serialisation work. However, if the client needs detail about any room, it must issue a separate `GET /api/v1/rooms/{roomId}` for each entry — the N+1 request problem — which can be far more expensive in total latency than one slightly larger response.

**Returning full objects**, as implemented here, means the client receives all necessary information in a single round-trip — ideal for admin dashboards or bulk-processing clients. The trade-off is higher per-request bandwidth. A production API would typically offer pagination (`?page=1&size=20`) or sparse fieldsets (`?fields=id,name`), allowing clients to choose the approach suited to their use case.

---

### Part 2.2 — Idempotency of DELETE

> **Question:** Is the DELETE operation idempotent in your implementation? Justify by describing what happens if a client sends the same DELETE request multiple times.

Yes, DELETE is **idempotent** in this implementation, consistent with RFC 7231. Idempotency means that making the same request N times produces the same server state as making it once.

In `RoomResource.deleteRoom()`, the **first** `DELETE /api/v1/rooms/{roomId}` call verifies the room exists and has no sensors, removes it from `DataStore.rooms`, and returns `HTTP 204 No Content`. If the **same request is sent again**, `DataStore.rooms.get(roomId)` returns `null`, and the method throws `NotFoundException`, resulting in `HTTP 404 Not Found`.

The server state is identical after the first call and any subsequent call: the room is absent. The HTTP status code changes (204 → 404), but this does not violate idempotency — the definition concerns server-side state, not response codes. No double-deletions, no data corruption, and no unintended side effects occur on repeat calls. This is the standard expected behaviour for idempotent DELETE in REST APIs.

---

### Part 3.1 — Effect of @Consumes and Content-Type Mismatches

> **Question:** We explicitly use `@Consumes(MediaType.APPLICATION_JSON)` on the POST method. What are the technical consequences if a client sends data in a different format, such as `text/plain` or `application/xml`?

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on `POST /api/v1/sensors` instructs the JAX-RS runtime to only accept requests whose `Content-Type` header is `application/json`. This constraint is enforced automatically by Jersey **before** the method body executes.

If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey performs content negotiation, finds no resource method that accepts the declared media type, and immediately returns **HTTP 415 Unsupported Media Type** — without invoking the method, deserialising any body content, or executing any business logic.

This is an important correctness and security boundary. It prevents malformed or unexpected data formats from reaching the Jackson deserialiser, which could cause obscure parsing exceptions. It also enforces the API contract at the framework level, providing a clear and immediate signal to the client rather than a confusing downstream error.

---

### Part 3.2 — @QueryParam vs. Path Segment for Filtering

> **Question:** Contrast the `@QueryParam` approach with embedding the filter in the URL path (e.g., `/sensors/type/CO2`). Why is the query parameter approach generally superior for filtering?

The `@QueryParam` approach in `GET /api/v1/sensors?type=CO2` is the correct RESTful design for collection filtering for several reasons.

**Optionality**: Query parameters are optional by definition. Omitting `?type` returns all sensors. If the type were a path segment (`/sensors/type/CO2`), a separate mapping would be needed for the unfiltered case, creating duplication.

**Semantic correctness**: URL path segments represent **resource identity** — they answer "what is this resource?". Filtering criteria are not resource identifiers. `/sensors/CO2` implies CO2 is a sub-resource, which is incorrect. `/sensors?type=CO2` correctly conveys "the sensors collection, narrowed by type=CO2".

**Composability**: Multiple filters combine naturally with no URL design complexity: `/sensors?type=CO2&status=ACTIVE`. Replicating this with path segments quickly becomes ambiguous and hard to route.

**Standards alignment**: HTTP caches and CDNs understand the query string as part of a collection view identifier, making query-param-based filtering behave predictably with standard HTTP infrastructure.

---

### Part 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

> **Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs?

The Sub-Resource Locator is implemented in `SensorResource` via `getReadingsResource()`, which carries `@Path("/{sensorId}/readings")` but **no HTTP method annotation**. When Jersey matches a request such as `POST /api/v1/sensors/TEMP-001/readings`, it calls this locator to obtain a `SensorReadingResource` instance, then dispatches the HTTP method to that object.

The primary benefit is **separation of concerns**. `SensorReadingResource` is solely responsible for reading management: history retrieval, MAINTENANCE status validation, UUID/timestamp assignment, and the side-effect of updating `currentValue` on the parent sensor. If all of this lived inside `SensorResource` alongside sensor CRUD, type filtering, and room-validation logic, the class would become a monolith — difficult to read, test, and maintain.

In large APIs with deep hierarchies (e.g., `/buildings/{id}/floors/{id}/rooms/{id}/sensors`), the sub-resource pattern allows each level to be handled by a dedicated class with a single responsibility. Each class is **independently testable**. Changes to reading logic require no changes to sensor-level logic, reducing regression risk. New developers can understand one resource class without needing to comprehend the full codebase. This directly applies the **Single Responsibility Principle** from SOLID software design.

---

### Part 5.2 — HTTP 422 vs. HTTP 404 for Missing Reference Errors

> **Question:** Why is HTTP 422 often considered more semantically accurate than 404 when the issue is a missing reference inside a valid JSON payload?

When a client sends `POST /api/v1/sensors` with `"roomId": "FAKE-999"`, the HTTP request is entirely valid: the JSON parses correctly, the `Content-Type` is correct, and the `/sensors` endpoint exists and is functioning. The problem is that the **content of the payload** references an entity that does not exist.

**HTTP 404 Not Found** is semantically reserved for situations where the requested resource — identified by the **URL** — cannot be located. Returning 404 for a POST to `/api/v1/sensors` would be misleading, because the sensors endpoint does exist and is working. The issue is not with the URL but with the payload semantics.

**HTTP 422 Unprocessable Entity** (defined in RFC 4918) was specifically designed for this scenario: the server understands the request format but cannot process the semantic instructions it contains. The payload is syntactically valid but semantically incorrect. This allows clients to precisely distinguish between "that endpoint does not exist" (404), "your JSON is malformed" (400), and "your JSON is valid but references something that does not exist" (422) — enabling accurate, targeted error handling.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

> **Question:** From a cybersecurity standpoint, explain the risks of exposing internal Java stack traces to external API consumers. What specific information could an attacker gather?

Exposing raw Java stack traces is a significant security vulnerability, categorised under OWASP A05 (Security Misconfiguration). The `GlobalExceptionMapper` in this project prevents this by catching all `Throwable` exceptions, logging the full trace **server-side only**, and returning a generic JSON message to the client.

A stack trace reveals multiple categories of sensitive information. It discloses the **exact technology stack**, including library names and version numbers (e.g., Jersey 2.41, Jackson 2.15.2). An attacker can immediately search published CVEs for those specific versions and craft targeted exploits. It also exposes **internal package and class names** (`com.smartcampus.resource.SensorResource`), revealing the application's architecture. In database-related errors it may leak **SQL query fragments**, table names, and schema structures. **File system paths** of compiled class files can indicate server directory layouts. `NullPointerException` traces reveal which fields are unexpectedly null, exposing data model weaknesses that could be exploited through crafted inputs to trigger denial-of-service or bypass validation logic.

By returning only a safe `HTTP 500` with a generic message and logging full detail internally, the API ensures the implementation is fully opaque to external actors, significantly reducing the attack surface.

---

### Part 5.5 — JAX-RS Filters vs. Manual Logging

> **Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

A cross-cutting concern is a behaviour that applies uniformly across many different modules. Logging every HTTP request and response is the classic example. The `LoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`, registered globally via `@Provider`, meaning it intercepts **every single request and response automatically**.

**DRY principle**: The logging logic exists in exactly one place. If the log format changes — for example, adding a correlation ID or timing information — only the filter needs updating. Manual logging would require editing every resource method individually, which is time-consuming and error-prone.

**Exhaustive by default**: Any newly added endpoint is automatically covered. Manual logging requires discipline; a developer can easily forget to add a log statement in a new method, creating invisible gaps in observability.

**Access to response context**: The `ContainerResponseFilter` interface provides access to the final HTTP status code, which is unavailable inside a resource method. This cannot be achieved with manual logging at the method level without awkward workarounds.

**Clean resource methods**: Methods stay focused solely on business logic with no logging boilerplate, improving readability and maintainability across the entire codebase.
