# Progress

## 2026-05-13

- Added Spring AI MCP WebMVC support.
- Added Met Office configuration properties for four independent API keys and configurable base URLs.
- Added MCP tools for atmospheric model orders/files, site-specific Global Spot, BPF collections/locations/forecasts, observations, and map image orders/files.
- Added shared `RestClient`-based Met Office DataHub client with header authentication, JSON passthrough, base64 binary handling, and typed error envelopes.
- Added tests for property binding, per-product API key headers, query construction, timestep validation, binary base64 encoding, missing-key handling, and `401`, `404`, `429`, and `500` error envelopes.
- Verified with `.\gradlew.bat test`.
- Added repository Javadoc guidance to `AGENTS.md` and recorded the documentation scope in the memory bank.
- Split product-specific Met Office API behavior into atmospheric, site-specific, observations, and map image clients while keeping `MetOfficeTools` as a thin MCP adapter.
- Moved API key ownership into the four product clients, kept `apikey` header application in the shared transport, and verified all product clients send keys as headers rather than query parameters.
- Moved product clients into product-specific `client/` packages and split request/response records into `model/` packages with one record per file. Verified with `.\gradlew.bat test`.
- Split the former combined MCP tool adapter into product-specific tool beans under `tools/`. Verified Spring registers all four tool beans with `.\gradlew.bat test`.
- Added Testcontainers MockServer and Mockito test dependencies. Added parameterized happy-path coverage for every MCP method, parameterized non-200 and invalid-request unhappy paths, containerized end-to-end tests for Docker-enabled environments, and Mockito adapter delegation tests. Local Docker was unavailable, so Testcontainers tests skipped in this session; fast mock and Mockito tests ran successfully with `.\gradlew.bat test`.
- Refactored unit tests so test classes map to their corresponding production classes: shared transport, properties, four product clients, and four product tool adapters. Kept containerized e2e coverage in a separate integration-style test class. Verified with `.\gradlew.bat test`.
- Moved test classes into package paths that mirror their production classes under `src/main/java`. Verified with `.\gradlew.bat test`.
- Enabled `MetOfficeToolsContainerTests` by removing the Docker-unavailable skip guard, added a test-scope docker-java API version override for Docker Engine 29+ compatibility, and switched the container test from the MockServer Java client to MockServer's REST API to avoid test classpath dependency conflicts. Verified with `.\gradlew.bat test`.
- Added entry-point tests for `MetOfficeMcpServerApplication` construction and `SpringApplication.run` delegation. Verified with `.\gradlew.bat test`.
- Added a local repo-root `.env` with Met Office API key environment variable placeholders and ignored it in `.gitignore` so real local keys are not tracked.
- Marked Spring AI MCP annotation scanner beans as infrastructure before BeanPostProcessor registration so startup no longer logs the scanner-related `BeanPostProcessorChecker` warnings. Verified startup with `.\gradlew.bat bootRun --args='--spring.main.web-application-type=none'` and full tests with `.\gradlew.bat test`.
- Updated GitHub CI to use the checked-in Gradle wrapper and Gradle output paths instead of Maven commands, caches, and `target/` artifacts. Verified locally with `.\gradlew.bat test` and `.\gradlew.bat assemble`.
- Updated GitHub CD to build and push a GraalVM native container image through Spring Boot `bootBuildImage` with the Paketo tiny builder. Added a `-PnativeImage` Gradle switch for native buildpack image settings. Verified task wiring with a `bootBuildImage --dry-run` and full tests with `.\gradlew.bat test`.
- Set the CD native image buildpack JVM version to 25 through `-PnativeImageJvmVersion=25`, because Paketo Spring Boot buildpack 5.36.2 does not support Spring Boot 4 native images with a downloaded GraalVM/NIK lower than Java 25.
- Added a user-first `README.md` covering features, configuration, quick start, Docker and Compose usage, MCP tool names, testing, native builds, security notes, project layout, and references.
- Added root `docker-compose.yaml` for running the published GHCR image with environment values loaded from the ignored local `.env` file. The image can be overridden with `MET_OFFICE_MCP_IMAGE`.
- Added a GraalVM runtime hint for Spring AI MCP `DefaultMetaProvider` so native images can instantiate the default MCP annotation metadata provider reflectively at startup. Verified with `.\gradlew.bat test`, `.\gradlew.bat processAot`, a full native `bootBuildImage`, and a native container startup smoke test.
- Added GraalVM reflection hints for `MetOfficeToolResponse` and nested `MetOfficeError` record component accessors so native images can serialize MCP tool responses. Verified with `.\gradlew.bat test`, `.\gradlew.bat processAot`, a full native `bootBuildImage`, and a native MCP `tools/call` smoke test against the local container.
- Disabled OTLP metrics export by default through `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED:false` so local and Compose runs do not warn when no collector is listening on `localhost:4318`; the Compose file sets the same default and documents the opt-in settings.

## Follow-Up Notes

- The observations base URL is configurable because the public Met Office docs page did not expose all concrete paths in static HTML during implementation.
- When changing tool paths or query parameters, update focused tests in `MetOfficeToolsTests`.
