# Project Context

This repository is a Java 21 Spring Boot 4 application built with Gradle. It exposes Met Office Weather DataHub APIs as MCP tools over Spring AI MCP WebMVC.

Application code lives under `src/main/java/io/github/smling/met_office_mcp_server`. Tests mirror the package under `src/test/java/io/github/smling/met_office_mcp_server`.

The public interface is the MCP tool set under four product-specific tool packages: `tools/atmospheric`, `tools/sitespecific`, `tools/observations`, and `tools/mapimages`. Product-specific DataHub paths and validation live in matching client packages: `client/atmospheric`, `client/sitespecific`, `client/observations`, and `client/mapimages`.

Each product client owns its environment-backed API key field and passes that key into `MetOfficeDataHubClient`. The shared HTTP transport adds the key as the required `apikey` request header and normalizes JSON, binary, and error responses.

Shared response types live in `model/`. Product request records live in product-specific model subpackages, with one record per file to keep history traceable and minimize merge conflicts.

Runtime configuration is in `src/main/resources/application.yaml`. API keys are read from environment variables and must not be committed:

- `MET_OFFICE_ATMOSPHERIC_API_KEY`
- `MET_OFFICE_SITE_SPECIFIC_API_KEY`
- `MET_OFFICE_OBSERVATIONS_API_KEY`
- `MET_OFFICE_MAP_IMAGES_API_KEY`

Each product base URL is also configurable through environment variables. This matters because some Met Office DataHub endpoint details can move between product versions.
