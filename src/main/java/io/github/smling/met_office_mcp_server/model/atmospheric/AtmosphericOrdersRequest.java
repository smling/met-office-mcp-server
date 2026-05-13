package io.github.smling.met_office_mcp_server.model.atmospheric;

public record AtmosphericOrdersRequest(String orderId, String run, String dataSpec) {
}
