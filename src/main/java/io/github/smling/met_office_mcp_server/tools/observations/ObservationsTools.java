package io.github.smling.met_office_mcp_server.tools.observations;

import io.github.smling.met_office_mcp_server.client.observations.ObservationsDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.observations.NearestStationRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByGeohashRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByLocationRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool adapter for Met Office Land Observations APIs.
 */
@Service
public class ObservationsTools {

    private final ObservationsDataHubClient observationsClient;

    public ObservationsTools(ObservationsDataHubClient observationsClient) {
        this.observationsClient = observationsClient;
    }

    /**
     * Finds the nearest Land Observations station for a latitude and longitude.
     *
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @return response envelope containing station metadata or a typed error
     */
    @McpTool(
            name = "metoffice_observations_nearest_station",
            description = "Find the nearest Met Office Land Observations station geohash for a latitude and longitude.")
    public MetOfficeToolResponse observationsNearestStation(
            @McpToolParam(description = "Latitude in decimal degrees.", required = true) double latitude,
            @McpToolParam(description = "Longitude in decimal degrees.", required = true) double longitude) {
        return observationsClient.nearestStation(new NearestStationRequest(latitude, longitude));
    }

    /**
     * Gets recent Land Observations for a station geohash.
     *
     * @param geohash station geohash
     * @return response envelope containing observation JSON or a typed error
     */
    @McpTool(
            name = "metoffice_observations_by_geohash",
            description = "Get recent Met Office Land Observations for a station geohash.")
    public MetOfficeToolResponse observationsByGeohash(
            @McpToolParam(description = "Six-character station geohash.", required = true) String geohash) {
        return observationsClient.byGeohash(new ObservationsByGeohashRequest(geohash));
    }

    /**
     * Gets recent Land Observations for the nearest station to a latitude and longitude.
     *
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @return response envelope containing observation JSON or a typed error
     */
    @McpTool(
            name = "metoffice_observations_by_location",
            description = "Get recent Met Office Land Observations for the nearest station to a latitude and longitude.")
    public MetOfficeToolResponse observationsByLocation(
            @McpToolParam(description = "Latitude in decimal degrees.", required = true) double latitude,
            @McpToolParam(description = "Longitude in decimal degrees.", required = true) double longitude) {
        return observationsClient.byLocation(new ObservationsByLocationRequest(latitude, longitude));
    }
}
