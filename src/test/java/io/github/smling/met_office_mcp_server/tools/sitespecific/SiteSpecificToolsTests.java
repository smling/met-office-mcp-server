package io.github.smling.met_office_mcp_server.tools.sitespecific;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.met_office_mcp_server.client.sitespecific.SiteSpecificDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfForecastRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfLocationsRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.GlobalSpotRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SiteSpecificToolsTests {

    private final SiteSpecificDataHubClient client = mock(SiteSpecificDataHubClient.class);
    private final SiteSpecificTools tools = new SiteSpecificTools(client);

    @Test
    void globalSpotBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("global");
        ArgumentCaptor<GlobalSpotRequest> captor = ArgumentCaptor.forClass(GlobalSpotRequest.class);
        when(client.globalSpot(captor.capture())).thenReturn(response);

        assertSame(response, tools.globalSpot(51.5, -0.1, "hourly", true, false));
        assertEquals(51.5, captor.getValue().latitude());
        assertEquals(-0.1, captor.getValue().longitude());
        assertEquals("hourly", captor.getValue().timesteps());
        assertEquals(true, captor.getValue().excludeParameterMetadata());
        assertEquals(false, captor.getValue().includeLocationName());
    }

    @Test
    void bpfCollectionsDelegatesToClient() {
        MetOfficeToolResponse response = ok("collections");
        when(client.bpfCollections()).thenReturn(response);

        assertSame(response, tools.bpfCollections());
        verify(client).bpfCollections();
    }

    @Test
    void bpfLocationsBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("locations");
        ArgumentCaptor<BpfLocationsRequest> captor = ArgumentCaptor.forClass(BpfLocationsRequest.class);
        when(client.bpfLocations(captor.capture())).thenReturn(response);

        assertSame(response, tools.bpfLocations("collection-1"));
        assertEquals("collection-1", captor.getValue().collectionId());
    }

    @Test
    void bpfForecastBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("forecast");
        ArgumentCaptor<BpfForecastRequest> captor = ArgumentCaptor.forClass(BpfForecastRequest.class);
        when(client.bpfForecast(captor.capture())).thenReturn(response);

        assertSame(response, tools.bpfForecast("collection-1", "location-1"));
        assertEquals("collection-1", captor.getValue().collectionId());
        assertEquals("location-1", captor.getValue().locationId());
    }

    private MetOfficeToolResponse ok(String operation) {
        return MetOfficeToolResponse.binary(MetOfficeProduct.SITE_SPECIFIC, operation, 200, "application/octet-stream", "AA==");
    }
}
