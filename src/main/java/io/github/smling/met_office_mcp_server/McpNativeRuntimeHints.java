package io.github.smling.met_office_mcp_server;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.springframework.ai.mcp.annotation.context.DefaultMetaProvider;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;

/**
 * Runtime hints needed by Spring AI MCP annotation processing in GraalVM native images.
 */
class McpNativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        try {
            hints.reflection().registerConstructor(DefaultMetaProvider.class.getConstructor(), ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Spring AI DefaultMetaProvider no-arg constructor is missing", ex);
        }

        registerRecord(hints, MetOfficeToolResponse.class);
        registerRecord(hints, MetOfficeToolResponse.MetOfficeError.class);
    }

    private void registerRecord(RuntimeHints hints, Class<?> recordType) {
        hints.reflection().registerType(recordType, builder -> {
            List<TypeReference> constructorParameterTypes = Arrays.stream(recordType.getRecordComponents())
                    .map(RecordComponent::getType)
                    .map(TypeReference::of)
                    .toList();
            builder.withConstructor(constructorParameterTypes, ExecutableMode.INVOKE);
            for (RecordComponent component : recordType.getRecordComponents()) {
                builder.withMethod(component.getAccessor().getName(), List.of(), ExecutableMode.INVOKE);
            }
        });
    }

}
