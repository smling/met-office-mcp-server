package io.github.smling.met_office_mcp_server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ai.mcp.annotation.context.DefaultMetaProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubClient;
import io.github.smling.met_office_mcp_server.client.MetOfficeDataHubTracingAspect;
import io.github.smling.met_office_mcp_server.model.MetOfficeToolResponse;

@SpringBootTest
class MetOfficeMcpServerApplicationTests {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void otlpMetricsExportIsDisabledByDefault() {
		assertEquals("false", applicationContext.getEnvironment()
				.getProperty("management.otlp.metrics.export.enabled"));
		assertFalse(Arrays.stream(applicationContext.getBeanDefinitionNames())
				.anyMatch(beanName -> beanName.toLowerCase().contains("otlp")
						&& beanName.toLowerCase().contains("meterregistry")));
	}

	@Test
	void tracingExportIsDisabledByDefault() {
		assertEquals("false", applicationContext.getEnvironment()
				.getProperty("management.tracing.export.otlp.enabled"));
		assertEquals("1.0", applicationContext.getEnvironment()
				.getProperty("management.tracing.sampling.probability"));
	}

	@Test
	void prometheusEndpointIsExposedByDefault() {
		assertEquals("health,info,metrics,prometheus", applicationContext.getEnvironment()
				.getProperty("management.endpoints.web.exposure.include"));
	}

	@Test
	void dataHubTracingAspectIsRegistered() {
		assertNotNull(applicationContext.getBean(MetOfficeDataHubTracingAspect.class));
	}

	@Test
	void dataHubClientIsProxiedForTracing() {
		assertTrue(AopUtils.isAopProxy(applicationContext.getBean(MetOfficeDataHubClient.class)));
	}

	@Test
	void mcpAnnotationScannerBeansAreInfrastructure() {
		for (String beanName : McpAnnotationScannerInfrastructureConfiguration.MCP_ANNOTATION_SCANNER_INFRASTRUCTURE_BEANS) {
			assertEquals(BeanDefinition.ROLE_INFRASTRUCTURE,
					applicationContext.getBeanFactory().getBeanDefinition(beanName).getRole());
		}
	}

	@Test
	void mcpDefaultMetaProviderConstructorIsRegisteredForNativeReflection() throws NoSuchMethodException {
		RuntimeHints hints = new RuntimeHints();

		new McpNativeRuntimeHints().registerHints(hints, getClass().getClassLoader());

		assertTrue(RuntimeHintsPredicates.reflection()
				.onConstructorInvocation(DefaultMetaProvider.class.getConstructor())
				.test(hints));
	}

	@Test
	void mcpToolResponseRecordsAreRegisteredForNativeReflection() throws NoSuchMethodException {
		RuntimeHints hints = new RuntimeHints();

		new McpNativeRuntimeHints().registerHints(hints, getClass().getClassLoader());

		for (String accessor : new String[] {"product", "operation", "status", "contentType", "data",
				"binaryBase64", "error"}) {
			assertTrue(RuntimeHintsPredicates.reflection()
					.onMethodInvocation(MetOfficeToolResponse.class.getMethod(accessor))
					.test(hints));
		}
		for (String accessor : new String[] {"message", "status", "product", "endpoint", "responseBodyPreview"}) {
			assertTrue(RuntimeHintsPredicates.reflection()
					.onMethodInvocation(MetOfficeToolResponse.MetOfficeError.class.getMethod(accessor))
					.test(hints));
		}
	}

	@Test
	void canBeConstructed() {
		assertNotNull(new MetOfficeMcpServerApplication());
	}

	@Test
	void mainDelegatesToSpringApplication() {
		String[] args = {"--spring.main.web-application-type=none"};
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			springApplication.when(() -> SpringApplication.run(MetOfficeMcpServerApplication.class, args))
					.thenReturn(context);

			MetOfficeMcpServerApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(MetOfficeMcpServerApplication.class, args));
		}
	}

}
