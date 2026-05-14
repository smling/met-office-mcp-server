package io.github.smling.met_office_mcp_server.client.atmospheric;

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
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericFileRequest;
import io.github.smling.met_office_mcp_server.model.atmospheric.AtmosphericOrdersRequest;
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

class AtmosphericDataHubClientTests {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final MetOfficeDataHubClient dataHubClient = MetOfficeTestSupport.client(restClientBuilder);
    private final AtmosphericDataHubClient client =
            new AtmosphericDataHubClient(dataHubClient, MetOfficeTestSupport.properties("atm-key", "site", "obs", "map"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("happyCases")
    void callsAtmosphericEndpoint(String name, URI uri, MediaType contentType, byte[] body, Expected expected) {
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "atm-key"))
                .andExpect(request -> assertFalse(String.valueOf(request.getURI().getQuery()).contains("apikey")))
                .andRespond(withSuccess(body, contentType));

        MetOfficeToolResponse response = expected.call().apply(client);

        assertEquals(200, response.status());
        assertEquals("atmospheric", response.product());
        assertEquals(expected.operation(), response.operation());
        assertNull(response.error());
        expected.assertPayload(response);
        server.verify();
    }

    private static Stream<Arguments> happyCases() {
        return Stream.of(
                Arguments.of(
                        "orders",
                        URI.create("https://example.test/atmos/orders?detail=MINIMAL"),
                        MediaType.APPLICATION_JSON,
                        bytes("{\"orders\":[\"atm\"]}"),
                        Expected.json("metoffice_atmospheric_orders", AtmosphericDataHubClient::orders, "orders")),
                Arguments.of(
                        "latest order",
                        URI.create("https://example.test/atmos/orders/order-1/latest?dataSpec=1.0&runfilter=00&detail=MINIMAL"),
                        MediaType.APPLICATION_JSON,
                        bytes("{\"latest\":\"atm\"}"),
                        Expected.json(
                                "metoffice_atmospheric_latest_order",
                                client -> client.latestOrder(new AtmosphericOrdersRequest("order-1", "00", "1.0")),
                                "latest")),
                Arguments.of(
                        "file",
                        URI.create("https://example.test/atmos/orders/order-1/latest/file-1/data?dataSpec=1.0"),
                        MediaType.parseMediaType("application/x-grib"),
                        "grib".getBytes(),
                        Expected.binary(
                                "metoffice_atmospheric_file",
                                client -> client.file(new AtmosphericFileRequest("order-1", null, "1.0", "file-1")),
                                "Z3JpYg==")),
                Arguments.of(
                        "file with reserved path characters",
                        URI.create("https://example.test/atmos/orders/order-1/latest/a%2Bb%2Fc%3Fd/data?dataSpec=1.0"),
                        MediaType.parseMediaType("application/x-grib"),
                        "grib".getBytes(),
                        Expected.binary(
                                "metoffice_atmospheric_file",
                                client -> client.file(new AtmosphericFileRequest("order-1", null, "1.0", "a+b/c?d")),
                                "Z3JpYg==")));
    }

    private static byte[] bytes(String value) {
        return value.getBytes();
    }

    private record Expected(
            String operation,
            Function<AtmosphericDataHubClient, MetOfficeToolResponse> call,
            String jsonField,
            String binaryBase64) {

        static Expected json(
                String operation, Function<AtmosphericDataHubClient, MetOfficeToolResponse> call, String jsonField) {
            return new Expected(operation, call, jsonField, null);
        }

        static Expected binary(
                String operation, Function<AtmosphericDataHubClient, MetOfficeToolResponse> call, String binaryBase64) {
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
