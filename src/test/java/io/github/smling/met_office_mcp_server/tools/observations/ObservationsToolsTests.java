package io.github.smling.met_office_mcp_server.tools.observations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.smling.met_office_mcp_server.client.observations.ObservationsDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.observations.NearestStationRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByGeohashRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByLocationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ObservationsToolsTests {

    private final ObservationsDataHubClient client = mock(ObservationsDataHubClient.class);
    private final ObservationsTools tools = new ObservationsTools(client);

    @Test
    void nearestStationBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("nearest");
        ArgumentCaptor<NearestStationRequest> captor = ArgumentCaptor.forClass(NearestStationRequest.class);
        when(client.nearestStation(captor.capture())).thenReturn(response);

        assertSame(response, tools.observationsNearestStation(51.5, -0.1));
        assertEquals(51.5, captor.getValue().latitude());
        assertEquals(-0.1, captor.getValue().longitude());
    }

    @Test
    void byGeohashBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("geohash");
        ArgumentCaptor<ObservationsByGeohashRequest> captor = ArgumentCaptor.forClass(ObservationsByGeohashRequest.class);
        when(client.byGeohash(captor.capture())).thenReturn(response);

        assertSame(response, tools.observationsByGeohash("gcj8ds"));
        assertEquals("gcj8ds", captor.getValue().geohash());
    }

    @Test
    void byLocationBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("location");
        ArgumentCaptor<ObservationsByLocationRequest> captor = ArgumentCaptor.forClass(ObservationsByLocationRequest.class);
        when(client.byLocation(captor.capture())).thenReturn(response);

        assertSame(response, tools.observationsByLocation(51.5, -0.1));
        assertEquals(51.5, captor.getValue().latitude());
        assertEquals(-0.1, captor.getValue().longitude());
    }

    private MetOfficeToolResponse ok(String operation) {
        return MetOfficeToolResponse.binary(MetOfficeProduct.OBSERVATIONS, operation, 200, "application/octet-stream", "AA==");
    }
}
