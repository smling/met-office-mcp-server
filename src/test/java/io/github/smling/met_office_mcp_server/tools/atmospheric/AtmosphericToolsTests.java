package io.github.smling.met_office_mcp_server.tools.atmospheric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.met_office_mcp_server.client.atmospheric.AtmosphericDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericFileRequest;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericOrdersRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AtmosphericToolsTests {

    private final AtmosphericDataHubClient client = mock(AtmosphericDataHubClient.class);
    private final AtmosphericTools tools = new AtmosphericTools(client);

    @Test
    void atmosphericOrdersDelegatesToClient() {
        MetOfficeToolResponse response = ok("orders");
        when(client.orders()).thenReturn(response);

        assertSame(response, tools.atmosphericOrders());
        verify(client).orders();
    }

    @Test
    void atmosphericLatestOrderBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("latest");
        ArgumentCaptor<AtmosphericOrdersRequest> captor = ArgumentCaptor.forClass(AtmosphericOrdersRequest.class);
        when(client.latestOrder(captor.capture())).thenReturn(response);

        assertSame(response, tools.atmosphericLatestOrder("order-1", "00", "1.0"));
        assertEquals("order-1", captor.getValue().orderId());
        assertEquals("00", captor.getValue().run());
        assertEquals("1.0", captor.getValue().dataSpec());
    }

    @Test
    void atmosphericFileBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("file");
        ArgumentCaptor<AtmosphericFileRequest> captor = ArgumentCaptor.forClass(AtmosphericFileRequest.class);
        when(client.file(captor.capture())).thenReturn(response);

        assertSame(response, tools.atmosphericFile("order-1", "00", "1.0", "file-1"));
        assertEquals("order-1", captor.getValue().orderId());
        assertEquals("file-1", captor.getValue().fileId());
    }

    private MetOfficeToolResponse ok(String operation) {
        return MetOfficeToolResponse.binary(MetOfficeProduct.ATMOSPHERIC, operation, 200, "application/octet-stream", "AA==");
    }
}
