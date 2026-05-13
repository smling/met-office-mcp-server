package io.github.smling.met_office_mcp_server.model;

public enum MetOfficeProduct {
    ATMOSPHERIC("atmospheric"),
    SITE_SPECIFIC("site-specific"),
    OBSERVATIONS("observations"),
    MAP_IMAGES("map-images");

    private final String id;

    MetOfficeProduct(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
