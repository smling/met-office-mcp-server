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
    private static final int BINARY_PREVIEW_LENGTH = 80;

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
        return getBinary(product, operation, uri, apiKey, accept, false);
    }

    public MetOfficeToolResponse getBinary(
            MetOfficeProduct product,
            String operation,
            URI uri,
            String apiKey,
            MediaType accept,
            boolean binaryDebug) {
        return get(product, operation, uri, apiKey, accept, false, binaryDebug);
    }

    public URI uri(String baseUrl, String path, QueryParameter... queryParameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path);
        for (QueryParameter queryParameter : queryParameters) {
            if (queryParameter.value() != null) {
                builder.queryParam(queryParameter.name(), queryParameter.value());
            }
        }
        return URI.create(builder.build().toUriString());
    }

    public String encodedPathSegment(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            int unsignedByte = valueByte & 0xff;
            if (isUnreserved(unsignedByte)) {
                encoded.append((char) unsignedByte);
            } else {
                encoded.append('%');
                encoded.append(Character.toUpperCase(Character.forDigit(unsignedByte >> 4, 16)));
                encoded.append(Character.toUpperCase(Character.forDigit(unsignedByte & 0xf, 16)));
            }
        }
        return encoded.toString();
    }

    public QueryParameter query(String name, Object value) {
        return new QueryParameter(name, value);
    }

    private MetOfficeToolResponse get(
            MetOfficeProduct product, String operation, URI uri, String apiKey, MediaType accept, boolean jsonResponse) {
        return get(product, operation, uri, apiKey, accept, jsonResponse, false);
    }

    private MetOfficeToolResponse get(
            MetOfficeProduct product,
            String operation,
            URI uri,
            String apiKey,
            MediaType accept,
            boolean jsonResponse,
            boolean binaryDebug) {
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
            if (binaryDebug) {
                return MetOfficeToolResponse.binaryDebug(
                        product, operation, status, contentType, binaryDebugData(body));
            }
            String binaryBase64 = Base64.getEncoder().encodeToString(body);
            return MetOfficeToolResponse.binary(product, operation, status, contentType, binaryBase64);
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

    private JsonNode binaryDebugData(byte[] body) {
        var data = objectMapper.createObjectNode();
        data.put("byteLength", body.length);
        data.put("base64Length", base64Length(body.length));
        data.put("base64Preview", base64Preview(body));
        data.putNull("binaryBase64");
        return data;
    }

    private long base64Length(int byteLength) {
        return ((byteLength + 2L) / 3) * 4;
    }

    private String base64Preview(byte[] body) {
        int previewLength = (int) Math.min(BINARY_PREVIEW_LENGTH, base64Length(body.length));
        if (previewLength == 0) {
            return "";
        }
        int previewBytes = Math.min(body.length, ((previewLength + 3) / 4) * 3);
        byte[] previewBody = new byte[previewBytes];
        System.arraycopy(body, 0, previewBody, 0, previewBytes);
        String encodedPreview = Base64.getEncoder().encodeToString(previewBody);
        return encodedPreview.substring(0, Math.min(previewLength, encodedPreview.length()));
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

    private boolean isUnreserved(int value) {
        return (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9')
                || value == '-'
                || value == '.'
                || value == '_'
                || value == '~';
    }

    public record QueryParameter(String name, Object value) {
    }
}
