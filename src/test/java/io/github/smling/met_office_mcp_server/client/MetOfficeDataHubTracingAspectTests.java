package io.github.smling.met_office_mcp_server.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.met_office_mcp_server.model.MetOfficeProduct;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse.MetOfficeError;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.net.URI;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetOfficeDataHubTracingAspectTests {

    private final OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
    private final Tracer tracer = mock(Tracer.class);
    private final SpanBuilder spanBuilder = mock(SpanBuilder.class);
    private final Span span = mock(Span.class);
    private final Scope scope = mock(Scope.class);
    private MetOfficeDataHubTracingAspect aspect;

    @BeforeEach
    void setUp() {
        when(openTelemetry.getTracer(MetOfficeDataHubTracingAspect.class.getName())).thenReturn(tracer);
        aspect = new MetOfficeDataHubTracingAspect(openTelemetry);
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        when(span.setAttribute(anyString(), anyLong())).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
    }

    @Test
    void tracesJsonSuccessResponse() throws Throwable {
        MetOfficeToolResponse response = MetOfficeToolResponse.json(
                MetOfficeProduct.MAP_IMAGES, "operation", 200, "application/json", null);
        ProceedingJoinPoint joinPoint = joinPoint("getJson", response);

        Object actual = aspect.traceDataHubCall(joinPoint);

        assertSame(response, actual);
        verify(tracer).spanBuilder("metoffice.datahub operation");
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute("metoffice.product", "map-images");
        verify(spanBuilder).setAttribute("metoffice.operation", "operation");
        verify(spanBuilder).setAttribute("metoffice.response.type", "json");
        verify(spanBuilder).setAttribute("server.address", "example.test");
        verify(spanBuilder).setAttribute("url.path", "/data");
        verify(span).setAttribute("http.response.status_code", 200L);
        verify(span).setAttribute("metoffice.outcome", "success");
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void tracesBinaryDebugSuccessResponse() throws Throwable {
        MetOfficeToolResponse response = MetOfficeToolResponse.binary(
                MetOfficeProduct.MAP_IMAGES, "operation", 200, "image/png", "cG5n");
        ProceedingJoinPoint joinPoint = joinPoint("getBinary", response, true);

        Object actual = aspect.traceDataHubCall(joinPoint);

        assertSame(response, actual);
        verify(spanBuilder).setAttribute("metoffice.response.type", "binary_debug");
        verify(span).setAttribute("metoffice.binary_base64.length", 4L);
        verify(span).setAttribute("metoffice.outcome", "success");
        verify(span).end();
    }

    @Test
    void marksLocalValidationResponseAsError() throws Throwable {
        MetOfficeToolResponse response = MetOfficeToolResponse.error(
                MetOfficeProduct.OBSERVATIONS,
                "operation",
                new MetOfficeError("bad request", 400, "observations", null, null));
        ProceedingJoinPoint joinPoint = joinPoint("getJson", response);

        Object actual = aspect.traceDataHubCall(joinPoint);

        assertSame(response, actual);
        verify(span).setAttribute("metoffice.outcome", "error");
        verify(span).setStatus(StatusCode.ERROR, "bad request");
        verify(span).setAttribute("error.type", "local_validation");
        verify(span).end();
    }

    @Test
    void marksBadGatewayResponseAsRuntimeExceptionError() throws Throwable {
        MetOfficeToolResponse response = MetOfficeToolResponse.error(
                MetOfficeProduct.OBSERVATIONS,
                "operation",
                new MetOfficeError("Unable to call Met Office DataHub", 502, "observations", "https://example.test", null));
        ProceedingJoinPoint joinPoint = joinPoint("getJson", response);

        Object actual = aspect.traceDataHubCall(joinPoint);

        assertSame(response, actual);
        verify(span).setAttribute("error.type", "runtime_exception");
        verify(span).end();
    }

    @Test
    void marksUpstreamErrorResponseAsNonSuccessStatus() throws Throwable {
        MetOfficeToolResponse response = MetOfficeToolResponse.error(
                MetOfficeProduct.OBSERVATIONS,
                "operation",
                new MetOfficeError("Met Office DataHub request failed", 404, "observations", "https://example.test", null));
        ProceedingJoinPoint joinPoint = joinPoint("getJson", response);

        Object actual = aspect.traceDataHubCall(joinPoint);

        assertSame(response, actual);
        verify(span).setAttribute("error.type", "non_success_status");
        verify(span).end();
    }

    @Test
    void recordsThrownExceptionOnSpanAndRethrows() throws Throwable {
        IllegalStateException failure = new IllegalStateException("boom");
        ProceedingJoinPoint joinPoint = joinPoint("getJson", failure);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class, () -> aspect.traceDataHubCall(joinPoint));

        assertSame(failure, actual);
        verify(span).recordException(failure);
        verify(span).setStatus(StatusCode.ERROR, "exception");
        verify(span).setAttribute("error.type", IllegalStateException.class.getName());
        verify(span).end();
        verify(scope).close();
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object resultOrFailure) throws Throwable {
        return joinPoint(methodName, resultOrFailure, false);
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object resultOrFailure, boolean binaryDebug) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {
                MetOfficeProduct.MAP_IMAGES,
                "operation",
                URI.create("https://example.test/data"),
                "api-key",
                null,
                binaryDebug
        });
        if (resultOrFailure instanceof Throwable failure) {
            when(joinPoint.proceed()).thenThrow(failure);
        } else {
            when(joinPoint.proceed()).thenReturn(resultOrFailure);
        }
        return joinPoint;
    }
}
