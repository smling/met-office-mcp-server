package io.github.smling.met_office_mcp_server.model.sitespecific;

public record GlobalSpotRequest(
        double latitude,
        double longitude,
        String timesteps,
        Boolean excludeParameterMetadata,
        Boolean includeLocationName) {
}
