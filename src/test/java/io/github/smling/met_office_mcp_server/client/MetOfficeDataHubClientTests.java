package io.github.smling.met_office_mcp_server.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.smling.met_office_mcp_server.MetOfficeTestSupport;
import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MetOfficeDataHubClientTests {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final MetOfficeDataHubClient client = MetOfficeTestSupport.client(restClientBuilder);

    @Test
    void getJsonAddsApiKeyHeaderAndParsesVendorPayload() {
        URI uri = URI.create("https://example.test/data?detail=MINIMAL");
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "key"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        MetOfficeToolResponse response =
                client.getJson(MetOfficeProduct.MAP_IMAGES, "operation", uri, "key");

        assertEquals(200, response.status());
        assertEquals("map-images", response.product());
        assertEquals("operation", response.operation());
        assertEquals(true, response.data().get("ok").asBoolean());
        assertNull(response.binaryBase64());
        assertNull(response.error());
        server.verify();
    }

    @Test
    void getBinaryAddsApiKeyHeaderAndEncodesPayload() {
        URI uri = URI.create("https://example.test/file");
        server.expect(once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "key"))
                .andExpect(header("Accept", "image/png"))
                .andRespond(withSuccess("png".getBytes(), MediaType.IMAGE_PNG));

        MetOfficeToolResponse response =
                client.getBinary(MetOfficeProduct.MAP_IMAGES, "operation", uri, "key", MediaType.IMAGE_PNG);

        assertEquals(200, response.status());
        assertEquals("cG5n", response.binaryBase64());
        assertNull(response.data());
        assertNull(response.error());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 400, 401, 403, 404, 409, 429, 500, 502, 503})
    void nonSuccessResponsesReturnTypedErrorEnvelope(int status) {
        URI uri = URI.create("https://example.test/data");
        server.expect(once(), requestTo(uri))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("error-" + status));

        MetOfficeToolResponse response =
                client.getJson(MetOfficeProduct.OBSERVATIONS, "operation", uri, "key");

        assertEquals(status, response.status());
        assertNotNull(response.error());
        assertEquals("observations", response.error().product());
        assertEquals("error-" + status, response.error().responseBodyPreview());
        assertNull(response.data());
        assertNull(response.binaryBase64());
        server.verify();
    }

    @Test
    void missingApiKeyReturnsValidationErrorWithoutHttpCall() {
        URI uri = URI.create("https://example.test/data");

        MetOfficeToolResponse response =
                client.getJson(MetOfficeProduct.OBSERVATIONS, "operation", uri, "");

        assertEquals(400, response.status());
        assertNotNull(response.error());
        assertEquals("Missing API key for observations", response.error().message());
        server.verify();
    }

    @Test
    void invalidJsonReturnsBadGatewayError() {
        URI uri = URI.create("https://example.test/data");
        server.expect(once(), requestTo(uri))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

        MetOfficeToolResponse response =
                client.getJson(MetOfficeProduct.MAP_IMAGES, "operation", uri, "key");

        assertEquals(502, response.status());
        assertNotNull(response.error());
        assertEquals("Unable to call Met Office DataHub", response.error().message());
        server.verify();
    }

    @Test
    void uriOmitsNullQueryParameters() {
        URI uri = client.uri(
                "https://example.test/base",
                "/path",
                client.query("present", "value"),
                client.query("missing", null));

        assertEquals("https://example.test/base/path?present=value", uri.toString());
    }
}
