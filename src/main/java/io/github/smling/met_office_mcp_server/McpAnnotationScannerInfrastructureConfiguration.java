package io.github.smling.met_office_mcp_server;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Marks Spring AI MCP annotation scanner beans as infrastructure so Spring does not warn when the scanner post-processor
 * needs them during BeanPostProcessor registration.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(McpTool.class)
@ConditionalOnProperty(prefix = "spring.ai.mcp.server.annotation-scanner", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@ImportRuntimeHints(McpNativeRuntimeHints.class)
class McpAnnotationScannerInfrastructureConfiguration {

    static final String[] MCP_ANNOTATION_SCANNER_INFRASTRUCTURE_BEANS = {
            "org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration",
            "serverAnnotatedBeanRegistry",
            "serverAnnotatedMethodBeanPostProcessor",
            "serverAnnotatedBeanFactoryInitializationAotProcessor",
            "spring.ai.mcp.server.annotation-scanner-org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerProperties"
    };

    @Bean
    static BeanDefinitionRegistryPostProcessor mcpAnnotationScannerInfrastructureRolePostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                for (String beanName : MCP_ANNOTATION_SCANNER_INFRASTRUCTURE_BEANS) {
                    if (registry.containsBeanDefinition(beanName)) {
                        registry.getBeanDefinition(beanName).setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // No bean instances are needed; only the definition roles are adjusted.
            }
        };
    }

}
