# Similar Products API

A Java 21 and Spring Boot service for returning details of products similar to a given product. The repository contains the technical-test contracts, mock services, load-test infrastructure, and the initial application scaffold.

## Project status

- The application scaffold listens on port `5000` by default; override it with `SERVER_PORT`.
- The agreed endpoint is `GET /product/{productId}/similar`.
- The mock APIs listen on port `3001`.
- The endpoint returns similar product details in ID order, an empty array when no similar IDs exist, and applies the documented partial-failure policy.
- Product detail lookups run concurrently with a per-instance limit and a bounded queue.

## API contract

### `GET /product/{productId}/similar`

Returns a JSON array of product details. The service first requests similar product IDs, then obtains the detail for each ID. The response must preserve the order returned by the similar-ID service.

Each item has this shape:

```json
{
  "id": "2",
  "name": "Dress",
  "price": 19.99,
  "availability": true
}
```

The response schema and declared `404` response are defined in [similarProducts.yaml](./similarProducts.yaml). The existing dependencies are documented in [existingApis.yaml](./existingApis.yaml):

| Purpose | Method and path | Mock base URL |
| --- | --- | --- |
| Get similar product IDs | `GET /product/{productId}/similarids` | `http://localhost:3001` |
| Get one product detail | `GET /product/{productId}` | `http://localhost:3001` |

For example, the mock returns IDs `[2,3,4]` for product `1`; the aggregate response should contain those three details in that order. Product details with `availability: false` are still valid results.

The API follows these related-product failure rules:

- Return `404` when the similar-ID service reports that the requested product does not exist.
- Omit related products whose detail lookup returns `404`.
- Omit isolated detail `5xx`, transport, and timeout failures if at least one detail succeeds.
- Return `[]` if all detail lookups return `404`.
- Return `502 Bad Gateway` if no detail succeeds and at least one lookup fails technically.
- Return `503 Service Unavailable` if lookup capacity is exhausted and no detail succeeds; keep successful details when other lookups are rejected.

The supplied load scenarios exercise dependency failures and delays. Executor saturation behavior is covered by unit tests.

## Technology and architecture

The application is configured with Java 21, Spring Boot `4.1.2`, Spring MVC, and Spring `RestClient`. The recommended design is a small hexagonal application:

- Runtime starters: `spring-boot-starter-webmvc` and `spring-boot-starter-restclient`.
- Test dependencies: `spring-boot-starter-test`, the focused Spring MVC and `RestClient` test starters, and OkHttp MockWebServer 3 for HTTP adapter tests.
- Maven Wrapper `3.3.4` pins Apache Maven `3.10.0`; dependency versions are managed by the Spring Boot parent where available.

- An HTTP controller exposes the public endpoint.
- An application use case coordinates the similar-ID and product-detail lookups.
- Outbound ports describe those two dependencies.
- HTTP adapters call the mock APIs; their base URL and timeout settings should be configurable.

Keep the orchestration independent of Spring and HTTP DTOs. Fetch product details concurrently with a bounded limit, then restore the original ID order before returning the result. The shared executor allows up to 100 active product lookups per instance and queues up to 1000 pending lookups by default. Do not add caching, retries, or a resilience framework without a clear requirement and tests for the behavior.

## Build and run the application

Prerequisites: Java 21 and network access the first time the Maven Wrapper downloads Maven `3.10.0` and project dependencies. A global Maven installation is not required.

```bash
./mvnw --batch-mode verify
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

The build targets Java 21 and uses the Maven Wrapper checked into the repository. The application starts on port `5000`; set `SERVER_PORT` to override it. Feature tests are added alongside each TDD implementation pull request.

### Configuration

| Setting | Environment variable | Default |
| --- | --- | --- |
| Application port | `SERVER_PORT` | `5000` |
| Product API base URL | `PRODUCT_API_BASE_URL` | `http://localhost:3001` |
| Product API connection timeout | `PRODUCT_API_CONNECT_TIMEOUT` | `500ms` |
| Product API read timeout | `PRODUCT_API_READ_TIMEOUT` | `2s` |
| Maximum active product detail lookups per instance | `PRODUCT_API_MAX_CONCURRENT_LOOKUPS` | `100` |
| Pending product detail lookup queue capacity | `PRODUCT_API_LOOKUP_QUEUE_CAPACITY` | `1000` |

## Run the supplied mock services and load test

Prerequisite: Docker with Docker Compose. Start the mocks and test reporting services:

```bash
docker-compose up -d simulado influxdb grafana
```

Check that the similar-ID mock responds:

```bash
curl http://localhost:3001/product/1/similarids
```

After the similar-products endpoint has been implemented and the application is running on port `5000`, run the provided k6 test:

```bash
docker-compose run --rm k6 run scripts/test.js
```

The Grafana dashboard is available at <http://localhost:3000/d/Le2Ku9NMk/k6-performance-test>.

The k6 script sends requests to the app through `host.docker.internal:5000`. It defines five scenarios—normal, not found, error, slow, and very slow—with 200 virtual users for 10 seconds per scenario. The mocks include individual detail responses delayed by 100 ms, 1 second, 5 seconds, and 50 seconds. The script measures load and response metrics; it does not assert response bodies or define pass/fail thresholds. Functional contract tests belong in the Maven test suite.

## Evaluation criteria

- Code clarity and maintainability
- Performance under concurrent requests and slow dependencies
- Resilience when a dependency returns an error or responds slowly

## Repository contents

| Path | Purpose |
| --- | --- |
| `pom.xml` | Java 21 and Spring Boot build configuration |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Pinned Maven Wrapper |
| `src/main/java/` | Spring Boot application entry point |
| `src/main/resources/application.properties` | Application and upstream HTTP client configuration |
| `.github/workflows/ci.yml` | Build verification on pushes and pull requests |
| `similarProducts.yaml` | Contract for the API to implement |
| `existingApis.yaml` | Contracts for the mock APIs |
| `shared/simulado/mocks.json` | Mock responses used by Simulado |
| `shared/k6/test.js` | Load-test scenarios |
| `docker-compose.yaml` | Mock, load-test, and reporting infrastructure |
