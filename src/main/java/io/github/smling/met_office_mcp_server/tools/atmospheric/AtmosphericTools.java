package io.github.smling.met_office_mcp_server.tools.atmospheric;

import io.github.smling.met_office_mcp_server.client.atmospheric.AtmosphericDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericFileRequest;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericOrdersRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool adapter for Met Office atmospheric model APIs.
 */
@Service
public class AtmosphericTools {

    private final AtmosphericDataHubClient atmosphericClient;

    public AtmosphericTools(AtmosphericDataHubClient atmosphericClient) {
        this.atmosphericClient = atmosphericClient;
    }

    /**
     * Lists active atmospheric model orders.
     *
     * @return response envelope containing Met Office order metadata or a typed error
     */
    @McpTool(name = "metoffice_atmospheric_orders", description = "List active Met Office atmospheric model orders.")
    public MetOfficeToolResponse atmosphericOrders() {
        return atmosphericClient.orders();
    }

    /**
     * Gets latest metadata for an atmospheric model order.
     *
     * @param orderId atmospheric order ID
     * @param run optional model run filter
     * @param dataSpec optional atmospheric data specification version
     * @return response envelope containing latest order metadata or a typed error
     */
    @McpTool(
            name = "metoffice_atmospheric_latest_order",
            description = "Get latest metadata for a Met Office atmospheric model order.")
    public MetOfficeToolResponse atmosphericLatestOrder(
            @McpToolParam(description = "Atmospheric order ID.", required = true) String orderId,
            @McpToolParam(description = "Optional model run filter, for example 00, 06, 12, or 18.", required = false)
                    String run,
            @McpToolParam(description = "Optional atmospheric data specification version.", required = false)
                    String dataSpec) {
        return atmosphericClient.latestOrder(new AtmosphericOrdersRequest(orderId, run, dataSpec));
    }

    /**
     * Downloads an atmospheric GRIB file as base64.
     *
     * @param orderId atmospheric order ID
     * @param run optional model run filter retained for client context
     * @param dataSpec optional atmospheric data specification version
     * @param fileId file ID from latest order metadata
     * @param binaryDebug when true, return compact binary diagnostics instead of full base64
     * @return response envelope containing base64 GRIB bytes, binary diagnostics, or a typed error
     */
    @McpTool(
            name = "metoffice_atmospheric_file",
            description = "Download a Met Office atmospheric model GRIB file as base64.")
    public MetOfficeToolResponse atmosphericFile(
            @McpToolParam(description = "Atmospheric order ID.", required = true) String orderId,
            @McpToolParam(description = "Optional model run filter retained for client context.", required = false) String run,
            @McpToolParam(description = "Optional atmospheric data specification version.", required = false) String dataSpec,
            @McpToolParam(description = "File ID from the latest order metadata.", required = true) String fileId,
            @McpToolParam(
                            description =
                                    "When true, return byte length and base64 preview instead of the full binary payload.",
                            required = false)
                    Boolean binaryDebug) {
        return atmosphericClient.file(new AtmosphericFileRequest(orderId, run, dataSpec, fileId, binaryDebug));
    }
}
