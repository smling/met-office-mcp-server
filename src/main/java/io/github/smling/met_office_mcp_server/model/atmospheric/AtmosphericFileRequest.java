package io.github.smling.met_office_mcp_server.model.atmospheric;

public record AtmosphericFileRequest(String orderId, String run, String dataSpec, String fileId, Boolean binaryDebug) {

    public AtmosphericFileRequest(String orderId, String run, String dataSpec, String fileId) {
        this(orderId, run, dataSpec, fileId, false);
    }
}
