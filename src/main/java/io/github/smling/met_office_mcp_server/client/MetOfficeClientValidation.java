package io.github.smling.met_office_mcp_server.client;

import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Shared local validation helpers for Met Office product clients.
 */
public final class MetOfficeClientValidation {

    private static final Pattern SIX_CHARACTER_GEOHASH = Pattern.compile("[0-9bcdefghjkmnpqrstuvwxyz]{6}");

    private MetOfficeClientValidation() {
    }

    /**
     * Returns a typed validation error when a required string is missing.
     *
     * @param product product that owns the tool operation
     * @param operation MCP operation name
     * @param name parameter name
     * @param value parameter value
     * @return validation error response, or {@code null} when the value is present
     */
    public static MetOfficeToolResponse requiredString(
            MetOfficeProduct product, String operation, String name, String value) {
        if (value == null || value.isBlank()) {
            return validationError(product, operation, name + " is required");
        }
        return null;
    }

    /**
     * Returns a typed validation error when coordinates are not finite or are outside WGS84 bounds.
     *
     * @param product product that owns the tool operation
     * @param operation MCP operation name
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @return validation error response, or {@code null} when coordinates are valid
     */
    public static MetOfficeToolResponse coordinates(
            MetOfficeProduct product, String operation, double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            return validationError(product, operation, "latitude must be a finite value between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            return validationError(product, operation, "longitude must be a finite value between -180 and 180");
        }
        return null;
    }

    /**
     * Returns a typed validation error when a station geohash does not match the documented six-character shape.
     *
     * @param product product that owns the tool operation
     * @param operation MCP operation name
     * @param geohash station geohash
     * @return validation error response, or {@code null} when the geohash is valid
     */
    public static MetOfficeToolResponse sixCharacterGeohash(
            MetOfficeProduct product, String operation, String geohash) {
        MetOfficeToolResponse required = requiredString(product, operation, "geohash", geohash);
        if (required != null) {
            return required;
        }
        if (!SIX_CHARACTER_GEOHASH.matcher(geohash.toLowerCase(Locale.ROOT)).matches()) {
            return validationError(product, operation, "geohash must be a six-character station geohash");
        }
        return null;
    }

    /**
     * Creates a typed local validation error response.
     *
     * @param product product that owns the tool operation
     * @param operation MCP operation name
     * @param message validation failure message
     * @return response envelope with status {@code 400}
     */
    public static MetOfficeToolResponse validationError(
            MetOfficeProduct product, String operation, String message) {
        return MetOfficeToolResponse.error(
                product,
                operation,
                new MetOfficeToolResponse.MetOfficeError(message, 400, product.id(), null, null));
    }
}
