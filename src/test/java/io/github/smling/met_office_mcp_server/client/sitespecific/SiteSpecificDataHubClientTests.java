package io.github.smling.met_office_mcp_server.client.sitespecific;

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
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfForecastRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.BpfLocationsRequest;
import io.github.smling.met_office_mcp_server.model.sitespecific.GlobalSpotRequest;
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

class SiteSpecificDataHubClientTests {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final MetOfficeDataHubClient dataHubClient = MetOfficeTestSupport.client(restClientBuilder);
    private final SiteSpecificDataHubClient client =
            new SiteSpecificDataHubClient(dataHubClient, MetOfficeTestSupport.properties("atm", "site-key", "obs", "map"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("happyCases")
    void callsSiteSpecificEndpoint(String name, URI uri, Expected expected) {
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "site-key"))
                .andExpect(request -> assertFalse(String.valueOf(request.getURI().getQuery()).contains("apikey")))
                .andRespond(withSuccess("{\"value\":\"ok\"}", MediaType.APPLICATION_JSON));

        MetOfficeToolResponse response = expected.call().apply(client);

        assertEquals(200, response.status());
        assertEquals("site-specific", response.product());
        assertEquals(expected.operation(), response.operation());
        assertNotNull(response.data().get("value"));
        assertNull(response.error());
        server.verify();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidRequestsReturnValidationErrorWithoutHttpCall(String name, Function<SiteSpecificDataHubClient, MetOfficeToolResponse> call) {
        MetOfficeToolResponse response = call.apply(client);

        assertEquals(400, response.status());
        assertEquals("site-specific", response.product());
        assertNotNull(response.error());
        assertNull(response.data());
        assertNull(response.binaryBase64());
        server.verify();
    }

    private static Stream<Arguments> happyCases() {
        return Stream.of(
                Arguments.of(
                        "global spot",
                        URI.create("https://example.test/site/hourly?latitude=51.5&longitude=-0.1&excludeParameterMetadata=true&includeLocationName=false"),
                        new Expected(
                                "metoffice_site_specific_global_spot",
                                client -> client.globalSpot(new GlobalSpotRequest(51.5, -0.1, "hourly", true, false)))),
                Arguments.of(
                        "bpf collections",
                        URI.create("https://example.test/bpf/collections"),
                        new Expected("metoffice_bpf_collections", SiteSpecificDataHubClient::bpfCollections)),
                Arguments.of(
                        "bpf locations",
                        URI.create("https://example.test/bpf/collections/improver-percentiles-spot-global/locations"),
                        new Expected(
                                "metoffice_bpf_locations",
                                client -> client.bpfLocations(new BpfLocationsRequest("improver-percentiles-spot-global")))),
                Arguments.of(
                        "bpf forecast",
                        URI.create("https://example.test/bpf/collections/improver-percentiles-spot-global/locations/loc-1"),
                        new Expected(
                                "metoffice_bpf_forecast",
                                client -> client.bpfForecast(
                                        new BpfForecastRequest("improver-percentiles-spot-global", "loc-1")))),
                Arguments.of(
                        "bpf forecast with reserved location id",
                        URI.create("https://example.test/bpf/collections/improver-percentiles-spot-global/locations/loc%2B1%2Fpart"),
                        new Expected(
                                "metoffice_bpf_forecast",
                                client -> client.bpfForecast(
                                        new BpfForecastRequest("improver-percentiles-spot-global", "loc+1/part")))));
    }

    private static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(
                        "invalid latitude",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.globalSpot(new GlobalSpotRequest(91.0, -0.1, "hourly", null, null))),
                Arguments.of(
                        "invalid longitude",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.globalSpot(new GlobalSpotRequest(51.5, Double.NaN, "hourly", null, null))),
                Arguments.of(
                        "blank timestep",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.globalSpot(new GlobalSpotRequest(51.5, -0.1, "", null, null))),
                Arguments.of(
                        "invalid timestep",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.globalSpot(new GlobalSpotRequest(51.5, -0.1, "weekly", null, null))),
                Arguments.of(
                        "blank locations collection",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.bpfLocations(new BpfLocationsRequest(" "))),
                Arguments.of(
                        "invalid locations collection",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.bpfLocations(new BpfLocationsRequest("invalid"))),
                Arguments.of(
                        "blank forecast location",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.bpfForecast(
                                        new BpfForecastRequest("improver-percentiles-spot-global", " "))),
                Arguments.of(
                        "invalid forecast collection",
                        (Function<SiteSpecificDataHubClient, MetOfficeToolResponse>)
                                client -> client.bpfForecast(new BpfForecastRequest("invalid", "loc-1"))));
    }

    private record Expected(String operation, Function<SiteSpecificDataHubClient, MetOfficeToolResponse> call) {
    }
}
