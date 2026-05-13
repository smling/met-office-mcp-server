package io.github.smling.met_office_mcp_server.client;

import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse.MetOfficeError;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MetOfficeDataHubClient {

    private static final int ERROR_PREVIEW_LENGTH = 2_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MetOfficeDataHubClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public MetOfficeToolResponse getJson(MetOfficeProduct product, String operation, URI uri, String apiKey) {
        return get(product, operation, uri, apiKey, MediaType.APPLICATION_JSON, true);
    }

    public MetOfficeToolResponse getBinary(
            MetOfficeProduct product, String operation, URI uri, String apiKey, MediaType accept) {
        return get(product, operation, uri, apiKey, accept, false);
    }

    public URI uri(String baseUrl, String path, QueryParameter... queryParameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path);
        for (QueryParameter queryParameter : queryParameters) {
            if (queryParameter.value() != null) {
                builder.queryParam(queryParameter.name(), queryParameter.value());
            }
        }
        return builder.build().toUri();
    }

    public QueryParameter query(String name, Object value) {
        return new QueryParameter(name, value);
    }

    private MetOfficeToolResponse get(
            MetOfficeProduct product, String operation, URI uri, String apiKey, MediaType accept, boolean jsonResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            return MetOfficeToolResponse.error(
                    product,
                    operation,
                    new MetOfficeError(
                            "Missing API key for " + product.id(),
                            HttpStatus.BAD_REQUEST.value(),
                            product.id(),
                            uri.toString(),
                            null));
        }

        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(uri)
                    .accept(accept)
                    .header("apikey", apiKey)
                    .exchange((request, clientResponse) -> {
                        byte[] body = StreamUtils.copyToByteArray(clientResponse.getBody());
                        return new ResponseEntity<>(body, clientResponse.getHeaders(), clientResponse.getStatusCode());
                    });

            int status = response.getStatusCode().value();
            String contentType = contentType(response.getHeaders());
            byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()) {
                return MetOfficeToolResponse.error(
                        product,
                        operation,
                        new MetOfficeError(
                                "Met Office DataHub request failed",
                                status,
                                product.id(),
                                uri.toString(),
                                preview(body)));
            }
            if (jsonResponse) {
                JsonNode data = body.length == 0 ? objectMapper.createObjectNode() : objectMapper.readTree(body);
                return MetOfficeToolResponse.json(product, operation, status, contentType, data);
            }
            return MetOfficeToolResponse.binary(
                    product, operation, status, contentType, Base64.getEncoder().encodeToString(body));
        } catch (RuntimeException ex) {
            return MetOfficeToolResponse.error(
                    product,
                    operation,
                    new MetOfficeError(
                            "Unable to call Met Office DataHub",
                            HttpStatus.BAD_GATEWAY.value(),
                            product.id(),
                            uri.toString(),
                            ex.getMessage()));
        }
    }

    private String contentType(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        return contentType == null ? null : contentType.toString();
    }

    private String preview(byte[] body) {
        String value = new String(body, StandardCharsets.UTF_8);
        if (value.length() <= ERROR_PREVIEW_LENGTH) {
            return value;
        }
        return value.substring(0, ERROR_PREVIEW_LENGTH);
    }

    public record QueryParameter(String name, Object value) {
    }
}
