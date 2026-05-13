package io.github.smling.met_office_mcp_server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.tools.atmospheric.AtmosphericTools;
import io.github.smling.met_office_mcp_server.tools.mapimages.MapImagesTools;
import io.github.smling.met_office_mcp_server.tools.observations.ObservationsTools;
import io.github.smling.met_office_mcp_server.tools.sitespecific.SiteSpecificTools;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.web.client.RestClient;

@Testcontainers
class MetOfficeToolsContainerTests {

    @Container
    private static final MockServerContainer MOCK_SERVER = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    private RestClient mockServerAdmin;
    private AtmosphericTools atmosphericTools;
    private SiteSpecificTools siteSpecificTools;
    private ObservationsTools observationsTools;
    private MapImagesTools mapImagesTools;

    @BeforeEach
    void setUp() {
        mockServerAdmin = RestClient.builder().baseUrl(MOCK_SERVER.getEndpoint()).build();
        resetMockServer();
        MetOfficeProperties properties = MetOfficeTestSupport.properties(
                MOCK_SERVER.getEndpoint(), "atm-key", "site-key", "obs-key", "map-key");
        MetOfficeDataHubClient client = MetOfficeTestSupport.client(RestClient.builder());
        atmosphericTools = MetOfficeTestSupport.atmosphericTools(client, properties);
        siteSpecificTools = MetOfficeTestSupport.siteSpecificTools(client, properties);
        observationsTools = MetOfficeTestSupport.observationsTools(client, properties);
        mapImagesTools = MetOfficeTestSupport.mapImagesTools(client, properties);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("happyCases")
    void happyCasesCallContainerizedDataHub(
            String name, ExpectedRequest request, ExpectedResponse response, Expected expected) {
        expect(request, response);

        MetOfficeToolResponse actual = expected.call().apply(this);

        assertEquals(200, actual.status());
        assertEquals(expected.product(), actual.product());
        assertEquals(expected.operation(), actual.operation());
        assertNull(actual.error());
        expected.assertPayload(actual);
        verify(request);
    }

    @ParameterizedTest(name = "status {0}")
    @MethodSource("nonSuccessStatuses")
    void unhappyCasesReturnTypedErrorsForNonSuccessResponses(int status) {
        ExpectedRequest request = request("GET", "/map/orders")
                .query("detail", "MINIMAL")
                .header("apikey", "map-key");
        expect(request, httpResponse(status, "text/plain", "error-" + status));

        MetOfficeToolResponse actual = mapImagesTools.mapImageOrders();

        assertEquals(status, actual.status());
        assertNull(actual.data());
        assertNull(actual.binaryBase64());
        assertNotNull(actual.error());
        assertEquals("map-images", actual.error().product());
        assertEquals("error-" + status, actual.error().responseBodyPreview());
        verify(request);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequestCases")
    void unhappyCasesRejectInvalidRequestsBeforeHttpCall(String name, Function<MetOfficeToolsContainerTests, MetOfficeToolResponse> call) {
        MetOfficeToolResponse actual = call.apply(this);

        assertEquals(400, actual.status());
        assertNotNull(actual.error());
        assertNull(actual.data());
        assertNull(actual.binaryBase64());
        verifyZeroInteractions();
    }

    @Test
    void invalidJsonResponseReturnsTypedBadGatewayError() {
        ExpectedRequest request = request("GET", "/map/orders")
                .query("detail", "MINIMAL")
                .header("apikey", "map-key");
        expect(request, httpResponse(200, "application/json", "{invalid"));

        MetOfficeToolResponse actual = mapImagesTools.mapImageOrders();

        assertEquals(502, actual.status());
        assertNotNull(actual.error());
        assertEquals("Unable to call Met Office DataHub", actual.error().message());
    }

    private static Stream<Arguments> happyCases() {
        return Stream.of(
                Arguments.of(
                        "atmospheric orders",
                        request("GET", "/atmos/orders")
                                .query("detail", "MINIMAL")
                                .header("apikey", "atm-key"),
                        json("{\"orders\":[\"atm\"]}"),
                        Expected.json("atmospheric", "metoffice_atmospheric_orders",
                                test -> test.atmosphericTools.atmosphericOrders(), "orders")),
                Arguments.of(
                        "atmospheric latest order",
                        request("GET", "/atmos/orders/order-1/latest")
                                .query("dataSpec", "1.0")
                                .query("runfilter", "00")
                                .query("detail", "MINIMAL")
                                .header("apikey", "atm-key"),
                        json("{\"latest\":\"atm\"}"),
                        Expected.json("atmospheric", "metoffice_atmospheric_latest_order",
                                test -> test.atmosphericTools.atmosphericLatestOrder("order-1", "00", "1.0"), "latest")),
                Arguments.of(
                        "atmospheric file",
                        request("GET", "/atmos/orders/order-1/latest/file-1/data")
                                .query("dataSpec", "1.0")
                                .header("apikey", "atm-key"),
                        binary("application/x-grib", "grib".getBytes()),
                        Expected.binary("atmospheric", "metoffice_atmospheric_file",
                                test -> test.atmosphericTools.atmosphericFile("order-1", null, "1.0", "file-1"),
                                "Z3JpYg==")),
                Arguments.of(
                        "global spot",
                        request("GET", "/site/hourly")
                                .query("latitude", "51.5")
                                .query("longitude", "-0.1")
                                .query("excludeParameterMetadata", "true")
                                .query("includeLocationName", "false")
                                .header("apikey", "site-key"),
                        json("{\"type\":\"FeatureCollection\"}"),
                        Expected.json("site-specific", "metoffice_site_specific_global_spot",
                                test -> test.siteSpecificTools.globalSpot(51.5, -0.1, "hourly", true, false), "type")),
                Arguments.of(
                        "bpf collections",
                        request("GET", "/bpf/collections").header("apikey", "site-key"),
                        json("{\"collections\":[\"c\"]}"),
                        Expected.json("site-specific", "metoffice_bpf_collections",
                                test -> test.siteSpecificTools.bpfCollections(), "collections")),
                Arguments.of(
                        "bpf locations",
                        request("GET", "/bpf/collections/improver-percentiles-spot-global/locations")
                                .header("apikey", "site-key"),
                        json("{\"locations\":[\"l\"]}"),
                        Expected.json("site-specific", "metoffice_bpf_locations",
                                test -> test.siteSpecificTools.bpfLocations("improver-percentiles-spot-global"), "locations")),
                Arguments.of(
                        "bpf forecast",
                        request("GET", "/bpf/collections/improver-percentiles-spot-global/locations/loc-1")
                                .header("apikey", "site-key"),
                        json("{\"forecast\":\"ok\"}"),
                        Expected.json("site-specific", "metoffice_bpf_forecast",
                                test -> test.siteSpecificTools.bpfForecast("improver-percentiles-spot-global", "loc-1"),
                                "forecast")),
                Arguments.of(
                        "observations nearest",
                        request("GET", "/observations/nearest-geohash")
                                .query("latitude", "51.5")
                                .query("longitude", "-0.1")
                                .header("apikey", "obs-key"),
                        json("{\"station\":\"nearest\"}"),
                        Expected.json("observations", "metoffice_observations_nearest_station",
                                test -> test.observationsTools.observationsNearestStation(51.5, -0.1), "station")),
                Arguments.of(
                        "observations by geohash",
                        request("GET", "/observations/observations/gcj8ds")
                                .header("apikey", "obs-key"),
                        json("{\"station\":\"gcj8ds\"}"),
                        Expected.json("observations", "metoffice_observations_by_geohash",
                                test -> test.observationsTools.observationsByGeohash("gcj8ds"), "station")),
                Arguments.of(
                        "observations by location",
                        request("GET", "/observations/observations")
                                .query("latitude", "51.5")
                                .query("longitude", "-0.1")
                                .header("apikey", "obs-key"),
                        json("{\"station\":\"location\"}"),
                        Expected.json("observations", "metoffice_observations_by_location",
                                test -> test.observationsTools.observationsByLocation(51.5, -0.1), "station")),
                Arguments.of(
                        "map image orders",
                        request("GET", "/map/orders")
                                .query("detail", "MINIMAL")
                                .header("apikey", "map-key"),
                        json("{\"orders\":[\"map\"]}"),
                        Expected.json("map-images", "metoffice_map_image_orders",
                                test -> test.mapImagesTools.mapImageOrders(), "orders")),
                Arguments.of(
                        "map image latest order",
                        request("GET", "/map/orders/map-order/latest")
                                .query("runfilter", "12")
                                .query("includeLand", "true")
                                .query("detail", "MINIMAL")
                                .header("apikey", "map-key"),
                        json("{\"latest\":\"map\"}"),
                        Expected.json("map-images", "metoffice_map_image_latest_order",
                                test -> test.mapImagesTools.mapImageLatestOrder("map-order", "12", true), "latest")),
                Arguments.of(
                        "map image file",
                        request("GET", "/map/orders/map-order/latest/map-file/data")
                                .query("includeLand", "false")
                                .header("apikey", "map-key"),
                        binary("image/png", "png".getBytes()),
                        Expected.binary("map-images", "metoffice_map_image_file",
                                test -> test.mapImagesTools.mapImageFile("map-order", null, "map-file", false),
                                "cG5n")));
    }

    private static Stream<Integer> nonSuccessStatuses() {
        return Stream.of(300, 301, 400, 401, 403, 404, 409, 429, 500, 502, 503);
    }

    private static Stream<Arguments> invalidRequestCases() {
        return Stream.of(
                Arguments.of("invalid global spot timestep",
                        (Function<MetOfficeToolsContainerTests, MetOfficeToolResponse>)
                                test -> test.siteSpecificTools.globalSpot(51.5, -0.1, "weekly", null, null)),
                Arguments.of("invalid bpf locations collection",
                        (Function<MetOfficeToolsContainerTests, MetOfficeToolResponse>)
                                test -> test.siteSpecificTools.bpfLocations("invalid")),
                Arguments.of("invalid bpf forecast collection",
                        (Function<MetOfficeToolsContainerTests, MetOfficeToolResponse>)
                                test -> test.siteSpecificTools.bpfForecast("invalid", "loc-1")));
    }

    private void resetMockServer() {
        mockServerAdmin.put().uri("/mockserver/reset").retrieve().toBodilessEntity();
    }

    private void expect(ExpectedRequest request, ExpectedResponse response) {
        mockServerAdmin.put()
                .uri("/mockserver/expectation")
                .body(Map.of("httpRequest", request.toMap(), "httpResponse", response.toMap()))
                .retrieve()
                .toBodilessEntity();
    }

    private void verify(ExpectedRequest request) {
        mockServerAdmin.put()
                .uri("/mockserver/verify")
                .body(Map.of(
                        "httpRequest", request.toMap(),
                        "times", Map.of("atLeast", 1, "atMost", 1)))
                .retrieve()
                .toBodilessEntity();
    }

    private void verifyZeroInteractions() {
        mockServerAdmin.put()
                .uri("/mockserver/verify")
                .body(Map.of(
                        "httpRequest", Map.of("path", ".*"),
                        "times", Map.of("atMost", 0)))
                .retrieve()
                .toBodilessEntity();
    }

    private static ExpectedRequest request(String method, String path) {
        return new ExpectedRequest(method, path, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private static ExpectedResponse json(String body) {
        return httpResponse(200, "application/json", body);
    }

    private static ExpectedResponse binary(String contentType, byte[] body) {
        return httpResponse(200, contentType, Map.of(
                "type", "BINARY",
                "base64Bytes", Base64.getEncoder().encodeToString(body)));
    }

    private static ExpectedResponse httpResponse(int status, String contentType, Object body) {
        return new ExpectedResponse(status, Map.of("Content-Type", List.of(contentType)), body);
    }

    private record ExpectedRequest(
            String method,
            String path,
            Map<String, List<String>> queryStringParameters,
            Map<String, List<String>> headers) {

        ExpectedRequest query(String name, String value) {
            queryStringParameters.put(name, List.of(value));
            return this;
        }

        ExpectedRequest header(String name, String value) {
            headers.put(name, List.of(value));
            return this;
        }

        Map<String, Object> toMap() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("method", method);
            request.put("path", path);
            if (!queryStringParameters.isEmpty()) {
                request.put("queryStringParameters", queryStringParameters);
            }
            if (!headers.isEmpty()) {
                request.put("headers", headers);
            }
            return request;
        }
    }

    private record ExpectedResponse(int statusCode, Map<String, List<String>> headers, Object body) {

        Map<String, Object> toMap() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("statusCode", statusCode);
            response.put("headers", headers);
            response.put("body", body);
            return response;
        }
    }

    private record Expected(
            String product,
            String operation,
            Function<MetOfficeToolsContainerTests, MetOfficeToolResponse> call,
            String jsonField,
            String binaryBase64) {

        static Expected json(
                String product,
                String operation,
                Function<MetOfficeToolsContainerTests, MetOfficeToolResponse> call,
                String jsonField) {
            return new Expected(product, operation, call, jsonField, null);
        }

        static Expected binary(
                String product,
                String operation,
                Function<MetOfficeToolsContainerTests, MetOfficeToolResponse> call,
                String binaryBase64) {
            return new Expected(product, operation, call, null, binaryBase64);
        }

        void assertPayload(MetOfficeToolResponse response) {
            if (binaryBase64 == null) {
                assertNotNull(response.data());
                assertNotNull(response.data().get(jsonField));
                assertNull(response.binaryBase64());
                return;
            }
            assertNull(response.data());
            assertEquals(binaryBase64, response.binaryBase64());
        }
    }
}
