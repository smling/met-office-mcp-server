package io.github.smling.met_office_mcp_server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest
class MetOfficeMcpServerApplicationTests {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void mcpAnnotationScannerBeansAreInfrastructure() {
		for (String beanName : McpAnnotationScannerInfrastructureConfiguration.MCP_ANNOTATION_SCANNER_INFRASTRUCTURE_BEANS) {
			assertEquals(BeanDefinition.ROLE_INFRASTRUCTURE,
					applicationContext.getBeanFactory().getBeanDefinition(beanName).getRole());
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
