package com.czagrzebski.printhelm.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class Application {
	private static final Logger logger = LogManager.getLogger(Application.class);

	public static void main(String[] args) {
		logger.info("Starting PrintHelm Web Service");

		// set the profile based on environment variable
		String profile = System.getProperty("spring.profiles.active");
		if (profile != null && !profile.isEmpty()) {
			System.setProperty("spring.profiles.active", profile);
			logger.info("Active profile set to: " + profile);
		} else {
			logger.info("No active profile set, using default configuration");
		}

		SpringApplication.run(Application.class, args);
	}

}
