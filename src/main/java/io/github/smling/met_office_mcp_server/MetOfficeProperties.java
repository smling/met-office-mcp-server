package io.github.smling.met_office_mcp_server;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "met-office")
public record MetOfficeProperties(
        Atmospheric atmospheric,
        SiteSpecific siteSpecific,
        Observations observations,
        MapImages mapImages) {

    public MetOfficeProperties {
        atmospheric = atmospheric == null ? new Atmospheric("", "") : atmospheric;
        siteSpecific = siteSpecific == null ? new SiteSpecific("", "", "") : siteSpecific;
        observations = observations == null ? new Observations("", "") : observations;
        mapImages = mapImages == null ? new MapImages("", "") : mapImages;
    }

    public record Atmospheric(String apiKey, String baseUrl) {
    }

    public record SiteSpecific(String apiKey, String globalSpotBaseUrl, String bpfBaseUrl) {
    }

    public record Observations(String apiKey, String baseUrl) {
    }

    public record MapImages(String apiKey, String baseUrl) {
    }
}
