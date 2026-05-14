package io.github.smling.met_office_mcp_server.client;

import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.net.URI;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Centralizes distributed tracing for outbound Met Office DataHub client calls.
 */
@Aspect
@Component
public class MetOfficeDataHubTracingAspect {

    private final Tracer tracer;

    public MetOfficeDataHubTracingAspect(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(MetOfficeDataHubTracingAspect.class.getName());
    }

    @Around("""
            execution(public io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse
                io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient.getJson(..))
            || execution(public io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse
                io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient.getBinary(..))
            """)
    public Object traceDataHubCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MetOfficeProduct product = (MetOfficeProduct) args[0];
        String operation = (String) args[1];
        URI uri = (URI) args[2];
        String responseType = responseType(joinPoint.getSignature().getName(), args);

        Span span = tracer.spanBuilder("metoffice.datahub " + operation)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("metoffice.product", product.id())
                .setAttribute("metoffice.operation", operation)
                .setAttribute("metoffice.response.type", responseType)
                .setAttribute("http.request.method", "GET")
                .setAttribute("server.address", uri.getHost() == null ? "" : uri.getHost())
                .setAttribute("url.path", uri.getPath())
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            if (result instanceof MetOfficeToolResponse response) {
                recordResponse(span, response);
            }
            return result;
        } catch (Throwable ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, "exception");
            span.setAttribute("error.type", ex.getClass().getName());
            throw ex;
        } finally {
            span.end();
        }
    }

    private void recordResponse(Span span, MetOfficeToolResponse response) {
        span.setAttribute("http.response.status_code", response.status());
        span.setAttribute("metoffice.outcome", response.error() == null ? "success" : "error");
        if (response.binaryBase64() != null) {
            span.setAttribute("metoffice.binary_base64.length", response.binaryBase64().length());
        }
        if (response.error() != null) {
            span.setStatus(StatusCode.ERROR, response.error().message());
            span.setAttribute("error.type", errorType(response));
        }
    }

    private String errorType(MetOfficeToolResponse response) {
        if (response.status() == 400 && response.error().endpoint() == null) {
            return "local_validation";
        }
        if (response.status() == 502) {
            return "runtime_exception";
        }
        return "non_success_status";
    }

    private String responseType(String methodName, Object[] args) {
        if ("getJson".equals(methodName)) {
            return "json";
        }
        boolean binaryDebug = args.length > 5 && Boolean.TRUE.equals(args[5]);
        return binaryDebug ? "binary_debug" : "binary";
    }
}
