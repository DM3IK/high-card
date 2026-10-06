package it.sara.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Spring Boot application that exposes the user REST API.
 */
@SpringBootApplication
public class HighCardApplication {

    /**
     * Starts the application. The {@code JWT_SECRET} environment variable must be set, see {@code HOW_TO_RUN.md}.
     *
     * @param args command-line arguments, passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(HighCardApplication.class, args);
    }

}
