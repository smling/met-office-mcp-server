package io.github.smling.met_office_mcp_server.tools.mapimages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.met_office_mcp_server.client.mapimages.MapImagesDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageFileRequest;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageOrdersRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MapImagesToolsTests {

    private final MapImagesDataHubClient client = mock(MapImagesDataHubClient.class);
    private final MapImagesTools tools = new MapImagesTools(client);

    @Test
    void ordersDelegatesToClient() {
        MetOfficeToolResponse response = ok("orders");
        when(client.orders()).thenReturn(response);

        assertSame(response, tools.mapImageOrders());
        verify(client).orders();
    }

    @Test
    void latestOrderBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("latest");
        ArgumentCaptor<MapImageOrdersRequest> captor = ArgumentCaptor.forClass(MapImageOrdersRequest.class);
        when(client.latestOrder(captor.capture())).thenReturn(response);

        assertSame(response, tools.mapImageLatestOrder("order-1", "12", true));
        assertEquals("order-1", captor.getValue().orderId());
        assertEquals("12", captor.getValue().run());
        assertEquals(true, captor.getValue().includeLand());
    }

    @Test
    void fileBuildsRequestRecord() {
        MetOfficeToolResponse response = ok("file");
        ArgumentCaptor<MapImageFileRequest> captor = ArgumentCaptor.forClass(MapImageFileRequest.class);
        when(client.file(captor.capture())).thenReturn(response);

        assertSame(response, tools.mapImageFile("order-1", "12", "file-1", false));
        assertEquals("order-1", captor.getValue().orderId());
        assertEquals("file-1", captor.getValue().fileId());
        assertEquals(false, captor.getValue().includeLand());
    }

    private MetOfficeToolResponse ok(String operation) {
        return MetOfficeToolResponse.binary(MetOfficeProduct.MAP_IMAGES, operation, 200, "application/octet-stream", "AA==");
    }
}
