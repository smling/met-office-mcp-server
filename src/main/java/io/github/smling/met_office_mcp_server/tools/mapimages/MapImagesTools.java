package io.github.smling.met_office_mcp_server.tools.mapimages;

import io.github.smling.met_office_mcp_server.client.mapimages.MapImagesDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageFileRequest;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageOrdersRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool adapter for Met Office map image APIs.
 */
@Service
public class MapImagesTools {

    private final MapImagesDataHubClient mapImagesClient;

    public MapImagesTools(MapImagesDataHubClient mapImagesClient) {
        this.mapImagesClient = mapImagesClient;
    }

    /**
     * Lists active map image orders.
     *
     * @return response envelope containing order metadata or a typed error
     */
    @McpTool(name = "metoffice_map_image_orders", description = "List active Met Office map image orders.")
    public MetOfficeToolResponse mapImageOrders() {
        return mapImagesClient.orders();
    }

    /**
     * Gets latest metadata for a map image order.
     *
     * @param orderId map image order ID
     * @param run optional model run filter
     * @param includeLand whether land map features should be included when supported
     * @return response envelope containing latest order metadata or a typed error
     */
    @McpTool(name = "metoffice_map_image_latest_order", description = "Get latest metadata for a Met Office map image order.")
    public MetOfficeToolResponse mapImageLatestOrder(
            @McpToolParam(description = "Map image order ID.", required = true) String orderId,
            @McpToolParam(description = "Optional model run filter.", required = false) String run,
            @McpToolParam(description = "Whether land map features should be included when supported.", required = false)
                    Boolean includeLand) {
        return mapImagesClient.latestOrder(new MapImageOrdersRequest(orderId, run, includeLand));
    }

    /**
     * Downloads a PNG map image file as base64.
     *
     * @param orderId map image order ID
     * @param run optional model run filter retained for client context
     * @param fileId file ID from latest order metadata
     * @param includeLand whether land map features should be included when supported
     * @return response envelope containing base64 PNG bytes or a typed error
     */
    @McpTool(name = "metoffice_map_image_file", description = "Download a Met Office map image PNG file as base64.")
    public MetOfficeToolResponse mapImageFile(
            @McpToolParam(description = "Map image order ID.", required = true) String orderId,
            @McpToolParam(description = "Optional model run filter retained for client context.", required = false) String run,
            @McpToolParam(description = "File ID from the latest order metadata.", required = true) String fileId,
            @McpToolParam(description = "Whether land map features should be included when supported.", required = false)
                    Boolean includeLand) {
        return mapImagesClient.file(new MapImageFileRequest(orderId, run, fileId, includeLand));
    }
}
