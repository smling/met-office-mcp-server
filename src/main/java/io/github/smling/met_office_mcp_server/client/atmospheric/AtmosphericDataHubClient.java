package io.github.smling.met_office_mcp_server.client.atmospheric;

import io.github.smling.met_office_mcp_server.MetOfficeProperties;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericFileRequest;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericOrdersRequest;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Client for Met Office atmospheric model order metadata and GRIB file downloads.
 */
@Service
public class AtmosphericDataHubClient {

    private static final String ORDERS = "metoffice_atmospheric_orders";
    private static final String LATEST_ORDER = "metoffice_atmospheric_latest_order";
    private static final String FILE = "metoffice_atmospheric_file";

    private final MetOfficeDataHubClient client;
    private final MetOfficeProperties.Atmospheric properties;
    private final String apiKey;

    public AtmosphericDataHubClient(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        this.client = client;
        this.properties = properties.atmospheric();
        this.apiKey = this.properties.apiKey();
    }

    public MetOfficeToolResponse orders() {
        URI uri = client.uri(properties.baseUrl(), "/orders", client.query("detail", "MINIMAL"));
        return client.getJson(MetOfficeProduct.ATMOSPHERIC, ORDERS, uri, apiKey);
    }

    public MetOfficeToolResponse latestOrder(AtmosphericOrdersRequest request) {
        URI uri = client.uri(
                properties.baseUrl(),
                "/orders/" + request.orderId() + "/latest",
                client.query("dataSpec", request.dataSpec()),
                client.query("runfilter", request.run()),
                client.query("detail", "MINIMAL"));
        return client.getJson(MetOfficeProduct.ATMOSPHERIC, LATEST_ORDER, uri, apiKey);
    }

    public MetOfficeToolResponse file(AtmosphericFileRequest request) {
        URI uri = client.uri(
                properties.baseUrl(),
                "/orders/" + client.encodedPathSegment(request.orderId()) + "/latest/"
                        + client.encodedPathSegment(request.fileId()) + "/data",
                client.query("dataSpec", request.dataSpec()));
        return client.getBinary(
                MetOfficeProduct.ATMOSPHERIC,
                FILE,
                uri,
                apiKey,
                MediaType.parseMediaType("application/x-grib"),
                Boolean.TRUE.equals(request.binaryDebug()));
    }
}
