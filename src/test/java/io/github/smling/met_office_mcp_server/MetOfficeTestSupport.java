package io.github.smling.met_office_mcp_server;

import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.client.atmospheric.AtmosphericDataHubClient;
import io.github.smling.met_office_mcp_server.client.mapimages.MapImagesDataHubClient;
import io.github.smling.met_office_mcp_server.client.observations.ObservationsDataHubClient;
import io.github.smling.met_office_mcp_server.client.sitespecific.SiteSpecificDataHubClient;
import io.github.smling.met_office_mcp_server.tools.atmospheric.AtmosphericTools;
import io.github.smling.met_office_mcp_server.tools.mapimages.MapImagesTools;
import io.github.smling.met_office_mcp_server.tools.observations.ObservationsTools;
import io.github.smling.met_office_mcp_server.tools.sitespecific.SiteSpecificTools;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

public final class MetOfficeTestSupport {

    private MetOfficeTestSupport() {
    }

    public static MetOfficeProperties properties(
            String baseUrl, String atmosphericKey, String siteSpecificKey, String observationsKey, String mapImagesKey) {
        return new MetOfficeProperties(
                new MetOfficeProperties.Atmospheric(atmosphericKey, baseUrl + "/atmos"),
                new MetOfficeProperties.SiteSpecific(siteSpecificKey, baseUrl + "/site", baseUrl + "/bpf"),
                new MetOfficeProperties.Observations(observationsKey, baseUrl + "/observations"),
                new MetOfficeProperties.MapImages(mapImagesKey, baseUrl + "/map"));
    }

    public static MetOfficeProperties properties(
            String atmosphericKey, String siteSpecificKey, String observationsKey, String mapImagesKey) {
        return properties("https://example.test", atmosphericKey, siteSpecificKey, observationsKey, mapImagesKey);
    }

    public static MetOfficeDataHubClient client(RestClient.Builder restClientBuilder) {
        return new MetOfficeDataHubClient(restClientBuilder, objectMapper(), new SimpleMeterRegistry());
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    public static AtmosphericTools atmosphericTools(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        return new AtmosphericTools(new AtmosphericDataHubClient(client, properties));
    }

    public static SiteSpecificTools siteSpecificTools(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        return new SiteSpecificTools(new SiteSpecificDataHubClient(client, properties));
    }

    public static ObservationsTools observationsTools(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        return new ObservationsTools(new ObservationsDataHubClient(client, properties));
    }

    public static MapImagesTools mapImagesTools(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        return new MapImagesTools(new MapImagesDataHubClient(client, properties));
    }
}
