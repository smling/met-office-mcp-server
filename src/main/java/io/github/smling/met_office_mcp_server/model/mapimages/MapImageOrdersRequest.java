package io.github.smling.met_office_mcp_server.model.mapimages;

public record MapImageOrdersRequest(String orderId, String run, Boolean includeLand) {
}
