package io.github.smling.met_office_mcp_server.tools.sitespecific;

import io.github.smling.met_office_mcp_server.client.sitespecific.SiteSpecificDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfForecastRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfLocationsRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.GlobalSpotRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool adapter for Met Office site-specific forecast APIs.
 */
@Service
public class SiteSpecificTools {

    private final SiteSpecificDataHubClient siteSpecificClient;

    public SiteSpecificTools(SiteSpecificDataHubClient siteSpecificClient) {
        this.siteSpecificClient = siteSpecificClient;
    }

    /**
     * Gets a Global Spot forecast for a latitude and longitude.
     *
     * @param latitude latitude in decimal degrees, between -90 and 90
     * @param longitude longitude in decimal degrees, between -180 and 180
     * @param timesteps forecast timestep, one of hourly, three-hourly, or daily
     * @param excludeParameterMetadata whether to exclude parameter metadata
     * @param includeLocationName whether to include location name
     * @return response envelope containing vendor GeoJSON or a typed error
     */
    @McpTool(
            name = "metoffice_site_specific_global_spot",
            description = "Get Met Office Global Spot site-specific forecast GeoJSON for a latitude and longitude.")
    public MetOfficeToolResponse globalSpot(
            @McpToolParam(description = "Latitude in decimal degrees, between -90 and 90.", required = true)
                    double latitude,
            @McpToolParam(description = "Longitude in decimal degrees, between -180 and 180.", required = true)
                    double longitude,
            @McpToolParam(description = "Forecast timestep: hourly, three-hourly, or daily.", required = true)
                    String timesteps,
            @McpToolParam(description = "Whether to exclude parameter metadata.", required = false)
                    Boolean excludeParameterMetadata,
            @McpToolParam(description = "Whether to include the location name.", required = false)
                    Boolean includeLocationName) {
        return siteSpecificClient.globalSpot(
                new GlobalSpotRequest(latitude, longitude, timesteps, excludeParameterMetadata, includeLocationName));
    }

    /**
     * Lists blended probabilistic forecast collections.
     *
     * @return response envelope containing BPF collections or a typed error
     */
    @McpTool(name = "metoffice_bpf_collections", description = "List Met Office BPF forecast collections.")
    public MetOfficeToolResponse bpfCollections() {
        return siteSpecificClient.bpfCollections();
    }

    /**
     * Lists locations for a blended probabilistic forecast collection.
     *
     * @param collectionId BPF collection ID
     * @return response envelope containing BPF locations or a typed error
     */
    @McpTool(name = "metoffice_bpf_locations", description = "List locations for a Met Office BPF collection.")
    public MetOfficeToolResponse bpfLocations(
            @McpToolParam(description = "BPF collection ID.", required = true) String collectionId) {
        return siteSpecificClient.bpfLocations(new BpfLocationsRequest(collectionId));
    }

    /**
     * Gets a blended probabilistic forecast for a collection and location.
     *
     * @param collectionId BPF collection ID
     * @param locationId BPF location ID
     * @return response envelope containing forecast JSON or a typed error
     */
    @McpTool(name = "metoffice_bpf_forecast", description = "Get BPF forecast JSON for a collection and location.")
    public MetOfficeToolResponse bpfForecast(
            @McpToolParam(description = "BPF collection ID.", required = true) String collectionId,
            @McpToolParam(description = "BPF location ID.", required = true) String locationId) {
        return siteSpecificClient.bpfForecast(new BpfForecastRequest(collectionId, locationId));
    }
}
