package io.github.smling.met_office_mcp_server.model.mapimages;

public record MapImageFileRequest(String orderId, String run, String fileId, Boolean includeLand, Boolean binaryDebug) {

    public MapImageFileRequest(String orderId, String run, String fileId, Boolean includeLand) {
        this(orderId, run, fileId, includeLand, false);
    }
}
