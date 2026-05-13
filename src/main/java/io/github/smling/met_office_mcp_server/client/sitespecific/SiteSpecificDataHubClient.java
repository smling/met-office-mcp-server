package io.github.smling.met_office_mcp_server.client.sitespecific;

import io.github.smling.met_office_mcp_server.MetOfficeProperties;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfForecastRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfLocationsRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.GlobalSpotRequest;
import java.net.URI;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Client for Met Office site-specific Global Spot and blended probabilistic forecast APIs.
 */
@Service
public class SiteSpecificDataHubClient {

    private static final String GLOBAL_SPOT = "metoffice_site_specific_global_spot";
    private static final String BPF_COLLECTIONS = "metoffice_bpf_collections";
    private static final String BPF_LOCATIONS = "metoffice_bpf_locations";
    private static final String BPF_FORECAST = "metoffice_bpf_forecast";
    private static final Set<String> GLOBAL_SPOT_TIMESTEPS = Set.of("hourly", "three-hourly", "daily");
    private static final Set<String> BPF_COLLECTION_IDS = Set.of(
            "improver-percentiles-spot-global",
            "improver-probabilities-spot-global",
            "improver-probabilities-spot-uk",
            "improver-percentiles-spot-uk");

    private final MetOfficeDataHubClient client;
    private final MetOfficeProperties.SiteSpecific properties;
    private final String apiKey;

    public SiteSpecificDataHubClient(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        this.client = client;
        this.properties = properties.siteSpecific();
        this.apiKey = this.properties.apiKey();
    }

    public MetOfficeToolResponse globalSpot(GlobalSpotRequest request) {
        if (!GLOBAL_SPOT_TIMESTEPS.contains(request.timesteps())) {
            return validationError(GLOBAL_SPOT, "timesteps must be one of hourly, three-hourly, or daily");
        }
        URI uri = client.uri(
                properties.globalSpotBaseUrl(),
                "/" + request.timesteps(),
                client.query("latitude", request.latitude()),
                client.query("longitude", request.longitude()),
                client.query("excludeParameterMetadata", request.excludeParameterMetadata()),
                client.query("includeLocationName", request.includeLocationName()));
        return client.getJson(MetOfficeProduct.SITE_SPECIFIC, GLOBAL_SPOT, uri, apiKey);
    }

    public MetOfficeToolResponse bpfCollections() {
        URI uri = client.uri(properties.bpfBaseUrl(), "/collections");
        return client.getJson(MetOfficeProduct.SITE_SPECIFIC, BPF_COLLECTIONS, uri, apiKey);
    }

    public MetOfficeToolResponse bpfLocations(BpfLocationsRequest request) {
        MetOfficeToolResponse validation = validateBpfCollection(BPF_LOCATIONS, request.collectionId());
        if (validation != null) {
            return validation;
        }
        URI uri = client.uri(properties.bpfBaseUrl(), "/collections/" + request.collectionId() + "/locations");
        return client.getJson(MetOfficeProduct.SITE_SPECIFIC, BPF_LOCATIONS, uri, apiKey);
    }

    public MetOfficeToolResponse bpfForecast(BpfForecastRequest request) {
        MetOfficeToolResponse validation = validateBpfCollection(BPF_FORECAST, request.collectionId());
        if (validation != null) {
            return validation;
        }
        URI uri = client.uri(
                properties.bpfBaseUrl(),
                "/collections/" + request.collectionId() + "/locations/" + request.locationId());
        return client.getJson(MetOfficeProduct.SITE_SPECIFIC, BPF_FORECAST, uri, apiKey);
    }

    private MetOfficeToolResponse validateBpfCollection(String operation, String collectionId) {
        if (!BPF_COLLECTION_IDS.contains(collectionId)) {
            return validationError(operation, "collectionId must be one of " + String.join(", ", BPF_COLLECTION_IDS));
        }
        return null;
    }

    private MetOfficeToolResponse validationError(String operation, String message) {
        return MetOfficeToolResponse.error(
                MetOfficeProduct.SITE_SPECIFIC,
                operation,
                new MetOfficeToolResponse.MetOfficeError(message, 400, MetOfficeProduct.SITE_SPECIFIC.id(), null, null));
    }
}
