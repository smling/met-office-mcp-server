# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot project built with Gradle. Application code lives under `src/main/java/io/github/smling/met_office_mcp_server`, with `MetOfficeMcpServerApplication.java` as the entry point. Runtime configuration lives in `src/main/resources/application.yaml`. Tests mirror the main package under `src/test/java/io/github/smling/met_office_mcp_server`.

Keep Met Office DataHub client code organized by product under `client/atmospheric`, `client/sitespecific`, `client/observations`, and `client/mapimages`. Shared HTTP transport belongs in `client/MetOfficeDataHubClient.java`.

Keep MCP tool adapters organized by product under `tools/atmospheric`, `tools/sitespecific`, `tools/observations`, and `tools/mapimages`. Each tool adapter should only contain `@McpTool` methods for its product and delegate to the matching product client.

Keep request and response records under `model/`, with product-specific request records in product subpackages such as `model/atmospheric` or `model/mapimages`. Store each record in its own file so change history is easy to trace and parallel changes avoid merge conflicts.

Keep generated output out of source control. The `build/`, `.gradle/`, and IDE metadata directories are local artifacts and should not be edited as source.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper so contributors run the same Gradle version:

```powershell
.\gradlew.bat test
```

Runs the JUnit Platform test suite.

```powershell
.\gradlew.bat build
```

Compiles, tests, and packages the application.

```powershell
.\gradlew.bat bootRun
```

Starts the Spring Boot application locally.

```powershell
.\gradlew.bat nativeCompile
```

Builds a GraalVM native image when the required local toolchain is available.

On Unix-like shells, use `./gradlew` instead of `.\gradlew.bat`.

## Coding Style & Naming Conventions

Follow standard Java conventions with 4-space indentation, one public top-level class per file, and descriptive class names in `PascalCase`. Methods and fields use `camelCase`. Keep packages under `io.github.smling.met_office_mcp_server` unless there is a deliberate module split.

No formatter or lint task is currently configured. Match the surrounding Spring Boot style, keep imports organized, and avoid adding broad abstractions before the application has clear module boundaries.

## Javadoc Guidelines

Create Javadoc for public APIs, MCP tool entry points, configuration properties, and non-obvious domain or integration behavior. Prefer concise comments that explain purpose, contract, inputs, outputs, side effects, and failure behavior where that information is not obvious from names and types.

Do not add Javadoc that merely repeats the method or field name. Keep implementation details out of API Javadoc unless callers need to know them. Update existing Javadoc whenever behavior, parameters, return values, exceptions, configuration, or external API assumptions change.

Use standard Javadoc tags when useful: `@param` for parameters with constraints or units, `@return` for meaningful return contracts, and `@throws` for exceptions that callers can reasonably handle. Keep examples short and only include them when they clarify a public contract.

## Testing Guidelines

Tests use JUnit 5 via Gradle’s `useJUnitPlatform()` and Spring Boot test support. Place tests under `src/test/java` in the same package as the code under test. Name test classes with a `Tests` suffix, for example `MetOfficeMcpServerApplicationTests`, and prefer focused method names such as `contextLoads` or `returnsForecastForValidLocation`.

Use parameterized tests for Met Office tool/client behavior. Happy-path coverage should exercise every MCP tool method. Keep fast mock HTTP tests for normal local verification, and keep Testcontainers MockServer tests for end-to-end HTTP verification against a real container.

Use Mockito for thin MCP tool adapters when verifying request record construction and delegation to product clients. Use mock HTTP or Testcontainers for product client behavior so headers, paths, query parameters, response parsing, binary base64 handling, and typed errors are exercised through HTTP.

Name and structure unit test classes to map directly to the production class under test, and place them in the matching package path under `src/test/java`. For example, `client/atmospheric/AtmosphericDataHubClient.java` maps to `client/atmospheric/AtmosphericDataHubClientTests.java`. Keep cross-class HTTP or container coverage in clearly named integration/e2e test classes instead of mixing it into unit tests.

Unhappy-path tests should cover representative non-200 statuses and invalid local requests, including invalid timesteps and invalid BPF collection IDs. When adding or changing a tool method, update both happy-path and unhappy-path parameterized cases.

Run `.\gradlew.bat test` before opening a pull request.

Testcontainers tests require a working Docker engine. If Docker is unavailable, they may skip locally; run them on a Docker-enabled machine or CI before merging HTTP client changes.

## Memory Bank

This repository keeps durable project context in `memory-bank/`. Before making non-trivial changes, read `memory-bank/README.md`, `memory-bank/project-context.md`, `memory-bank/decisions.md`, and `memory-bank/progress.md`.

Update the memory bank when a change affects architecture, public behavior, dependencies, configuration, test strategy, known risks, or follow-up work. Keep entries concise and factual. Do not store secrets, API keys, local credentials, or generated output in the memory bank.

## Commit & Pull Request Guidelines

This checkout does not include Git history, so no repository-specific commit pattern can be verified. Use short, imperative commit messages such as `Add weather forecast endpoint` or `Fix application configuration`.

Pull requests should include a concise summary, the reason for the change, test results, and any configuration impact. Link related issues when available. For API or behavior changes, include example requests, responses, or logs that help reviewers verify the change.

## Security & Configuration Tips

Do not commit secrets, API keys, or local credentials to `application.yaml`. Prefer environment variables or profile-specific local configuration ignored by Git. Document any required external configuration in the pull request and in project docs when it becomes part of normal setup.

Each Met Office product client owns its environment-backed API key field and passes it to the shared `MetOfficeDataHubClient`, which adds it as the `apikey` header. Do not move API key lookup back into the shared transport, and do not send API keys as query parameters. When adding or changing a product client, include tests that verify the expected `apikey` header and that the request URI does not contain an API key.
