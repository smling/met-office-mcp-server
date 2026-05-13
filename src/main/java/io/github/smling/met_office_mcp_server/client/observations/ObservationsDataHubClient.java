package io.github.smling.met_office_mcp_server.client.observations;

import io.github.smling.met_office_mcp_server.MetOfficeProperties;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.observations.NearestStationRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByGeohashRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByLocationRequest;
import java.net.URI;
import org.springframework.stereotype.Service;

/**
 * Client for Met Office Land Observations station lookup and observation payloads.
 */
@Service
public class ObservationsDataHubClient {

    private static final String NEAREST_STATION = "metoffice_observations_nearest_station";
    private static final String BY_GEOHASH = "metoffice_observations_by_geohash";
    private static final String BY_LOCATION = "metoffice_observations_by_location";

    private final MetOfficeDataHubClient client;
    private final MetOfficeProperties.Observations properties;
    private final String apiKey;

    public ObservationsDataHubClient(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        this.client = client;
        this.properties = properties.observations();
        this.apiKey = this.properties.apiKey();
    }

    public MetOfficeToolResponse nearestStation(NearestStationRequest request) {
        URI uri = client.uri(
                properties.baseUrl(),
                "/nearest-geohash",
                client.query("latitude", request.latitude()),
                client.query("longitude", request.longitude()));
        return client.getJson(MetOfficeProduct.OBSERVATIONS, NEAREST_STATION, uri, apiKey);
    }

    public MetOfficeToolResponse byGeohash(ObservationsByGeohashRequest request) {
        URI uri = client.uri(properties.baseUrl(), "/observations/" + request.geohash());
        return client.getJson(MetOfficeProduct.OBSERVATIONS, BY_GEOHASH, uri, apiKey);
    }

    public MetOfficeToolResponse byLocation(ObservationsByLocationRequest request) {
        URI uri = client.uri(
                properties.baseUrl(),
                "/observations",
                client.query("latitude", request.latitude()),
                client.query("longitude", request.longitude()));
        return client.getJson(MetOfficeProduct.OBSERVATIONS, BY_LOCATION, uri, apiKey);
    }
}
