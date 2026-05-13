package io.github.smling.met_office_mcp_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MetOfficeProperties.class)
public class MetOfficeMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetOfficeMcpServerApplication.class, args);
	}

}
