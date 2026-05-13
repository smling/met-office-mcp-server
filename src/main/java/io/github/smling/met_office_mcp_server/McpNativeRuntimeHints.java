package io.github.smling.met_office_mcp_server;

import org.springframework.ai.mcp.annotation.context.DefaultMetaProvider;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

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
    }

}
