package io.github.smling.met_office_mcp_server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class MetOfficePropertiesTests {

    @Test
    void bindsFromExternalConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("met-office.atmospheric.api-key", "atm")
                .withProperty("met-office.atmospheric.base-url", "https://atm.example")
                .withProperty("met-office.site-specific.api-key", "site")
                .withProperty("met-office.site-specific.global-spot-base-url", "https://site.example")
                .withProperty("met-office.site-specific.bpf-base-url", "https://bpf.example")
                .withProperty("met-office.observations.api-key", "obs")
                .withProperty("met-office.observations.base-url", "https://obs.example")
                .withProperty("met-office.map-images.api-key", "map")
                .withProperty("met-office.map-images.base-url", "https://map.example");

        MetOfficeProperties bound = Binder.get(environment)
                .bind("met-office", Bindable.of(MetOfficeProperties.class))
                .get();

        assertEquals("atm", bound.atmospheric().apiKey());
        assertEquals("https://site.example", bound.siteSpecific().globalSpotBaseUrl());
        assertEquals("https://bpf.example", bound.siteSpecific().bpfBaseUrl());
        assertEquals("obs", bound.observations().apiKey());
        assertEquals("https://map.example", bound.mapImages().baseUrl());
    }

    @Test
    void defaultsMissingNestedPropertiesToEmptyRecords() {
        MetOfficeProperties properties = new MetOfficeProperties(null, null, null, null);

        assertEquals("", properties.atmospheric().apiKey());
        assertEquals("", properties.siteSpecific().apiKey());
        assertEquals("", properties.observations().apiKey());
        assertEquals("", properties.mapImages().apiKey());
    }
}
