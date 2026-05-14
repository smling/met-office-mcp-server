package io.github.smling.met_office_mcp_server.client.mapimages;

import io.github.smling.met_office_mcp_server.MetOfficeProperties;
import io.github.smling.met_office_mcp_server.client.MetOfficeClientValidation;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageFileRequest;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageOrdersRequest;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Client for Met Office map image order metadata and PNG file downloads.
 */
@Service
public class MapImagesDataHubClient {

    private static final String ORDERS = "metoffice_map_image_orders";
    private static final String LATEST_ORDER = "metoffice_map_image_latest_order";
    private static final String FILE = "metoffice_map_image_file";

    private final MetOfficeDataHubClient client;
    private final MetOfficeProperties.MapImages properties;
    private final String apiKey;

    public MapImagesDataHubClient(MetOfficeDataHubClient client, MetOfficeProperties properties) {
        this.client = client;
        this.properties = properties.mapImages();
        this.apiKey = this.properties.apiKey();
    }

    public MetOfficeToolResponse orders() {
        URI uri = client.uri(properties.baseUrl(), "/orders", client.query("detail", "MINIMAL"));
        return client.getJson(MetOfficeProduct.MAP_IMAGES, ORDERS, uri, apiKey);
    }

    public MetOfficeToolResponse latestOrder(MapImageOrdersRequest request) {
        MetOfficeToolResponse validation = MetOfficeClientValidation.requiredString(
                MetOfficeProduct.MAP_IMAGES, LATEST_ORDER, "orderId", request.orderId());
        if (validation != null) {
            return validation;
        }
        URI uri = client.uri(
                properties.baseUrl(),
                "/orders/" + client.encodedPathSegment(request.orderId()) + "/latest",
                client.query("runfilter", request.run()),
                client.query("includeLand", request.includeLand()),
                client.query("detail", "MINIMAL"));
        return client.getJson(MetOfficeProduct.MAP_IMAGES, LATEST_ORDER, uri, apiKey);
    }

    public MetOfficeToolResponse file(MapImageFileRequest request) {
        MetOfficeToolResponse validation = MetOfficeClientValidation.requiredString(
                MetOfficeProduct.MAP_IMAGES, FILE, "orderId", request.orderId());
        if (validation != null) {
            return validation;
        }
        validation = MetOfficeClientValidation.requiredString(
                MetOfficeProduct.MAP_IMAGES, FILE, "fileId", request.fileId());
        if (validation != null) {
            return validation;
        }
        URI uri = client.uri(
                properties.baseUrl(),
                "/orders/" + client.encodedPathSegment(request.orderId()) + "/latest/"
                        + client.encodedPathSegment(request.fileId()) + "/data",
                client.query("includeLand", request.includeLand()));
        return client.getBinary(
                MetOfficeProduct.MAP_IMAGES,
                FILE,
                uri,
                apiKey,
                MediaType.IMAGE_PNG,
                Boolean.TRUE.equals(request.binaryDebug()));
    }
}
