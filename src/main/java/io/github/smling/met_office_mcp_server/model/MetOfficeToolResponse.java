package io.github.smling.met_office_mcp_server.model;

import tools.jackson.databind.JsonNode;

public record MetOfficeToolResponse(
        String product,
        String operation,
        int status,
        String contentType,
        JsonNode data,
        String binaryBase64,
        MetOfficeError error) {

    public static MetOfficeToolResponse json(
            MetOfficeProduct product, String operation, int status, String contentType, JsonNode data) {
        return new MetOfficeToolResponse(product.id(), operation, status, contentType, data, null, null);
    }

    public static MetOfficeToolResponse binary(
            MetOfficeProduct product, String operation, int status, String contentType, String binaryBase64) {
        return new MetOfficeToolResponse(product.id(), operation, status, contentType, null, binaryBase64, null);
    }

    public static MetOfficeToolResponse error(MetOfficeProduct product, String operation, MetOfficeError error) {
        return new MetOfficeToolResponse(product.id(), operation, error.status(), null, null, null, error);
    }

    public record MetOfficeError(
            String message,
            int status,
            String product,
            String endpoint,
            String responseBodyPreview) {
    }
}
