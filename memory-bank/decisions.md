# Decisions

## Spring AI MCP Version

The project uses Spring Boot 4.0.6, so Spring AI `2.0.0-M6` is used through the Spring AI BOM. Spring AI 1.x targets the Spring Boot 3 line and is not the right compatibility baseline for this project.

## Spring AI MCP Annotation Scanner Startup Warning

Spring AI `2.0.0-M6` eagerly creates MCP annotation scanner support beans while registering `serverAnnotatedMethodBeanPostProcessor`, which causes Spring's `BeanPostProcessorChecker` to warn that those scanner beans are not eligible for all post-processors. Keep `McpAnnotationScannerInfrastructureConfiguration` while this dependency behavior remains: it marks only the Spring AI MCP annotation scanner bean definitions as `ROLE_INFRASTRUCTURE`, preserving annotation scanning while suppressing that framework warning.

## Vendor Payload Handling

Met Office forecast, observation, and order metadata payloads are preserved as `JsonNode` instead of being fully typed. This keeps the MCP server resilient to DataHub payload shape changes while still returning a typed local response envelope.

Binary product downloads, such as GRIB and PNG responses, are returned as base64 strings in the same envelope.

Binary file download tools also support opt-in `binaryDebug` mode. In that mode the response keeps the full `binaryBase64` payload null and returns compact diagnostics in `data` so MCP clients and transcripts can verify large binary downloads without carrying the full payload.

Dynamic DataHub path segments that come from order or file metadata must be encoded as single path segments before URI construction. File IDs can contain reserved characters such as `+`, so product clients should use the shared transport helper instead of interpolating raw file IDs into download paths.

## Error Handling

Non-2xx Met Office responses are converted to typed error envelopes containing product, operation, status, endpoint, and a bounded response body preview. Missing API keys are reported as local validation errors before any HTTP request is attempted.

## Authentication

Met Office DataHub API keys are sent as an `apikey` header. Do not move API keys into query parameters.

## Product Client Split

Met Office product-specific endpoint construction, validation, and environment-backed API key ownership are split across four clients: atmospheric, site-specific, observations, and map images. MCP tool adapters are also split by product under `tools/` and should stay thin.

Keep common HTTP transport, `apikey` header application, response parsing, base64 encoding, and error envelope handling in `MetOfficeDataHubClient`. Do not move product key lookup back into the shared transport.

Product clients live in their own packages under `client/`. Product request records live under `model/<product>/`, and each record has its own file. Preserve this layout to make change history traceable and reduce merge conflicts.

Product tool adapters live under `tools/<product>/`. Preserve this layout so MCP annotation changes for one product do not conflict with unrelated product work.

## Javadoc Scope

Javadoc is expected for public APIs, MCP tool entry points, configuration properties, and non-obvious integration behavior. Comments should describe useful contracts and assumptions, not restate names or implementation details.

## Testable Helper Visibility

Use `protected` rather than `private` for non-public helper methods when direct method-level unit tests are needed to maintain 100% coverage. Do not make helpers `public` only for tests; keep them implementation-focused and cover them from matching package tests or focused subclasses.

## Test Strategy

Use parameterized tests for Met Office tool and client behavior. Fast mock HTTP tests should cover every MCP method and representative non-200 responses during normal local runs. Testcontainers MockServer tests provide end-to-end HTTP coverage when Docker is available. Mockito tests cover thin MCP adapter delegation and request record construction.

Unit test classes should map directly to the production class under test and live in the matching package path under `src/test/java`. Use separate integration/e2e test classes for cross-class flows such as containerized HTTP coverage.

Keep invalid request tests for local validation paths such as unsupported Global Spot timesteps and unsupported BPF collection IDs.

Set the docker-java test client API version to `1.44` through `src/test/resources/docker-java.properties` so the Testcontainers client can run against Docker Engine 29+ environments that reject older API defaults.

Configure the MockServer container through its REST API in container tests. Do not add `mockserver-client-java` to the test classpath unless its transitive `com.networknt:json-schema-validator` version is reconciled with Spring AI MCP's validator dependency.
