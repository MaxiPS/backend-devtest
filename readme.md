# Similar Products API

A Spring Boot service for returning product details for the products similar to a given product. This repository currently contains the technical-test contracts, mock services, and load-test infrastructure; the application source and build descriptor have not been added yet.

## Project status

- The required application is expected to listen on port `5000`.
- The agreed endpoint is `GET /product/{productId}/similar`.
- The mock APIs listen on port `3001`.
- The Spring Boot application cannot be built or started from this checkout yet because it has no application source, Maven/Gradle build file, or build wrapper. The implementation workflow is described in [workflow.md](./workflow.md).

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

The contract does not define how to handle an individual related-product lookup that returns `404`, `5xx`, or times out. Choose and document that policy before implementing the use case. The supplied load scenarios exercise these cases.

## Technology and architecture

The requested implementation is a Spring Boot application. The recommended design is a small hexagonal application:

- An HTTP controller exposes the public endpoint.
- An application use case coordinates the similar-ID and product-detail lookups.
- Outbound ports describe those two dependencies.
- HTTP adapters call the mock APIs; their base URL and timeout settings should be configurable.

Keep the orchestration independent of Spring and HTTP DTOs. Fetch product details concurrently with a bounded limit, then restore the original ID order before returning the result. Do not add caching, retries, or a resilience framework without a clear requirement and tests for the behavior.

## Build and run the application

This starter checkout does not include a buildable application yet. The implementation workflow calls for a Maven Wrapper-based Spring Boot project; once its build files are added, the documented commands should include:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd clean verify` and `.\mvnw.cmd spring-boot:run`. The app should expose port `5000`. These commands are planned and are not runnable until the application scaffold is committed.

## Run the supplied mock services and load test

Prerequisite: Docker with Docker Compose. Start the mocks and test reporting services:

```bash
docker-compose up -d simulado influxdb grafana
```

Check that the similar-ID mock responds:

```bash
curl http://localhost:3001/product/1/similarids
```

After the application is running on port `5000`, run the provided k6 test:

```bash
docker-compose run --rm k6 run scripts/test.js
```

The Grafana dashboard is available at <http://localhost:3000/d/Le2Ku9NMk/k6-performance-test>.

The k6 script sends requests to the app through `host.docker.internal:5000`. It defines five scenarios—normal, not found, error, slow, and very slow—with 200 virtual users for 10 seconds per scenario. The mocks include individual detail responses delayed by 100 ms, 1 second, 5 seconds, and 50 seconds. The script currently measures load and response metrics; it does not assert response bodies or define pass/fail thresholds. Add functional contract tests as part of the application implementation.

## Evaluation criteria

- Code clarity and maintainability
- Performance under concurrent requests and slow dependencies
- Resilience when a dependency returns an error or responds slowly

## Repository contents

| Path | Purpose |
| --- | --- |
| `similarProducts.yaml` | Contract for the API to implement |
| `existingApis.yaml` | Contracts for the mock APIs |
| `shared/simulado/mocks.json` | Mock responses used by Simulado |
| `shared/k6/test.js` | Load-test scenarios |
| `docker-compose.yaml` | Mock, load-test, and reporting infrastructure |
| `workflow.md` | TDD-oriented implementation workflow |
