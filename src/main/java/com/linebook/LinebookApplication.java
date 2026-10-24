package com.linebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LinebookApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinebookApplication.class, args);
    }
}
