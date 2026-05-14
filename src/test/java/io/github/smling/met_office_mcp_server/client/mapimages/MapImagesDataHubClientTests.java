package io.github.smling.met_office_mcp_server.client.mapimages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.smling.met_office_mcp_server.MetOfficeTestSupport;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageFileRequest;
import io.github.smling.met_office_mcp_server.model.mapimages.MapImageOrdersRequest;
import java.net.URI;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MapImagesDataHubClientTests {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final MetOfficeDataHubClient dataHubClient = MetOfficeTestSupport.client(restClientBuilder);
    private final MapImagesDataHubClient client =
            new MapImagesDataHubClient(dataHubClient, MetOfficeTestSupport.properties("atm", "site", "obs", "map-key"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("happyCases")
    void callsMapImagesEndpoint(String name, URI uri, MediaType contentType, byte[] body, Expected expected) {
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "map-key"))
                .andExpect(request -> assertFalse(String.valueOf(request.getURI().getQuery()).contains("apikey")))
                .andRespond(withSuccess(body, contentType));

        MetOfficeToolResponse response = expected.call().apply(client);

        assertEquals(200, response.status());
        assertEquals("map-images", response.product());
        assertEquals(expected.operation(), response.operation());
        assertNull(response.error());
        expected.assertPayload(response);
        server.verify();
    }

    private static Stream<Arguments> happyCases() {
        return Stream.of(
                Arguments.of(
                        "orders",
                        URI.create("https://example.test/map/orders?detail=MINIMAL"),
                        MediaType.APPLICATION_JSON,
                        bytes("{\"orders\":[\"map\"]}"),
                        Expected.json("metoffice_map_image_orders", MapImagesDataHubClient::orders, "orders")),
                Arguments.of(
                        "latest order",
                        URI.create("https://example.test/map/orders/map-order/latest?runfilter=12&includeLand=true&detail=MINIMAL"),
                        MediaType.APPLICATION_JSON,
                        bytes("{\"latest\":\"map\"}"),
                        Expected.json(
                                "metoffice_map_image_latest_order",
                                client -> client.latestOrder(new MapImageOrdersRequest("map-order", "12", true)),
                                "latest")),
                Arguments.of(
                        "file",
                        URI.create("https://example.test/map/orders/map-order/latest/map-file/data?includeLand=false"),
                        MediaType.IMAGE_PNG,
                        "png".getBytes(),
                        Expected.binary(
                                "metoffice_map_image_file",
                                client -> client.file(new MapImageFileRequest("map-order", null, "map-file", false)),
                                "cG5n")),
                Arguments.of(
                        "file with reserved path character",
                        URI.create(
                                "https://example.test/map/orders/map-order/latest/cloud_amount_total_ts0_%2B00/data?includeLand=false"),
                        MediaType.IMAGE_PNG,
                        "png".getBytes(),
                        Expected.binary(
                                "metoffice_map_image_file",
                                client -> client.file(new MapImageFileRequest(
                                        "map-order", null, "cloud_amount_total_ts0_+00", false)),
                                "cG5n")));
    }

    private static byte[] bytes(String value) {
        return value.getBytes();
    }

    private record Expected(
            String operation,
            Function<MapImagesDataHubClient, MetOfficeToolResponse> call,
            String jsonField,
            String binaryBase64) {

        static Expected json(
                String operation, Function<MapImagesDataHubClient, MetOfficeToolResponse> call, String jsonField) {
            return new Expected(operation, call, jsonField, null);
        }

        static Expected binary(
                String operation, Function<MapImagesDataHubClient, MetOfficeToolResponse> call, String binaryBase64) {
            return new Expected(operation, call, null, binaryBase64);
        }

        void assertPayload(MetOfficeToolResponse response) {
            if (binaryBase64 == null) {
                assertNotNull(response.data().get(jsonField));
                assertNull(response.binaryBase64());
                return;
            }
            assertEquals(binaryBase64, response.binaryBase64());
            assertNull(response.data());
        }
    }
}
