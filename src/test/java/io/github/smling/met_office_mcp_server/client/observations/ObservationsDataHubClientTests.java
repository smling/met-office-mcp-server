package io.github.smling.met_office_mcp_server.client.observations;

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
import io.github.smling.met_office_mcp_server.model.observations.NearestStationRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByGeohashRequest;
import io.github.smling.met_office_mcp_server.model.observations.ObservationsByLocationRequest;
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

class ObservationsDataHubClientTests {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final MetOfficeDataHubClient dataHubClient = MetOfficeTestSupport.client(restClientBuilder);
    private final ObservationsDataHubClient client =
            new ObservationsDataHubClient(dataHubClient, MetOfficeTestSupport.properties("atm", "site", "obs-key", "map"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("happyCases")
    void callsObservationsEndpoint(String name, URI uri, Expected expected) {
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "obs-key"))
                .andExpect(request -> assertFalse(String.valueOf(request.getURI().getQuery()).contains("apikey")))
                .andRespond(withSuccess("{\"station\":\"ok\"}", MediaType.APPLICATION_JSON));

        MetOfficeToolResponse response = expected.call().apply(client);

        assertEquals(200, response.status());
        assertEquals("observations", response.product());
        assertEquals(expected.operation(), response.operation());
        assertNotNull(response.data().get("station"));
        assertNull(response.error());
        server.verify();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidRequestsReturnValidationErrorWithoutHttpCall(
            String name, Function<ObservationsDataHubClient, MetOfficeToolResponse> call) {
        MetOfficeToolResponse response = call.apply(client);

        assertEquals(400, response.status());
        assertEquals("observations", response.product());
        assertNotNull(response.error());
        assertNull(response.data());
        assertNull(response.binaryBase64());
        server.verify();
    }

    private static Stream<Arguments> happyCases() {
        return Stream.of(
                Arguments.of(
                        "nearest station",
                        URI.create("https://example.test/observations/nearest?lat=51.5&lon=-0.1"),
                        new Expected(
                                "metoffice_observations_nearest_station",
                                client -> client.nearestStation(new NearestStationRequest(51.5, -0.1)))),
                Arguments.of(
                        "by geohash",
                        URI.create("https://example.test/observations/gcj8ds"),
                        new Expected(
                                "metoffice_observations_by_geohash",
                                client -> client.byGeohash(new ObservationsByGeohashRequest("gcj8ds")))),
                Arguments.of(
                        "by uppercase geohash",
                        URI.create("https://example.test/observations/GCJ8DS"),
                        new Expected(
                                "metoffice_observations_by_geohash",
                                client -> client.byGeohash(new ObservationsByGeohashRequest("GCJ8DS")))),
                Arguments.of(
                        "by location",
                        URI.create("https://example.test/observations/nearest?lat=51.5&lon=-0.1"),
                        new Expected(
                                "metoffice_observations_by_location",
                                client -> client.byLocation(new ObservationsByLocationRequest(51.5, -0.1)))));
    }

    private static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(
                        "invalid nearest latitude",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.nearestStation(new NearestStationRequest(Double.NaN, -0.1))),
                Arguments.of(
                        "invalid nearest longitude",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.nearestStation(new NearestStationRequest(51.5, 181.0))),
                Arguments.of(
                        "blank geohash",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.byGeohash(new ObservationsByGeohashRequest(" "))),
                Arguments.of(
                        "short geohash",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.byGeohash(new ObservationsByGeohashRequest("gcj8d"))),
                Arguments.of(
                        "invalid geohash character",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.byGeohash(new ObservationsByGeohashRequest("gcj8io"))),
                Arguments.of(
                        "invalid by-location longitude",
                        (Function<ObservationsDataHubClient, MetOfficeToolResponse>)
                                client -> client.byLocation(new ObservationsByLocationRequest(51.5, Double.POSITIVE_INFINITY))));
    }

    private record Expected(String operation, Function<ObservationsDataHubClient, MetOfficeToolResponse> call) {
    }
}
